package io.github.thedude1996.growcalc.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class IrrigationCalculatorTest {

    private fun minutes(vararg values: String) = values.map { WateringInput(it, DurationUnit.MINUTES) }

    @Test
    fun `2 L per h, 2 drippers, 3 min is 200 ml`() =
        assertEquals(200.0, IrrigationCalculator.mlPerPot(2.0, 2, 180.0), 1e-9)

    @Test
    fun `blue preset, 2 drippers, 3 min is 220 ml`() {
        val result = IrrigationCalculator.evaluate(
            IrrigationInput(preset = DripperPreset.BLUE, drippers = 2, waterings = minutes("3"))
        )
        assertEquals(2.2, result.flowLph!!, 1e-9)
        assertEquals(220.0, result.wateringMl.single()!!, 1e-9)
        assertEquals(220.0, result.perPotDayMl!!, 1e-9)
    }

    @Test
    fun `seconds and minutes give the same amount`() {
        val seconds = IrrigationCalculator.evaluate(
            IrrigationInput(waterings = listOf(WateringInput("90", DurationUnit.SECONDS)))
        )
        val minutes = IrrigationCalculator.evaluate(IrrigationInput(waterings = minutes("1,5")))
        assertEquals(55.0, seconds.wateringMl.single()!!, 1e-9)
        assertEquals(55.0, minutes.wateringMl.single()!!, 1e-9)
    }

    @Test
    fun `several waterings add up to the daily amount, pots and tank`() {
        val result = IrrigationCalculator.evaluate(
            IrrigationInput(
                preset = DripperPreset.BLUE,
                drippers = 2,
                waterings = listOf(WateringInput("2", DurationUnit.MINUTES, 8 * 60), WateringInput("3", DurationUnit.MINUTES)),
                potsText = "4",
                tankText = "20"
            )
        )
        assertEquals(146.667, result.wateringMl[0]!!, 1e-3)
        assertEquals(220.0, result.wateringMl[1]!!, 1e-9)
        assertEquals(366.666, result.perPotDayMl!!, 1e-3)
        assertEquals(1_466.666, result.totalDayMl!!, 1e-3)
        assertEquals(13.636, result.tankDays!!, 1e-3)
    }

    @Test
    fun `reverse mode computes the run time`() {
        assertEquals(180.0, IrrigationCalculator.secondsForAmount(220.0, 2.2, 2), 1e-9)
        val result = IrrigationCalculator.evaluate(IrrigationInput(drippers = 2, targetText = "220"))
        assertEquals(180.0, result.targetSeconds!!, 1e-9)
    }

    @Test
    fun `calibration derives the real flow from a measuring cup`() {
        assertEquals(2.4, IrrigationCalculator.flowFromMeasurement(40.0, 60.0, 1), 1e-9)
        assertEquals(1.8, IrrigationCalculator.flowFromMeasurement(60.0, 60.0, 2), 1e-9)
        val calibrated = IrrigationCalculator.evaluate(
            IrrigationInput(preset = null, customFlowText = "2,4", waterings = minutes("1"))
        )
        assertEquals(40.0, calibrated.wateringMl.single()!!, 1e-9)
    }

    @Test
    fun `empty fields hide results instead of showing zero`() {
        val result = IrrigationCalculator.evaluate(IrrigationInput())
        assertNull(result.wateringMl.single())
        assertNull(result.perPotDayMl)
        assertNull(result.totalDayMl)
        assertNull(result.tankDays)
        assertNull(result.targetSeconds)
        assertFalse(result.wateringInvalid.single())
        assertEquals(1, result.pots)
    }

    @Test
    fun `zero, negative and invalid values are flagged`() {
        val result = IrrigationCalculator.evaluate(
            IrrigationInput(
                preset = null,
                customFlowText = "0",
                waterings = minutes("-1", "abc"),
                potsText = "1,5",
                tankText = "0",
                targetText = "-5"
            )
        )
        assertNull(result.flowLph)
        assertTrue(result.flowInvalid)
        assertEquals(listOf(true, true), result.wateringInvalid)
        assertTrue(result.potsInvalid)
        assertTrue(result.tankInvalid)
        assertTrue(result.targetInvalid)
    }

    @Test
    fun `an empty extra row is ignored, an invalid one hides the daily total`() {
        val withEmpty = IrrigationCalculator.evaluate(IrrigationInput(waterings = minutes("3", "")))
        assertEquals(110.0, withEmpty.perPotDayMl!!, 1e-9)

        val withInvalid = IrrigationCalculator.evaluate(IrrigationInput(waterings = minutes("3", "x")))
        assertEquals(110.0, withInvalid.wateringMl[0]!!, 1e-9)
        assertNull(withInvalid.perPotDayMl)
    }

    @Test
    fun `comma and dot are accepted`() {
        assertEquals(2.45, IrrigationCalculator.parsePositive("2,45")!!, 1e-9)
        assertEquals(2.45, IrrigationCalculator.parsePositive(" 2.45 ")!!, 1e-9)
        assertNull(IrrigationCalculator.parsePositive(""))
        assertNull(IrrigationCalculator.parsePositive("5", max = 4.0))
    }

    @Test
    fun `volumes switch to liters from 1000 ml`() {
        assertEquals("220 ml", IrrigationCalculator.formatVolume(220.0))
        assertEquals("7,5 ml", IrrigationCalculator.formatVolume(7.5))
        assertEquals("999 ml", IrrigationCalculator.formatVolume(999.4))
        assertEquals("1 L", IrrigationCalculator.formatVolume(1_000.0))
        assertEquals("1,47 L", IrrigationCalculator.formatVolume(1_466.666))
        assertEquals("1.200 L", IrrigationCalculator.formatVolume(1_200_000.0))
    }

    @Test
    fun `durations and tank times are formatted`() {
        assertEquals("45 s", IrrigationCalculator.formatDuration(45.0))
        assertEquals("1:30 min", IrrigationCalculator.formatDuration(90.0))
        assertEquals("3:00 min", IrrigationCalculator.formatDuration(179.6))
        assertEquals("1:02:05 h", IrrigationCalculator.formatDuration(3_725.0))
        assertEquals(TankDuration.Days("13,6", isOne = false), IrrigationCalculator.tankDuration(13.636))
        assertEquals(TankDuration.Days("1", isOne = true), IrrigationCalculator.tankDuration(1.02))
        assertEquals(TankDuration.Hours(18), IrrigationCalculator.tankDuration(0.75))
        assertEquals(TankDuration.Hours(1), IrrigationCalculator.tankDuration(0.001))
        assertEquals("08:05", IrrigationCalculator.formatTime(8 * 60 + 5))
    }

    @Test
    fun `english locale uses dot decimals and comma grouping`() {
        assertEquals("7.5 ml", IrrigationCalculator.formatVolume(7.5, Locale.US))
        assertEquals("1.47 L", IrrigationCalculator.formatVolume(1_466.666, Locale.US))
        assertEquals("1,200 L", IrrigationCalculator.formatVolume(1_200_000.0, Locale.US))
        assertEquals("2.45", IrrigationCalculator.formatNumber(2.45, Locale.US))
        assertEquals("2,45", IrrigationCalculator.formatNumber(2.45))
        assertEquals(TankDuration.Days("13.6", isOne = false), IrrigationCalculator.tankDuration(13.636, Locale.US))
    }

    @Test
    fun `formatted numbers parse back in both languages`() {
        for (locale in listOf(Locale.GERMANY, Locale.US)) {
            val text = IrrigationCalculator.formatNumber(2.45, locale)
            assertEquals(2.45, IrrigationCalculator.parsePositive(text)!!, 1e-9)
        }
    }
}
