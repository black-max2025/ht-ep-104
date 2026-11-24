package app.ht104.control.diagnostics

import app.ht104.control.core.Json
import app.ht104.control.core.Uuids
import app.ht104.control.core.formatTime
import app.ht104.control.core.toHex
import app.ht104.control.core.toPrintableText

object ReportBuilder {
    fun toJson(r: DiagnosticsReport): String = Json.encode(toMap(r))

    private fun uuidMap(u: UuidInfo) = mapOf("uuid" to u.uuid, "name" to u.name)

    fun toMap(r: DiagnosticsReport): Map<String, Any?> = linkedMapOf(
        "generatedAt" to formatTime(r.generatedAtMs),
        "appVersion" to r.appVersion,
        "environment" to r.environment,
        "permissions" to r.permissions.map { mapOf("permission" to it.permission, "granted" to it.granted, "purpose" to it.purpose) },
        "adapter" to r.adapter?.let {
            mapOf(
                "enabled" to it.enabled, "name" to it.adapterName, "leFeature" to it.leFeature,
                "leScannerAvailable" to it.leScannerAvailable, "le2mPhy" to it.le2mPhy,
                "leExtendedAdvertising" to it.leExtendedAdvertising,
            )
        },
        "selectedAddress" to r.selectedAddress,
        "bondedDevices" to r.bondedDevices.map {
            mapOf(
                "name" to it.name, "address" to it.address, "bondState" to it.bondState, "type" to it.deviceType,
                "class" to it.deviceClass, "looksLikeTarget" to it.looksLikeTarget, "serviceUuids" to it.uuids.map { u -> uuidMap(u) },
            )
        },
        "profileStates" to r.profileStates.map { mapOf("profile" to it.profile, "state" to it.state) },
        "systemReportedBatteryPercent" to r.systemBatteryPercent,
        "bleAdvertisements" to r.bleAdvertisements.map {
            mapOf(
                "address" to it.address, "name" to it.name, "rssi" to it.rssi, "connectable" to it.connectable,
                "txPower" to it.txPower, "sightings" to it.sightings, "looksLikeTarget" to it.looksLikeTarget,
                "serviceUuids" to it.serviceUuids.map { u -> uuidMap(u) },
                "manufacturerData" to it.manufacturerData.mapKeys { e -> "0x%04X".format(e.key) },
                "serviceData" to it.serviceData,
            )
        },
        "gatt" to mapOf(
            "state" to r.gatt.state.name, "address" to r.gatt.address, "rssi" to r.gatt.rssi, "mtu" to r.gatt.mtu,
            "errors" to r.gatt.errors,
            "services" to r.gatt.services.map { s ->
                mapOf(
                    "uuid" to s.uuid.toString(), "short" to Uuids.display(s.uuid), "name" to s.name,
                    "instanceId" to s.instanceId, "primary" to s.primary,
                    "characteristics" to s.characteristics.map { c ->
                        mapOf(
                            "uuid" to c.uuid.toString(), "short" to Uuids.display(c.uuid), "name" to c.name,
                            "instanceId" to c.instanceId, "properties" to c.properties,
                            "valueHex" to c.valueHex, "valueText" to c.value?.toPrintableText(),
                            "readStatus" to c.readStatus?.let { GattStatus.describe(it) },
                            "descriptors" to c.descriptors.map { d ->
                                mapOf("uuid" to d.uuid.toString(), "name" to d.name, "valueHex" to d.value?.toHex())
                            },
                        )
                    },
                )
            },
        ),
        "capabilities" to r.capabilities.map {
            mapOf("feature" to it.feature.name, "level" to it.level.name, "evidence" to it.evidence)
        },
        "events" to r.events.map { mapOf("time" to formatTime(it.timeMs), "source" to it.source, "message" to it.message) },
        "buttonProbeLog" to r.buttonProbeLog,
    )

