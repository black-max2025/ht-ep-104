package app.ht104.control

import app.ht104.control.core.Uuids
import app.ht104.control.diagnostics.CharacteristicDump
import app.ht104.control.diagnostics.GattSnapshot
import app.ht104.control.diagnostics.GattState
import app.ht104.control.diagnostics.ServiceDump
import app.ht104.control.protocol.BatteryLevelStatusParser
import app.ht104.control.protocol.ChargeState
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.PlatformEvidence
import app.ht104.control.protocol.ProtocolRegistry
import app.ht104.control.protocol.SupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class ProtocolTest {
    private fun char(uuid: UUID, props: List<String>, value: ByteArray?) =
        CharacteristicDump(uuid, null, 1, props, value, if (value != null) 0 else null, emptyList())

    private fun snapshot(vararg services: ServiceDump) =
        GattSnapshot(GattState.READY, "AA:BB", services.toList(), null, null, emptyList())

    private val batteryOnly = snapshot(
        ServiceDump(Uuids.BATTERY_SERVICE, "Battery", 1, true, listOf(char(Uuids.BATTERY_LEVEL, listOf("READ"), byteArrayOf(87)))),
    )

    @Test fun parsesChargingFromBatteryLevelStatus() {
        // flags=level present, power state: battery present + charging
        val s = BatteryLevelStatusParser.parse(byteArrayOf(0x02, 0x21, 0x00, 80))!!
        assertEquals(ChargeState.CHARGING, s.chargeState)
        assertEquals(80, s.percent)
        val d = BatteryLevelStatusParser.parse(byteArrayOf(0x02, 0x41, 0x00, 55))!!
        assertEquals(ChargeState.DISCHARGING_ACTIVE, d.chargeState)
    }

    @Test fun rejectsTruncatedStatus() {
        assertNull(BatteryLevelStatusParser.parse(byteArrayOf(0x02, 0x21)))
        assertNull(BatteryLevelStatusParser.parse(byteArrayOf(0x02, 0x21, 0x00)))
    }

    @Test fun batteryPercentDoesNotImplyCharging() {
        val caps = ProtocolRegistry.capabilities(batteryOnly, PlatformEvidence())
        assertEquals(SupportLevel.SUPPORTED, caps[Feature.BATTERY_LEVEL]!!.level)
        assertEquals(SupportLevel.UNSUPPORTED, caps[Feature.BATTERY_CHARGING]!!.level)
        val reading = ProtocolRegistry.batteryReadings(batteryOnly, PlatformEvidence()).single()
        assertEquals(87, reading.percent)
        assertEquals(ChargeState.UNKNOWN, reading.chargeState)
    }

    @Test fun componentsStayUnsupportedAfterDiscovery() {
        val caps = ProtocolRegistry.capabilities(batteryOnly, PlatformEvidence())
        listOf(Feature.BATTERY_LEFT, Feature.BATTERY_RIGHT, Feature.BATTERY_CASE).forEach {
            assertEquals(SupportLevel.UNSUPPORTED, caps[it]!!.level)
        }
    }

    @Test fun beforeDiscoveryNothingIsClaimed() {
        val caps = ProtocolRegistry.capabilities(GattSnapshot.IDLE, PlatformEvidence())
        assertEquals(SupportLevel.UNVERIFIED, caps[Feature.BATTERY_LEVEL]!!.level)
        assertEquals(SupportLevel.UNVERIFIED, caps[Feature.FIND_ALERT]!!.level)
        assertEquals(SupportLevel.UNVERIFIED, caps[Feature.BATTERY_LEFT]!!.level)
    }

    @Test fun systemBatteryAloneGivesLevelButNoCharging() {
        val caps = ProtocolRegistry.capabilities(GattSnapshot.IDLE, PlatformEvidence(systemBatteryPercent = 64))
        assertEquals(SupportLevel.SUPPORTED, caps[Feature.BATTERY_LEVEL]!!.level)
        assertNotNull(caps[Feature.BATTERY_CHARGING])
        assertEquals(SupportLevel.UNVERIFIED, caps[Feature.BATTERY_CHARGING]!!.level)
    }

    @Test fun immediateAlertRequiresWritableAlertLevel() {
        val withIas = snapshot(
            ServiceDump(Uuids.IMMEDIATE_ALERT, "Immediate Alert", 1, true, listOf(char(Uuids.ALERT_LEVEL, listOf("WRITE_NO_RESPONSE"), null))),
        )
        assertEquals(SupportLevel.SUPPORTED, ProtocolRegistry.capabilities(withIas, PlatformEvidence())[Feature.FIND_ALERT]!!.level)
        val readOnly = snapshot(
            ServiceDump(Uuids.IMMEDIATE_ALERT, "Immediate Alert", 1, true, listOf(char(Uuids.ALERT_LEVEL, listOf("READ"), null))),
        )
        assertEquals(SupportLevel.UNSUPPORTED, ProtocolRegistry.capabilities(readOnly, PlatformEvidence())[Feature.FIND_ALERT]!!.level)
        // The standard service can never address a single earbud.
        assertEquals(SupportLevel.UNSUPPORTED, ProtocolRegistry.capabilities(withIas, PlatformEvidence())[Feature.FIND_LEFT_RIGHT]!!.level)
    }

    @Test fun powerAndAdvancedFeaturesAreNeverClaimed() {
        val caps = ProtocolRegistry.capabilities(batteryOnly, PlatformEvidence())
        listOf(Feature.REMOTE_POWER_OFF, Feature.REMOTE_POWER_ON, Feature.ANC, Feature.EQUALIZER, Feature.BUTTON_CONFIG).forEach {
            assertEquals(SupportLevel.UNVERIFIED, caps[it]!!.level)
        }
        assertEquals(SupportLevel.UNSUPPORTED, caps[Feature.FIRMWARE_UPDATE]!!.level)
        assertEquals(SupportLevel.UNSUPPORTED, caps[Feature.DISCONNECT_CLASSIC]!!.level)
    }
}
