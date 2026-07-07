package app.siphon

import app.siphon.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun formatsBytesAcrossUnits() {
        assertEquals("—", Formatters.bytes(null))
        assertEquals("—", Formatters.bytes(0))
        assertEquals("512 B", Formatters.bytes(512))
        assertEquals("1.0 KB", Formatters.bytes(1024))
        assertEquals("1.5 MB", Formatters.bytes(1_572_864))
        assertEquals("2.0 GB", Formatters.bytes(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun marksEstimates() {
        assertEquals("≈ 1.0 KB", Formatters.bytes(1024, estimate = true))
    }

    @Test
    fun formatsDurations() {
        assertEquals("", Formatters.duration(null))
        assertEquals("0:45", Formatters.duration(45))
        assertEquals("3:05", Formatters.duration(185))
        assertEquals("1:01:01", Formatters.duration(3661))
    }

    @Test
    fun computesProgressSafely() {
        assertEquals(0, Formatters.progressPercent(10, 0))
        assertEquals(0, Formatters.progressPercent(10, -1))
        assertEquals(50, Formatters.progressPercent(50, 100))
        assertEquals(100, Formatters.progressPercent(150, 100))
    }

    @Test
    fun formatsEta() {
        assertEquals("", Formatters.eta(-1))
        assertEquals("30s left", Formatters.eta(30))
        assertEquals("2m 5s left", Formatters.eta(125))
        assertEquals("1h 1m left", Formatters.eta(3660))
    }
}
