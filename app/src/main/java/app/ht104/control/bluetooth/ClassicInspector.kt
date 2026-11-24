package app.ht104.control.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import app.ht104.control.core.TargetMatcher
import app.ht104.control.core.Uuids
import app.ht104.control.diagnostics.AdapterInfo
import app.ht104.control.diagnostics.ClassicDeviceInfo
import app.ht104.control.diagnostics.ProfileState
import app.ht104.control.diagnostics.UuidInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Read-only Bluetooth Classic inspection: bonded devices, SDP UUIDs, A2DP/HFP connection state. */
@SuppressLint("MissingPermission")
class ClassicInspector(private val context: Context) {
    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun adapterInfo(): AdapterInfo = AdapterInfo(
        enabled = adapter?.isEnabled == true,
        adapterName = runCatching { adapter?.name }.getOrNull(),
        leFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
        leScannerAvailable = runCatching { adapter?.bluetoothLeScanner != null }.getOrDefault(false),
        le2mPhy = runCatching { adapter?.isLe2MPhySupported == true }.getOrDefault(false),
        leExtendedAdvertising = runCatching { adapter?.isLeExtendedAdvertisingSupported == true }.getOrDefault(false),
    )

    fun bondedDevices(): List<ClassicDeviceInfo> =
        (adapter?.bondedDevices ?: emptySet()).map { describe(it) }
            .sortedWith(compareByDescending<ClassicDeviceInfo> { it.looksLikeTarget }.thenBy { it.name ?: "" })

    private fun describe(d: BluetoothDevice): ClassicDeviceInfo {
        val name = runCatching { d.name }.getOrNull()
        val uuids = runCatching { d.uuids }.getOrNull()?.map {
            UuidInfo(it.uuid.toString(), Uuids.classicProfileName(it.uuid) ?: Uuids.serviceName(it.uuid))
        } ?: emptyList()
        return ClassicDeviceInfo(
            name = name,
            address = d.address,
            bondState = when (d.bondState) {
                BluetoothDevice.BOND_BONDED -> "BONDED"
                BluetoothDevice.BOND_BONDING -> "BONDING"
                else -> "NONE"
            },
            deviceType = when (d.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                BluetoothDevice.DEVICE_TYPE_LE -> "LE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual-mode"
                else -> "Unknown"
            },
            deviceClass = d.bluetoothClass?.let { "major=0x%04X device=0x%04X".format(it.majorDeviceClass, it.deviceClass) },
            uuids = uuids,
            looksLikeTarget = TargetMatcher.matchesName(name),
        )
    }

    suspend fun profileStates(address: String): List<ProfileState> {
        val ad = adapter ?: return emptyList()
        val device = runCatching { ad.getRemoteDevice(address) }.getOrNull() ?: return emptyList()
        return listOf(
            ProfileState("A2DP", queryProfile(ad, BluetoothProfile.A2DP, device)),
            ProfileState("HFP", queryProfile(ad, BluetoothProfile.HEADSET, device)),
            ProfileState("AVRCP", "no public Android API (inferred from SDP UUIDs only)"),
        )
    }

    private suspend fun queryProfile(ad: BluetoothAdapter, profile: Int, device: BluetoothDevice): String =
        withTimeoutOrNull(3000) {
            suspendCancellableCoroutine<String> { cont ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(p: Int, proxy: BluetoothProfile) {
                        val state = runCatching { stateName(proxy.getConnectionState(device)) }
                            .getOrElse { "error: ${it.message}" }
                        runCatching { ad.closeProfileProxy(p, proxy) }
                        if (cont.isActive) cont.resume(state)
                    }

                    override fun onServiceDisconnected(p: Int) {}
                }
                val ok = runCatching { ad.getProfileProxy(context, listener, profile) }.getOrDefault(false)
                if (!ok && cont.isActive) cont.resume("proxy unavailable")
            }
        } ?: "timeout"

    private fun stateName(s: Int) = when (s) {
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        else -> "UNKNOWN($s)"
    }

    /**
     * Battery level the Android stack learned over HFP/AVRCP. Uses a hidden API through reflection, so it may
     * be unavailable on some Android versions; null means "no value", never a guess.
     */
    @SuppressLint("DiscouragedPrivateApi", "PrivateApi")
    fun systemReportedBattery(address: String): Int? = runCatching {
        val device = adapter!!.getRemoteDevice(address)
        val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
        (method.invoke(device) as Int).takeIf { it in 0..100 }
    }.getOrNull()
}
