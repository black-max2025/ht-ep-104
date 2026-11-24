package app.ht104.control

import app.ht104.control.core.Json
import app.ht104.control.core.TargetMatcher
import app.ht104.control.core.Uuids
import app.ht104.control.core.toHex
import app.ht104.control.core.toPrintableText
import app.ht104.control.diagnostics.GattStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test fun shortUuidRoundTrip() {
        assertEquals("0000180f-0000-1000-8000-00805f9b34fb", Uuids.BATTERY_SERVICE.toString())
        assertEquals(0x180F, Uuids.shortId(Uuids.BATTERY_SERVICE))
        assertEquals("0x2A19", Uuids.display(Uuids.BATTERY_LEVEL))
        assertEquals("Battery", Uuids.serviceName(Uuids.BATTERY_SERVICE))
    }

    @Test fun vendorUuidIsNotShort() {
        val vendor = java.util.UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        assertNull(Uuids.shortId(vendor))
        assertTrue(Uuids.isVendorSpecific(vendor))
        assertEquals(vendor.toString(), Uuids.display(vendor))
    }

    @Test fun hexAndText() {
        assertEquals("0A FF 10", byteArrayOf(0x0A, 0xFF.toByte(), 0x10).toHex())
        assertEquals("ABC", byteArrayOf(65, 66, 67, 0).toPrintableText())
        assertNull(byteArrayOf(1, 2, 3).toPrintableText())
        assertNull(byteArrayOf().toPrintableText())
    }

    @Test fun jsonEscapesAndNests() {
        val out = Json.encode(mapOf("a" to "x\"y\n", "b" to listOf(1, true, null), "c" to emptyMap<String, Any>()), pretty = false)
        assertEquals("{\"a\":\"x\\\"y\\n\",\"b\":[1,true,null],\"c\":{}}", out)
    }

    @Test fun gattStatusNames() {
        assertEquals("133 (GATT_ERROR)", GattStatus.describe(133))
        assertEquals("0 (GATT_SUCCESS)", GattStatus.describe(0))
        assertEquals("999 (UNKNOWN)", GattStatus.describe(999))
    }

    @Test fun targetMatcher() {
        assertTrue(TargetMatcher.matchesName("HT-EP104"))
        assertTrue(TargetMatcher.matchesName("hootoo ht ep104"))
        assertFalse(TargetMatcher.matchesName("Galaxy Buds"))
        assertFalse(TargetMatcher.matchesName(null))
    }
}
