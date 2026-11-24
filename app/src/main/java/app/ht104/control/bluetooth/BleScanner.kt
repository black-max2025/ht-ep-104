package app.ht104.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import app.ht104.control.core.TargetMatcher
import app.ht104.control.core.Uuids
import app.ht104.control.core.toHex
import app.ht104.control.diagnostics.BleAdvertisement
import app.ht104.control.diagnostics.UuidInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Passive, unfiltered BLE scan that records advertisements. It never connects or writes. */
@SuppressLint("MissingPermission")
class BleScanner(private val context: Context, private val selectedAddress: () -> String?) {
    private val main = Handler(Looper.getMainLooper())
    private val _results = MutableStateFlow<Map<String, BleAdvertisement>>(emptyMap())
    val results: StateFlow<Map<String, BleAdvertisement>> = _results.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val stopRunnable = Runnable { stop() }

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = record(result)

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { record(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            _error.value = "BLE scan failed, error code $errorCode"
            _scanning.value = false
        }
    }

    private fun record(r: ScanResult) {
        val rec = r.scanRecord
        val address = r.device.address
        val name = rec?.deviceName ?: runCatching { r.device.name }.getOrNull()
        val mfg = LinkedHashMap<Int, String>()
        rec?.manufacturerSpecificData?.let { sa ->
            for (i in 0 until sa.size()) mfg[sa.keyAt(i)] = sa.valueAt(i).toHex()
        }
        val svcData = LinkedHashMap<String, String>()
        rec?.serviceData?.forEach { (k, v) -> svcData[Uuids.display(k.uuid)] = v.toHex() }
        val uuids = rec?.serviceUuids?.map { UuidInfo(Uuids.display(it.uuid), Uuids.serviceName(it.uuid)) } ?: emptyList()
        val now = System.currentTimeMillis()
        _results.update { map ->
            val previous = map[address]
            map + (address to BleAdvertisement(
                address = address,
                name = name ?: previous?.name,
                rssi = r.rssi,
                connectable = r.isConnectable,
                serviceUuids = if (uuids.isNotEmpty()) uuids else previous?.serviceUuids ?: emptyList(),
                manufacturerData = if (mfg.isNotEmpty()) mfg else previous?.manufacturerData ?: emptyMap(),
                serviceData = if (svcData.isNotEmpty()) svcData else previous?.serviceData ?: emptyMap(),
                txPower = rec?.txPowerLevel?.takeIf { it != Int.MIN_VALUE },
                lastSeenMs = now,
                sightings = (previous?.sightings ?: 0) + 1,
                looksLikeTarget = TargetMatcher.matchesName(name ?: previous?.name) || address == selectedAddress(),
            ))
        }
    }

    fun start(durationMs: Long = 20_000) {
        if (_scanning.value) return
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            _error.value = "BLE scanner unavailable (Bluetooth off or LE unsupported)"
            return
        }
        _error.value = null
        _results.value = emptyMap()
        try {
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
            _scanning.value = true
            main.postDelayed(stopRunnable, durationMs)
        } catch (e: SecurityException) {
            _error.value = "Missing Bluetooth scan permission: ${e.message}"
        } catch (e: IllegalStateException) {
            _error.value = "Scan could not start: ${e.message}"
        }
    }

    fun stop() {
        main.removeCallbacks(stopRunnable)
        if (!_scanning.value) return
        runCatching {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner?.stopScan(callback)
        }
        _scanning.value = false
    }
}
