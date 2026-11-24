package app.ht104.control.protocol

import app.ht104.control.core.Uuids
import app.ht104.control.diagnostics.GattSnapshot
import app.ht104.control.diagnostics.GattState
import app.ht104.control.diagnostics.ServiceDump
import app.ht104.control.protocol.Feature.*
import app.ht104.control.protocol.SupportLevel.SUPPORTED
import app.ht104.control.protocol.SupportLevel.UNSUPPORTED
import app.ht104.control.protocol.SupportLevel.UNVERIFIED

/** Bluetooth SIG standard services only: Battery, Device Information, Immediate Alert, Link Loss. */
object StandardGattProtocol : DeviceProtocol {
    override val id = "standard-gatt"
    override val displayName = "Standard Bluetooth SIG GATT services"
    override fun matches(snapshot: GattSnapshot) = true

    private fun batteryServices(s: GattSnapshot): List<ServiceDump> =
        s.services.filter { it.uuid == Uuids.BATTERY_SERVICE }

    override fun batteryReadings(snapshot: GattSnapshot, evidence: PlatformEvidence): List<BatteryReading> {
        val out = ArrayList<BatteryReading>()
        for (svc in batteryServices(snapshot)) {
            val level = svc.characteristics.firstOrNull { it.uuid == Uuids.BATTERY_LEVEL }
                ?.value?.firstOrNull()?.toInt()?.and(0xFF)?.takeIf { it in 0..100 }
            val statusBytes = svc.characteristics.firstOrNull { it.uuid == Uuids.BATTERY_LEVEL_STATUS }?.value
            val status = statusBytes?.let { BatteryLevelStatusParser.parse(it) }
            val percent = level ?: status?.percent
            if (percent == null && status == null) continue
            out += BatteryReading(
                source = "GATT Battery Service 0x180F (instance ${svc.instanceId})",
                percent = percent,
                chargeState = status?.chargeState ?: ChargeState.UNKNOWN,
                evidence = buildString {
                    if (level != null) append("0x2A19 = $level%. ")
                    if (status != null) append("0x2BED parsed per BAS 1.1.")
                }.trim(),
            )
        }
        evidence.systemBatteryPercent?.let {
            out += BatteryReading(
                source = "Android system (HFP/AVRCP aggregate, not per-earbud)",
                percent = it,
                chargeState = ChargeState.UNKNOWN,
                evidence = "BluetoothDevice battery level reported by the Android Bluetooth stack.",
            )
        }
        return out
    }

