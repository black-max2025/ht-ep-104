package app.ht104.control

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ht104.control.control.AudioController
import app.ht104.control.control.ButtonProbe
import app.ht104.control.control.CallController
import app.ht104.control.data.ConnectionEventLog
import app.ht104.control.data.LastLocation
import app.ht104.control.data.Notifier
import app.ht104.control.data.PermissionHelper
import app.ht104.control.data.SettingsStore
import app.ht104.control.diagnostics.DiagnosticEngine
import app.ht104.control.diagnostics.GattState
import app.ht104.control.diagnostics.ReportBuilder
import app.ht104.control.protocol.BatteryReading
import app.ht104.control.protocol.Capability
import app.ht104.control.protocol.ChargeState
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.PlatformEvidence
import app.ht104.control.protocol.ProtocolRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class SelectedDevice(val address: String, val name: String?)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val context: Context = app
    val settings = SettingsStore(app)
    val engine = DiagnosticEngine(app, settings)
    val audio = AudioController(app)
    val calls = CallController(app)
    val buttonProbe = ButtonProbe(app)

    private val _selected = MutableStateFlow(
        settings.selectedAddress?.let { SelectedDevice(it, settings.selectedName) },
    )
    val selected: StateFlow<SelectedDevice?> = _selected.asStateFlow()

    private val _systemBattery = MutableStateFlow<Int?>(null)
    val systemBattery: StateFlow<Int?> = _systemBattery.asStateFlow()

    private val _permissionTick = MutableStateFlow(0)
    val permissionTick: StateFlow<Int> = _permissionTick.asStateFlow()

    private val _refreshSeconds = MutableStateFlow(settings.refreshSeconds)
    val refreshSeconds: StateFlow<Int> = _refreshSeconds.asStateFlow()
    private val _lowThreshold = MutableStateFlow(settings.lowBatteryThreshold)
    val lowThreshold: StateFlow<Int> = _lowThreshold.asStateFlow()
    private val _notifyDisconnect = MutableStateFlow(settings.notifyOnDisconnect)
    val notifyDisconnect: StateFlow<Boolean> = _notifyDisconnect.asStateFlow()
    private val _saveLocation = MutableStateFlow(settings.saveLocationOnDisconnect)
    val saveLocation: StateFlow<Boolean> = _saveLocation.asStateFlow()

    data class LastSeen(val connectedMs: Long, val disconnectedMs: Long, val location: LastLocation?)

    private val _lastSeen = MutableStateFlow(readLastSeen())
    val lastSeen: StateFlow<LastSeen> = _lastSeen.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()
    private val _alertActive = MutableStateFlow(false)
    val alertActive: StateFlow<Boolean> = _alertActive.asStateFlow()

    val capabilities: StateFlow<Map<Feature, Capability>> =
        combine(engine.gatt.snapshot, _systemBattery, engine.scanner.results) { g, sb, scan ->
            ProtocolRegistry.capabilities(g, PlatformEvidence(sb, scanRssi(scan.values.map { it.rssi }, g.address)))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val batteryReadings: StateFlow<List<BatteryReading>> =
        combine(engine.gatt.snapshot, _systemBattery) { g, sb ->
            ProtocolRegistry.batteryReadings(g, PlatformEvidence(sb))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var lowNotified = false

    init {
        engine.registerReceivers()
        engine.refreshAdapter()
        refreshAll()
        viewModelScope.launch {
            ConnectionEventLog.entries.collect {
                _lastSeen.value = readLastSeen()
                engine.refreshProfiles()
                refreshSystemBattery()
            }
        }
        viewModelScope.launch {
            batteryReadings.collect { checkLowBattery(it) }
        }
        viewModelScope.launch {
            while (true) {
                delay(settings.refreshSeconds * 1000L)
                refreshSystemBattery()
                if (engine.gatt.snapshot.value.state == GattState.READY) {
                    engine.gatt.refreshBattery()
                    engine.gatt.readRssi()
                }
            }
        }
    }

    private fun scanRssi(rssis: List<Int>, address: String?): Int? = if (address == null) null else rssis.maxOrNull()

    private fun readLastSeen() = LastSeen(settings.lastConnectedMs, settings.lastDisconnectedMs, settings.lastLocation)

    fun refreshAll() {
        engine.refreshAdapter()
        engine.refreshBonded()
        viewModelScope.launch {
            engine.refreshProfiles()
            refreshSystemBattery()
        }
        _lastSeen.value = readLastSeen()
    }

    fun onPermissionResult() {
        _permissionTick.value++
        refreshAll()
    }

    private fun refreshSystemBattery() {
        val address = settings.selectedAddress
        _systemBattery.value =
            if (address != null && PermissionHelper.hasBluetoothConnect(context)) engine.classic.systemReportedBattery(address) else null
    }

    fun select(address: String, name: String?) {
        settings.selectedAddress = address
        settings.selectedName = name
        _selected.value = SelectedDevice(address, name)
        engine.gatt.disconnect()
        engine.clearProfiles()
        _lastSeen.value = readLastSeen()
        refreshAll()
    }

    fun connectGatt(address: String? = settings.selectedAddress) {
        if (address != null) engine.gatt.connect(address)
    }

    fun disconnectGatt() = engine.gatt.disconnect()

    fun refreshBatteryNow() {
        refreshSystemBattery()
        engine.gatt.refreshBattery()
        engine.gatt.readRssi()
    }

    fun setLanguage(lang: String) {
        settings.language = lang
    }

    fun setRefreshSeconds(v: Int) {
        settings.refreshSeconds = v
        _refreshSeconds.value = v
    }

    fun setLowThreshold(v: Int) {
        settings.lowBatteryThreshold = v
        _lowThreshold.value = v
    }

    fun setNotifyDisconnect(v: Boolean) {
        settings.notifyOnDisconnect = v
        _notifyDisconnect.value = v
    }

    fun setSaveLocation(v: Boolean) {
        settings.saveLocationOnDisconnect = v
        _saveLocation.value = v
    }

    fun startAlert() = sendAlert(2)

    fun stopAlert() = sendAlert(0)

    private fun sendAlert(level: Int) {
        if (capabilities.value[Feature.FIND_ALERT]?.level != app.ht104.control.protocol.SupportLevel.SUPPORTED) {
            _alertMessage.value = "Immediate Alert is not supported by this headset (see Diagnostics)."
            return
        }
        val error = engine.gatt.writeImmediateAlert(level)
        if (error == null) {
            _alertActive.value = level != 0
            _alertMessage.value = if (level == 0) {
                "Stop (level 0) was sent. The characteristic is write-only, so the stop cannot be read back: confirm by ear."
            } else {
                "High alert (level 2) was sent."
            }
        } else {
            _alertMessage.value = error
        }
    }

    private fun checkLowBattery(readings: List<BatteryReading>) {
        val threshold = settings.lowBatteryThreshold
        val low = readings.filter { it.percent != null && it.chargeState != ChargeState.CHARGING }
            .minOfOrNull { it.percent!! } ?: return
        if (low <= threshold && !lowNotified) {
            lowNotified = true
            Notifier.notify(
                context, 1002,
                context.getString(R.string.notif_low_battery_title),
                context.getString(R.string.notif_low_battery_text, low),
            )
        } else if (low > threshold + 5) {
            lowNotified = false
        }
    }

    /** Writes a report to the app cache and returns the file (shared by the UI through FileProvider). */
    fun export(json: Boolean): Pair<File, String> {
        val report = engine.buildReport(
            permissions = PermissionHelper.statuses(context),
            capabilities = capabilities.value.values.toList(),
            systemBattery = _systemBattery.value,
            buttonLog = buttonProbe.log.value,
        )
        val dir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val stamp = System.currentTimeMillis()
        val file = File(dir, "ht-ep104-diagnostics-$stamp.${if (json) "json" else "txt"}")
        file.writeText(if (json) ReportBuilder.toJson(report) else ReportBuilder.toText(report), Charsets.UTF_8)
        return file to (if (json) "application/json" else "text/plain")
    }

    fun shareIntent(file: File, mime: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCleared() {
        buttonProbe.stop()
        engine.scanner.stop()
        engine.gatt.disconnect()
        engine.unregisterReceivers()
    }
}
