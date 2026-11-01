package io.github.thedude1996.growcalc.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import kotlin.math.roundToLong

class LuxPpfdConverterTest {

    private fun ppfd(lux: Double, source: LightSource) = LuxPpfdConverter.luxToPpfd(lux, source).roundToLong()

    @Test
    fun `30000 lux sunlight is 540 ppfd`() = assertEquals(540L, ppfd(30_000.0, LightSource.SUNLIGHT))

    @Test
    fun `30000 lux LED is 420 ppfd`() = assertEquals(420L, ppfd(30_000.0, LightSource.LED_FULL_SPECTRUM))

    @Test
    fun `20000 lux HPS is 280 ppfd`() = assertEquals(280L, ppfd(20_000.0, LightSource.HPS))

    @Test
    fun `10000 lux fluorescent is 130 ppfd`() = assertEquals(130L, ppfd(10_000.0, LightSource.FLUORESCENT))

    @Test
    fun `light sources cycle in both directions`() {
        assertEquals(LightSource.LED_FULL_SPECTRUM, LightSource.SUNLIGHT.next())
        assertEquals(LightSource.SUNLIGHT, LightSource.FLUORESCENT.next())
        assertEquals(LightSource.FLUORESCENT, LightSource.SUNLIGHT.previous())
        assertEquals(LightSource.HPS, LightSource.FLUORESCENT.previous())
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
        assertEquals(1.5, LuxPpfdConverter.parseCalibration(" 1.5 ")!!, 1e-9)
        assertNull(LuxPpfdConverter.parseCalibration("0"))
        assertNull(LuxPpfdConverter.parseCalibration("abc"))
        assertNull(LuxPpfdConverter.parseCalibration(""))
        assertNull(LuxPpfdConverter.parseCalibration(null))
    }

    @Test
    fun `lux is rounded with german thousands separators`() {
        assertEquals("30.000", LuxPpfdConverter.formatRounded(30_000.000_000_004))
        assertEquals("1.235", LuxPpfdConverter.formatRounded(1_234.5))
        assertEquals("161", LuxPpfdConverter.formatRounded(161.2))
    }

    @Test
    fun `low ppfd shows one decimal, high ppfd whole numbers`() {
        assertEquals("2,9", LuxPpfdConverter.formatPpfd(2.88))
        assertEquals("0,0", LuxPpfdConverter.formatPpfd(0.0))
        assertEquals("99,9", LuxPpfdConverter.formatPpfd(99.94))
        assertEquals("540", LuxPpfdConverter.formatPpfd(540.4))
        assertEquals("1.350", LuxPpfdConverter.formatPpfd(1_350.0))
    }

    @Test
    fun `english locale formats lux and ppfd with english separators`() {
        assertEquals("30,000", LuxPpfdConverter.formatRounded(30_000.0, Locale.US))
        assertEquals("2.9", LuxPpfdConverter.formatPpfd(2.88, Locale.US))
        assertEquals("1,350", LuxPpfdConverter.formatPpfd(1_350.0, Locale.US))
    }
}
