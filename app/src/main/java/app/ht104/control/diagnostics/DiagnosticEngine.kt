package app.ht104.control.diagnostics

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.content.ContextCompat
import app.ht104.control.BuildConfig
import app.ht104.control.bluetooth.BleScanner
import app.ht104.control.bluetooth.ClassicInspector
import app.ht104.control.bluetooth.GattInspector
import app.ht104.control.data.BtEventHandler
import app.ht104.control.data.ConnectionEventLog
import app.ht104.control.data.PermissionHelper
import app.ht104.control.data.SettingsStore
import app.ht104.control.protocol.Capability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Orchestrates all read-only discovery: Classic, BLE advertisements and GATT enumeration. */
@SuppressLint("MissingPermission")
class DiagnosticEngine(private val context: Context, private val settings: SettingsStore) {
    val classic = ClassicInspector(context)
    val scanner = BleScanner(context) { settings.selectedAddress }
    val gatt = GattInspector(context)

    private val _bonded = MutableStateFlow<List<ClassicDeviceInfo>>(emptyList())
    val bonded: StateFlow<List<ClassicDeviceInfo>> = _bonded.asStateFlow()
    private val _profiles = MutableStateFlow<List<ProfileState>>(emptyList())
    val profiles: StateFlow<List<ProfileState>> = _profiles.asStateFlow()
    private val _adapter = MutableStateFlow<AdapterInfo?>(null)
    val adapter: StateFlow<AdapterInfo?> = _adapter.asStateFlow()
    private val _classicError = MutableStateFlow<String?>(null)
    val classicError: StateFlow<String?> = _classicError.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val action = intent.action ?: return
            BtEventHandler.handle(c, intent, action)
        }
    }
    private var registered = false

    fun registerReceivers() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        // HFP vendor-specific events (e.g. battery reports) need a company-id category; 76 (0x004C) is a common one.
        val vendor = IntentFilter(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).apply {
            addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + "." + 76)
        }
        ContextCompat.registerReceiver(context, receiver, vendor, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    fun unregisterReceivers() {
        if (!registered) return
        runCatching { context.unregisterReceiver(receiver) }
        registered = false
    }

    fun refreshAdapter() {
        _adapter.value = classic.adapterInfo()
    }

    fun refreshBonded() {
        if (!PermissionHelper.hasBluetoothConnect(context)) {
            _classicError.value = "BLUETOOTH_CONNECT permission not granted"
            return
        }
        try {
            _bonded.value = classic.bondedDevices()
            _classicError.value = null
        } catch (e: SecurityException) {
            _classicError.value = "SecurityException: ${e.message}"
        }
    }

    suspend fun refreshProfiles() {
        val address = settings.selectedAddress ?: return
        if (!PermissionHelper.hasBluetoothConnect(context)) return
        _profiles.value = runCatching { classic.profileStates(address) }.getOrDefault(emptyList())
    }

    fun clearProfiles() {
        _profiles.value = emptyList()
    }

    fun environment(): Map<String, String> = linkedMapOf(
        "manufacturer" to Build.MANUFACTURER,
        "model" to Build.MODEL,
        "android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "bluetoothLeFeature" to (adapter.value?.leFeature?.toString() ?: "unknown"),
    )

    fun buildReport(
        permissions: List<PermissionStatus>,
        capabilities: List<Capability>,
        systemBattery: Int?,
        buttonLog: List<String>,
    ): DiagnosticsReport = DiagnosticsReport(
        generatedAtMs = System.currentTimeMillis(),
        appVersion = BuildConfig.VERSION_NAME,
        environment = environment(),
        permissions = permissions,
        adapter = adapter.value,
        bondedDevices = bonded.value,
        selectedAddress = settings.selectedAddress,
        profileStates = profiles.value,
        systemBatteryPercent = systemBattery,
        bleAdvertisements = scanner.results.value.values.sortedByDescending { it.rssi },
        gatt = gatt.snapshot.value,
        capabilities = capabilities,
        events = ConnectionEventLog.entries.value,
        buttonProbeLog = buttonLog,
    )
}
