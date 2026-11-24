package app.ht104.control.protocol

import app.ht104.control.diagnostics.GattSnapshot

/**
 * Standard protocol first, then any verified vendor protocols. The vendor list is intentionally empty:
 * no HT-EP104 vendor protocol has been identified yet (see docs/PROTOCOL_REPORT.md).
 */
object ProtocolRegistry {
    val vendorProtocols: List<DeviceProtocol> = emptyList()

    private fun active(snapshot: GattSnapshot): List<DeviceProtocol> =
        listOf<DeviceProtocol>(StandardGattProtocol) + vendorProtocols.filter { it.matches(snapshot) }

    private fun rank(l: SupportLevel) = when (l) {
        SupportLevel.SUPPORTED -> 2
        SupportLevel.UNVERIFIED -> 1
        SupportLevel.UNSUPPORTED -> 0
    }

    fun capabilities(snapshot: GattSnapshot, evidence: PlatformEvidence): Map<Feature, Capability> {
        val merged = LinkedHashMap<Feature, Capability>()
        for (p in active(snapshot)) {
            for ((f, c) in p.capabilities(snapshot, evidence)) {
                val existing = merged[f]
                if (existing == null || rank(c.level) > rank(existing.level)) merged[f] = c
            }
        }
        return merged
    }

    fun batteryReadings(snapshot: GattSnapshot, evidence: PlatformEvidence): List<BatteryReading> =
        active(snapshot).flatMap { it.batteryReadings(snapshot, evidence) }
}
