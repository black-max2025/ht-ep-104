package app.ht104.control.protocol

import app.ht104.control.diagnostics.GattSnapshot

/** Data the protocol layer may use that does not come from GATT. */
data class PlatformEvidence(val systemBatteryPercent: Int? = null, val scanRssi: Int? = null)

data class BatteryReading(
    /** Where the number came from, e.g. "GATT Battery Service instance 12". Never a guessed component. */
    val source: String,
    val percent: Int?,
    /** UNKNOWN unless a real characteristic reported it. */
    val chargeState: ChargeState,
    val evidence: String,
)

/**
 * Extension point for device protocols. A vendor protocol must only be added to [ProtocolRegistry]
 * after its UUIDs, payloads and responses are documented in docs/PROTOCOL_REPORT.md.
 */
interface DeviceProtocol {
    val id: String
    val displayName: String
    fun matches(snapshot: GattSnapshot): Boolean
    fun capabilities(snapshot: GattSnapshot, evidence: PlatformEvidence): Map<Feature, Capability>
    fun batteryReadings(snapshot: GattSnapshot, evidence: PlatformEvidence): List<BatteryReading>
}
