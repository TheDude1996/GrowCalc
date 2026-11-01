package io.github.thedude1996.growcalc.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.roundToLong

class LuxPpfdConverterTest {

    private fun rounded(value: Double?) = value!!.roundToLong()

    @Test
    fun `30000 lux sunlight is 540 ppfd`() {
        assertEquals(540L, rounded(LuxPpfdConverter.luxToPpfd(30_000.0, LightSource.SUNLIGHT)))
    }

    @Test
    fun `30000 lux LED is 420 ppfd`() {
        assertEquals(420L, rounded(LuxPpfdConverter.luxToPpfd(30_000.0, LightSource.LED_FULL_SPECTRUM)))
    }

    @Test
    fun `10000 lux fluorescent is 130 ppfd`() {
        assertEquals(130L, rounded(LuxPpfdConverter.luxToPpfd(10_000.0, LightSource.FLUORESCENT)))
    }

    @Test
    fun `540 ppfd sunlight is 30000 lux`() {
        assertEquals(30_000L, rounded(LuxPpfdConverter.ppfdToLux(540.0, LightSource.SUNLIGHT)))
    }

    @Test
    fun `convert respects direction`() {
        assertEquals(280L, rounded(LuxPpfdConverter.convert(20_000.0, ConversionDirection.LUX_TO_PPFD, LightSource.HPS)))
        assertEquals(20_000L, rounded(LuxPpfdConverter.convert(280.0, ConversionDirection.PPFD_TO_LUX, LightSource.HPS)))
    }

    @Test
    fun `direction swaps back and forth`() {
        assertEquals(ConversionDirection.PPFD_TO_LUX, ConversionDirection.LUX_TO_PPFD.swapped())
        assertEquals(ConversionDirection.LUX_TO_PPFD, ConversionDirection.PPFD_TO_LUX.swapped())
    }

    @Test
    fun `parse accepts comma, dot and german thousands grouping`() {
        assertEquals(12.5, LuxPpfdConverter.parseInput("12,5")!!, 1e-9)
        assertEquals(12.5, LuxPpfdConverter.parseInput(" 12.5 ")!!, 1e-9)
        assertEquals(30_000.0, LuxPpfdConverter.parseInput("30.000")!!, 1e-9)
        assertEquals(1_250_000.0, LuxPpfdConverter.parseInput("1.250.000")!!, 1e-9)
        assertEquals(0.0, LuxPpfdConverter.parseInput("0")!!, 1e-9)
    }

    @Test
    fun `invalid or empty input gives no result`() {
        listOf(null, "", "   ", "abc", "-5", "1,2,3", ",", "NaN", "Infinity", "1e400").forEach {
            assertNull("input '$it'", LuxPpfdConverter.parseInput(it))
            assertNull(LuxPpfdConverter.convertInput(it, ConversionDirection.LUX_TO_PPFD, LightSource.SUNLIGHT))
        }
    }

    @Test
    fun `calibration multiplies the raw sensor value`() {
        assertEquals(1_200.0, LuxPpfdConverter.applyCalibration(1_000.0, 1.2), 1e-9)
        assertEquals(1_000.0, LuxPpfdConverter.applyCalibration(1_000.0, LuxPpfdConverter.DEFAULT_CALIBRATION), 1e-9)
    }

    @Test
    fun `calibration range is validated`() {
        assertTrue(LuxPpfdConverter.isValidCalibration(1.0))
        assertTrue(LuxPpfdConverter.isValidCalibration(0.1))
        assertTrue(LuxPpfdConverter.isValidCalibration(10.0))
        assertFalse(LuxPpfdConverter.isValidCalibration(0.0))
        assertFalse(LuxPpfdConverter.isValidCalibration(10.5))
        assertFalse(LuxPpfdConverter.isValidCalibration(Double.NaN))
        assertFalse(LuxPpfdConverter.isValidCalibration(null))
    }

    @Test
    fun `calibration text is parsed with comma or dot`() {
        assertEquals(1.15, LuxPpfdConverter.parseCalibration("1,15")!!, 1e-9)
        assertEquals(1.5, LuxPpfdConverter.parseCalibration("1.5")!!, 1e-9)
        assertNull(LuxPpfdConverter.parseCalibration("0"))
        assertNull(LuxPpfdConverter.parseCalibration("abc"))
        assertNull(LuxPpfdConverter.parseCalibration(""))
    }

    @Test
    fun `results are rounded and use thousands separators`() {
        assertEquals("30.000", LuxPpfdConverter.formatRounded(30_000.000_000_004, Locale.GERMANY))
        assertEquals("1.235", LuxPpfdConverter.formatRounded(1_234.5, Locale.GERMANY))
        assertEquals("540", LuxPpfdConverter.formatRounded(540.4, Locale.GERMANY))
    }

    @Test
    fun `typical table covers all lux values and light sources`() {
        val table = LuxPpfdConverter.typicalTable()
        assertEquals(LuxPpfdConverter.TYPICAL_LUX_VALUES, table.map { it.first })
        table.forEach { (_, values) -> assertEquals(LightSource.entries.size, values.size) }
        val (lux, values) = table.first()
        assertEquals(5_000, lux)
        assertEquals(listOf(90L, 70L, 70L, 65L), values.map { it.roundToLong() })
    }
}
