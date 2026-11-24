package app.ht104.control

import app.ht104.control.diagnostics.DiagnosticsReport
import app.ht104.control.diagnostics.GattSnapshot
import app.ht104.control.diagnostics.ReportBuilder
import app.ht104.control.protocol.Capability
import app.ht104.control.protocol.Feature
import app.ht104.control.protocol.SupportLevel
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {
    private val report = DiagnosticsReport(
        generatedAtMs = 1_700_000_000_000,
        appVersion = "test",
        environment = mapOf("model" to "Pixel"),
        permissions = emptyList(),
        adapter = null,
        bondedDevices = emptyList(),
        selectedAddress = null,
        profileStates = emptyList(),
        systemBatteryPercent = null,
        bleAdvertisements = emptyList(),
        gatt = GattSnapshot.IDLE,
        capabilities = listOf(Capability(Feature.ANC, SupportLevel.UNVERIFIED, "no vendor protocol")),
        events = emptyList(),
        buttonProbeLog = emptyList(),
    )

    @Test fun textReportHasSections() {
        val t = ReportBuilder.toText(report)
        assertTrue(t.contains("Diagnostics Report"))
        assertTrue(t.contains("==== GATT ===="))
        assertTrue(t.contains("UNVERIFIED"))
    }

    @Test fun jsonReportIsWellFormedEnough() {
        val j = ReportBuilder.toJson(report)
        assertTrue(j.trimStart().startsWith("{"))
        assertTrue(j.trimEnd().endsWith("}"))
        assertTrue(j.contains("\"capabilities\""))
        assertTrue(j.contains("\"ANC\""))
    }
}