    fun toText(r: DiagnosticsReport): String = buildString {
        fun h(t: String) = append("\n==== ").append(t).append(" ====\n")
        append("HT-EP104 Control - Diagnostics Report\n")
        append("Generated: ").append(formatTime(r.generatedAtMs)).append("\nApp version: ").append(r.appVersion).append('\n')

        h("Environment")
        r.environment.forEach { (k, v) -> append("$k: $v\n") }
        h("Permissions")
        r.permissions.forEach { append("${if (it.granted) "[granted]" else "[DENIED] "} ${it.permission} - ${it.purpose}\n") }
        h("Bluetooth adapter")
        append(r.adapter?.toString() ?: "unavailable").append('\n')

        h("Bonded devices (selected: ${r.selectedAddress ?: "none"})")
        r.bondedDevices.forEach { d ->
            append("- ${d.name ?: "?"} [${d.address}] bond=${d.bondState} type=${d.deviceType} class=${d.deviceClass ?: "-"}")
            append(if (d.looksLikeTarget) "  <-- looks like HT-EP104\n" else "\n")
            d.uuids.forEach { append("    SDP ${it.uuid}${it.name?.let { n -> "  ($n)" } ?: ""}\n") }
        }
        h("Classic profile states")
        r.profileStates.forEach { append("${it.profile}: ${it.state}\n") }
        append("Android-reported battery (aggregate): ${r.systemBatteryPercent?.let { "$it%" } ?: "not available"}\n")

        h("BLE advertisements")
        if (r.bleAdvertisements.isEmpty()) append("(none recorded)\n")
        r.bleAdvertisements.forEach { a ->
            append("- ${a.name ?: "(no name)"} [${a.address}] rssi=${a.rssi} connectable=${a.connectable} tx=${a.txPower ?: "-"} seen=${a.sightings}")
            append(if (a.looksLikeTarget) "  <-- candidate\n" else "\n")
            a.serviceUuids.forEach { append("    service ${it.uuid}${it.name?.let { n -> " ($n)" } ?: ""}\n") }
            a.manufacturerData.forEach { (id, hex) -> append("    manufacturer 0x%04X: %s\n".format(id, hex)) }
            a.serviceData.forEach { (u, hex) -> append("    service-data $u: $hex\n") }
        }

        h("GATT")
        append("State: ${r.gatt.state}  Address: ${r.gatt.address ?: "-"}  RSSI: ${r.gatt.rssi ?: "-"}  MTU: ${r.gatt.mtu ?: "-"}\n")
        r.gatt.errors.forEach { append("ERROR: $it\n") }
        r.gatt.services.forEach { s ->
            append("\nService ${Uuids.display(s.uuid)} ${s.name ?: ""} (instance ${s.instanceId}${if (s.primary) ", primary" else ""})\n")
            if (!s.uuid.toString().startsWith("0000")) append("  full UUID: ${s.uuid}\n")
            s.characteristics.forEach { c ->
                append("  Char ${Uuids.display(c.uuid)} ${c.name ?: ""} [${c.properties.joinToString("|")}]\n")
                if (c.value != null) {
                    append("    value: ${c.valueHex}")
                    c.value.toPrintableText()?.let { append("  \"$it\"") }
                    append('\n')
                } else if (c.readStatus != null) {
                    append("    read failed: ${GattStatus.describe(c.readStatus)}\n")
                }
                c.descriptors.forEach { d ->
                    append("    Desc ${Uuids.display(d.uuid)} ${d.name ?: ""}${d.value?.let { v -> " = ${v.toHex()}" } ?: ""}\n")
                }
            }
        }

        h("Capability assessment (evidence based)")
        r.capabilities.forEach { append("${it.level.name.padEnd(11)} ${it.feature.name}: ${it.evidence}\n") }
        h("Event log")
        r.events.forEach { append("${formatTime(it.timeMs)} [${it.source}] ${it.message}\n") }
        h("Physical button probe")
        if (r.buttonProbeLog.isEmpty()) append("(not run)\n")
        r.buttonProbeLog.forEach { append("$it\n") }
    }
}
