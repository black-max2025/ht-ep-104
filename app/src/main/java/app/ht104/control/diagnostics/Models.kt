package app.ht104.control.diagnostics

import app.ht104.control.core.toHex
import java.util.UUID

data class UuidInfo(val uuid: String, val name: String?)

data class AdapterInfo(
    val enabled: Boolean,
    val adapterName: String?,
    val leFeature: Boolean,
    val leScannerAvailable: Boolean,
    val le2mPhy: Boolean,
    val leExtendedAdvertising: Boolean,
)

data class ClassicDeviceInfo(
    val name: String?,
    val address: String,
    val bondState: String,
    val deviceType: String,
    val deviceClass: String?,
    val uuids: List<UuidInfo>,
    val looksLikeTarget: Boolean,
)

data class ProfileState(val profile: String, val state: String)

data class BleAdvertisement(
    val address: String,
    val name: String?,
    val rssi: Int,
    val connectable: Boolean,
    val serviceUuids: List<UuidInfo>,
    val manufacturerData: Map<Int, String>,
    val serviceData: Map<String, String>,
    val txPower: Int?,
    val lastSeenMs: Long,
    val sightings: Int,
    val looksLikeTarget: Boolean,
)

enum class GattState { IDLE, CONNECTING, CONNECTED, DISCOVERING, READY, DISCONNECTED, FAILED }

class DescriptorDump(val uuid: UUID, val name: String?, val value: ByteArray?)

class CharacteristicDump(
    val uuid: UUID,
    val name: String?,
    val instanceId: Int,
    val properties: List<String>,
    val value: ByteArray?,
    val readStatus: Int?,
    val descriptors: List<DescriptorDump>,
) {
    val valueHex: String? get() = value?.toHex()
}

class ServiceDump(
    val uuid: UUID,
    val name: String?,
    val instanceId: Int,
    val primary: Boolean,
    val characteristics: List<CharacteristicDump>,
)

class GattSnapshot(
    val state: GattState,
    val address: String?,
    val services: List<ServiceDump>,
    val rssi: Int?,
    val mtu: Int?,
    val errors: List<String>,
) {
    companion object {
        val IDLE = GattSnapshot(GattState.IDLE, null, emptyList(), null, null, emptyList())
    }
}

data class EventLogEntry(val timeMs: Long, val source: String, val message: String)

data class PermissionStatus(val permission: String, val granted: Boolean, val purpose: String)

data class DiagnosticsReport(
    val generatedAtMs: Long,
    val appVersion: String,
    val environment: Map<String, String>,
    val permissions: List<PermissionStatus>,
    val adapter: AdapterInfo?,
    val bondedDevices: List<ClassicDeviceInfo>,
    val selectedAddress: String?,
    val profileStates: List<ProfileState>,
    val systemBatteryPercent: Int?,
    val bleAdvertisements: List<BleAdvertisement>,
    val gatt: GattSnapshot,
    val capabilities: List<app.ht104.control.protocol.Capability>,
    val events: List<EventLogEntry>,
    val buttonProbeLog: List<String>,
)
