package app.ht104.control.protocol

enum class ChargeState { UNKNOWN, CHARGING, DISCHARGING_ACTIVE, DISCHARGING_INACTIVE }

enum class PowerSourceState { NO, YES, UNKNOWN }

data class BatteryLevelStatus(
    val batteryPresent: Boolean,
    val wiredPower: PowerSourceState,
    val wirelessPower: PowerSourceState,
    val chargeState: ChargeState,
    val percent: Int?,
)

/**
 * Parser for the Bluetooth SIG Battery Level Status characteristic (0x2BED), layout per BAS 1.1:
 * flags(1) | power state(2, little endian) | [identifier(2)] | [battery level(1)] | [additional status(1)].
 * The raw bytes are always exported in diagnostics so this interpretation can be audited.
 */
object BatteryLevelStatusParser {
    fun parse(bytes: ByteArray): BatteryLevelStatus? {
        if (bytes.size < 3) return null
        val flags = bytes[0].toInt() and 0xFF
        val power = (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        var index = 3
        if (flags and 0x01 != 0) index += 2 // identifier
        var percent: Int? = null
        if (flags and 0x02 != 0) {
            if (bytes.size <= index) return null
            val p = bytes[index].toInt() and 0xFF
            percent = if (p in 0..100) p else null
        }
        return BatteryLevelStatus(
            batteryPresent = power and 0x01 != 0,
            wiredPower = powerSource((power shr 1) and 0x03),
            wirelessPower = powerSource((power shr 3) and 0x03),
            chargeState = when ((power shr 5) and 0x03) {
                1 -> ChargeState.CHARGING
                2 -> ChargeState.DISCHARGING_ACTIVE
                3 -> ChargeState.DISCHARGING_INACTIVE
                else -> ChargeState.UNKNOWN
            },
            percent = percent,
        )
    }

    private fun powerSource(v: Int) = when (v) {
        0 -> PowerSourceState.NO
        1 -> PowerSourceState.YES
        else -> PowerSourceState.UNKNOWN
    }
}
