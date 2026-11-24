package app.ht104.control.core

import java.util.UUID

/** Bluetooth SIG assigned-number helpers. Names are informational only; they never imply a capability. */
object Uuids {
    private const val BASE_SUFFIX = "-0000-1000-8000-00805f9b34fb"

    fun fromShort(id: Int): UUID = UUID.fromString(String.format("%08x%s", id, BASE_SUFFIX))

    val DEVICE_INFORMATION: UUID = fromShort(0x180A)
    val BATTERY_SERVICE: UUID = fromShort(0x180F)
    val IMMEDIATE_ALERT: UUID = fromShort(0x1802)
    val LINK_LOSS: UUID = fromShort(0x1803)

    val BATTERY_LEVEL: UUID = fromShort(0x2A19)
    val BATTERY_LEVEL_STATUS: UUID = fromShort(0x2BED)
    val ALERT_LEVEL: UUID = fromShort(0x2A06)
    val MODEL_NUMBER: UUID = fromShort(0x2A24)
    val HARDWARE_REVISION: UUID = fromShort(0x2A27)
    val FIRMWARE_REVISION: UUID = fromShort(0x2A26)
    val SOFTWARE_REVISION: UUID = fromShort(0x2A28)
    val MANUFACTURER_NAME: UUID = fromShort(0x2A29)

    val CHAR_USER_DESCRIPTION: UUID = fromShort(0x2901)
    val CHAR_PRESENTATION_FORMAT: UUID = fromShort(0x2904)

    /** 16-bit alias of a Bluetooth-base UUID, or null for vendor 128-bit UUIDs. */
    fun shortId(uuid: UUID): Int? {
        val s = uuid.toString().lowercase()
        return if (s.endsWith(BASE_SUFFIX) && s.startsWith("0000")) s.substring(4, 8).toInt(16) else null
    }

    fun isVendorSpecific(uuid: UUID): Boolean = shortId(uuid) == null

    fun display(uuid: UUID): String = shortId(uuid)?.let { "0x%04X".format(it) } ?: uuid.toString()

    private val services = mapOf(
        0x1800 to "Generic Access",
        0x1801 to "Generic Attribute",
        0x1802 to "Immediate Alert",
        0x1803 to "Link Loss",
        0x1804 to "Tx Power",
        0x180A to "Device Information",
        0x180F to "Battery",
        0x1812 to "Human Interface Device",
        0x1858 to "Gaming Audio",
        0x184E to "Audio Stream Control",
        0x1844 to "Volume Control",
    )

    private val characteristics = mapOf(
        0x2A00 to "Device Name",
        0x2A01 to "Appearance",
        0x2A04 to "Peripheral Preferred Connection Parameters",
        0x2A05 to "Service Changed",
        0x2A06 to "Alert Level",
        0x2A19 to "Battery Level",
        0x2A23 to "System ID",
        0x2A24 to "Model Number",
        0x2A25 to "Serial Number",
        0x2A26 to "Firmware Revision",
        0x2A27 to "Hardware Revision",
        0x2A28 to "Software Revision",
        0x2A29 to "Manufacturer Name",
        0x2A2A to "IEEE 11073-20601 Regulatory Certification",
        0x2A50 to "PnP ID",
        0x2BED to "Battery Level Status",
    )

    private val descriptors = mapOf(
        0x2900 to "Characteristic Extended Properties",
        0x2901 to "Characteristic User Description",
        0x2902 to "Client Characteristic Configuration",
        0x2904 to "Characteristic Presentation Format",
    )

    private val classicProfiles = mapOf(
        0x1101 to "Serial Port",
        0x1108 to "Headset (HSP)",
        0x1112 to "Headset Audio Gateway",
        0x110A to "A2DP Audio Source",
        0x110B to "A2DP Audio Sink",
        0x110C to "AVRCP Target",
        0x110D to "Advanced Audio Distribution",
        0x110E to "AVRCP (A/V Remote Control)",
        0x110F to "AVRCP Controller",
        0x111E to "Hands-Free (HFP)",
        0x111F to "Hands-Free Audio Gateway",
        0x1200 to "PnP Information",
    )

    fun serviceName(uuid: UUID): String? = shortId(uuid)?.let { services[it] }
    fun characteristicName(uuid: UUID): String? = shortId(uuid)?.let { characteristics[it] }
    fun descriptorName(uuid: UUID): String? = shortId(uuid)?.let { descriptors[it] }
    fun classicProfileName(uuid: UUID): String? = shortId(uuid)?.let { classicProfiles[it] }
}