    override fun capabilities(snapshot: GattSnapshot, evidence: PlatformEvidence): Map<Feature, Capability> {
        val out = LinkedHashMap<Feature, Capability>()
        fun put(f: Feature, level: SupportLevel, text: String) {
            out[f] = Capability(f, level, text)
        }

        val discovered = snapshot.services.isNotEmpty()
        val needDiscovery = if (snapshot.state == GattState.FAILED) {
            "GATT connection failed (${snapshot.errors.lastOrNull() ?: "unknown error"}); standard GATT data is unavailable."
        } else {
            "GATT discovery has not been run yet."
        }

        val battery = batteryServices(snapshot)
        val levelReadable = battery.any { s ->
            s.characteristics.any { it.uuid == Uuids.BATTERY_LEVEL && !(it.value?.isEmpty() ?: true) }
        }
        val readings = batteryReadings(snapshot, evidence)
        val chargeKnown = readings.any { it.chargeState != ChargeState.UNKNOWN }

        when {
            levelReadable -> put(BATTERY_LEVEL, SUPPORTED, "GATT Battery Service 0x180F / 0x2A19 returned a value.")
            evidence.systemBatteryPercent != null ->
                put(BATTERY_LEVEL, SUPPORTED, "Android reports a battery level for this device (aggregate).")
            discovered && battery.isEmpty() ->
                put(BATTERY_LEVEL, UNSUPPORTED, "No Battery Service (0x180F) in GATT and Android reports no battery level.")
            discovered -> put(BATTERY_LEVEL, UNVERIFIED, "Battery Service present but 0x2A19 returned no value.")
            else -> put(BATTERY_LEVEL, UNVERIFIED, needDiscovery)
        }

        when {
            chargeKnown -> put(BATTERY_CHARGING, SUPPORTED, "Battery Level Status 0x2BED reports a charge state.")
            discovered -> put(
                BATTERY_CHARGING, UNSUPPORTED,
                "Battery Level Status (0x2BED) is not exposed. Charging is never inferred from a percentage.",
            )
            else -> put(BATTERY_CHARGING, UNVERIFIED, needDiscovery)
        }

        for (f in listOf(BATTERY_LEFT, BATTERY_RIGHT, BATTERY_CASE)) {
            if (discovered) {
                put(f, UNSUPPORTED, "No data source labels this component (${battery.size} unlabeled GATT battery instance(s) found).")
            } else {
                put(f, UNVERIFIED, needDiscovery)
            }
        }

        val vendorCount = snapshot.services.count { Uuids.isVendorSpecific(it.uuid) }
        val alertWritable = snapshot.services
            .filter { it.uuid == Uuids.IMMEDIATE_ALERT }
            .any { s -> s.characteristics.any { it.uuid == Uuids.ALERT_LEVEL && "WRITE_NO_RESPONSE" in it.properties } }
        when {
            alertWritable -> put(FIND_ALERT, SUPPORTED, "Immediate Alert Service 0x1802 with writable Alert Level 0x2A06.")
            discovered -> put(
                FIND_ALERT, UNSUPPORTED,
                "No usable Immediate Alert Service. $vendorCount vendor-specific service(s) not yet investigated.",
            )
            else -> put(FIND_ALERT, UNVERIFIED, needDiscovery)
        }
        if (discovered) {
            put(FIND_LEFT_RIGHT, UNSUPPORTED, "No standard mechanism addresses an individual earbud.")
        } else {
            put(FIND_LEFT_RIGHT, UNVERIFIED, needDiscovery)
        }

        when {
            snapshot.services.any { it.uuid == Uuids.LINK_LOSS } ->
                put(LINK_LOSS, UNVERIFIED, "Link Loss Service 0x1803 is present; this app does not write to it.")
            discovered -> put(LINK_LOSS, UNSUPPORTED, "Link Loss Service 0x1803 not present.")
            else -> put(LINK_LOSS, UNVERIFIED, needDiscovery)
        }

        val gattLive = snapshot.state == GattState.CONNECTED || snapshot.state == GattState.DISCOVERING || snapshot.state == GattState.READY
        put(
            DISCONNECT_GATT,
            if (gattLive) SUPPORTED else UNVERIFIED,
            if (gattLive) "GATT link is open and can be closed by this app." else "GATT is not connected.",
        )
        put(
            DISCONNECT_CLASSIC, UNSUPPORTED,
            "Android has no public API to connect/disconnect Classic profiles. Use system Bluetooth settings.",
        )
        val noVendor = "No standard command exists; needs vendor-protocol evidence (not identified)."
        put(REMOTE_POWER_OFF, UNVERIFIED, noVendor)
        put(REMOTE_POWER_ON, UNVERIFIED, noVendor)
        put(BUTTON_CONFIG, UNVERIFIED, noVendor)
        put(ANC, UNVERIFIED, noVendor)
        put(TRANSPARENCY, UNVERIFIED, noVendor)
        put(EQUALIZER, UNVERIFIED, noVendor)
        put(MIC_CONTROL, UNVERIFIED, noVendor)
        put(RENAME, UNVERIFIED, noVendor)
        put(FIRMWARE_UPDATE, UNSUPPORTED, "No documented safe update mechanism; intentionally not implemented.")

        val disUuids = setOf(
            Uuids.FIRMWARE_REVISION, Uuids.SOFTWARE_REVISION, Uuids.HARDWARE_REVISION,
            Uuids.MODEL_NUMBER, Uuids.MANUFACTURER_NAME,
        )
        val disHasValue = snapshot.services.filter { it.uuid == Uuids.DEVICE_INFORMATION }
            .any { s -> s.characteristics.any { it.uuid in disUuids && it.value != null } }
        when {
            disHasValue -> put(FIRMWARE_INFO, SUPPORTED, "Device Information Service 0x180A returned revision data.")
            discovered -> put(FIRMWARE_INFO, UNSUPPORTED, "Device Information Service 0x180A exposes no readable revision strings.")
            else -> put(FIRMWARE_INFO, UNVERIFIED, needDiscovery)
        }

        if (snapshot.rssi != null || evidence.scanRssi != null) {
            put(SIGNAL_STRENGTH, SUPPORTED, "RSSI sampled from a GATT link or BLE advertisement.")
        } else {
            put(SIGNAL_STRENGTH, UNVERIFIED, "No RSSI sample yet; run a scan or connect GATT.")
        }

        put(MEDIA_CONTROL, SUPPORTED, "Android media-key dispatch; AVRCP is handled by the OS. Headset behavior to be validated.")
        put(CALL_CONTROL, SUPPORTED, "Android Telecom API; requires the ANSWER_PHONE_CALLS permission.")
        return out
    }
}
