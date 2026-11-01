package com.example.volumenstromrechner.calculation

import com.example.volumenstromrechner.model.FeedProgram
import com.example.volumenstromrechner.model.FeedWeek
import com.example.volumenstromrechner.model.NutrientComponent
import com.example.volumenstromrechner.model.NutrientDataProvider
import com.example.volumenstromrechner.model.NutrientProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientCalculatorTest {

    private val grow = NutrientComponent("Grow", 1.0, "g", NutrientProfile(n = 24.0, mg = 1.2))
    private val calcium = NutrientComponent(NutrientCalculator.CALCIUM, 0.3, "g", NutrientProfile(ca = 9.0))
    private val noWater = WaterInput(ca = 0.0, mg = 0.0, ec = 0.0)

    private val feedWeek = FeedWeek("Woche 1", 1.2, listOf(grow, calcium))
    private val flushWeek = FeedWeek("Flush", 0.2, emptyList())
    private val ghfLike = FeedProgram("GHF", listOf(feedWeek, flushWeek))
    private val withoutCalcium = FeedProgram("Other", listOf(FeedWeek("Woche 1", 1.2, listOf(grow))))

    @Test
    fun ratioMode_correctsMagnesiumAndCalcium() {
        val result = NutrientCalculator.calculate(ghfLike, feedWeek, noWater, 10.0, CaMgMode.RATIO_3_0)

        // Grow 1 g/L × 1.2 % = 12 mg/L Mg → corrected to 20 mg/L with Bittersalz
        assertEquals(12.0, result.waterAnalysis.fertMg, 1e-9)
        assertEquals(8.0, result.waterAnalysis.epsomMg, 1e-9)
        assertEquals(20.0, result.analysis.mg.total, 1e-9)

        // 20 mg/L Mg × 3 = 60 mg/L Ca → 60 / 90 mg per gram
        assertEquals(60.0, result.analysis.ca.total, 1e-9)
        assertEquals(60.0 / 90.0, result.waterAnalysis.finalCaDosage, 1e-9)
        assertEquals(3.0, result.analysis.finalRatio, 1e-9)

        val epsom = result.components.single { it.name == "Bittersalz" }
        assertEquals(8.0 / 98.0 * 10.0, epsom.amountTotal, 1e-9)
        assertEquals(ComponentKind.ADDITIVE, epsom.kind)
    }

    @Test
    fun ratioMode_waterCalciumReducesDosage() {
        val water = WaterInput(ca = 40.0, mg = 10.0, ec = 0.4)
        val result = NutrientCalculator.calculate(ghfLike, feedWeek, water, 1.0, CaMgMode.RATIO_3_0)

        // Mg: 10 water + 12 fertilizer = 22 → no Bittersalz, target Ca = 66
        assertTrue(result.components.none { it.name == "Bittersalz" })
        assertEquals(26.0, result.waterAnalysis.extraCaNeeded, 1e-9)
        assertEquals(3.0, result.analysis.finalRatio, 1e-9)
        assertEquals(1.6, result.totalEc, 1e-9)
    }

    @Test
    fun manufacturerMode_usesTableDosage() {
        val result = NutrientCalculator.calculate(ghfLike, feedWeek, noWater, 1.0, CaMgMode.MANUFACTURER)

        assertTrue(result.components.none { it.name == "Bittersalz" })
        assertEquals(0.3, result.components.single { it.name == NutrientCalculator.CALCIUM }.amountTotal, 1e-9)
        assertEquals(27.0, result.analysis.ca.total, 1e-9)
    }

    @Test
    fun flushWeek_withNonZeroEc_addsNothing() {
        val result = NutrientCalculator.calculate(ghfLike, flushWeek, noWater, 10.0, CaMgMode.RATIO_3_0)

        assertTrue(result.isFlush)
        assertTrue(result.components.isEmpty())
    }

    @Test
    fun programWithoutCalcium_fallsBackToManufacturerMode() {
        assertFalse(NutrientCalculator.supportsRatioMode(withoutCalcium))

        val week = withoutCalcium.weeks[0]
        val result = NutrientCalculator.calculate(withoutCalcium, week, noWater, 10.0, CaMgMode.RATIO_3_0)

        assertEquals(CaMgMode.MANUFACTURER, result.mode)
        assertTrue(result.components.none { it.name == "Bittersalz" })
    }

    @Test
    fun liquidProduct_usesDensity() {
        val liquid = NutrientComponent("CaMg", 1.0, "ml", NutrientProfile(mg = 1.0), density = 1.2)
        val program = FeedProgram("Liquid", listOf(FeedWeek("W1", 1.0, listOf(liquid))))
        val result = NutrientCalculator.calculate(program, program.weeks[0], noWater, 1.0, CaMgMode.MANUFACTURER)

        // 1 ml × 1.2 g/ml × 1 % = 12 mg/L
        assertEquals(12.0, result.analysis.mg.total, 1e-9)
    }

    @Test
    fun allGhfPrograms_reachTargetRatioWithSoftWater() {
        val ghf = NutrientDataProvider.manufacturers.single { it.name == "Green House Feeding" }
        ghf.lines.flatMap { it.programs }.forEach { program ->
            assertTrue(program.name, NutrientCalculator.supportsRatioMode(program))
            program.weeks.filter { it.components.isNotEmpty() }.forEach { week ->
                val result = NutrientCalculator.calculate(program, week, noWater, 10.0, CaMgMode.RATIO_3_0)
                assertEquals("${program.name} / ${week.label}", 3.0, result.analysis.finalRatio, 1e-6)
            }
        }
    }

    @Test
    fun otherManufacturers_doNotSupportRatioMode() {
        NutrientDataProvider.manufacturers
            .filter { it.name != "Green House Feeding" }
            .flatMap { m -> m.lines.flatMap { it.programs } }
            .forEach { assertFalse(it.name, NutrientCalculator.supportsRatioMode(it)) }
        assertNull(CaMgMode.MANUFACTURER.ratio)
    }
}
