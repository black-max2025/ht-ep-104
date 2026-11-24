package app.ht104.control.protocol

enum class SupportLevel { SUPPORTED, UNSUPPORTED, UNVERIFIED }

enum class Feature {
    BATTERY_LEVEL,
    BATTERY_CHARGING,
    BATTERY_LEFT,
    BATTERY_RIGHT,
    BATTERY_CASE,
    FIND_ALERT,
    FIND_LEFT_RIGHT,
    LINK_LOSS,
    DISCONNECT_GATT,
    DISCONNECT_CLASSIC,
    REMOTE_POWER_OFF,
    REMOTE_POWER_ON,
    BUTTON_CONFIG,
    ANC,
    TRANSPARENCY,
    EQUALIZER,
    MIC_CONTROL,
    RENAME,
    FIRMWARE_INFO,
    FIRMWARE_UPDATE,
    SIGNAL_STRENGTH,
    MEDIA_CONTROL,
    CALL_CONTROL,
}

/** A feature's support level plus the evidence behind it. Evidence is technical English text. */
data class Capability(val feature: Feature, val level: SupportLevel, val evidence: String)
