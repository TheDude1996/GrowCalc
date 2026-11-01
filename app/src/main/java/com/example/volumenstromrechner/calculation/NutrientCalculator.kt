package com.example.volumenstromrechner.calculation

import com.example.volumenstromrechner.model.FeedProgram
import com.example.volumenstromrechner.model.FeedWeek
import com.example.volumenstromrechner.model.NutrientComponent

enum class CaMgMode(val label: String, val ratio: Double?) {
    MANUFACTURER("Original", null),
    RATIO_2_0("2.0 : 1", 2.0),
    RATIO_2_5("2.5 : 1", 2.5),
    RATIO_3_0("3.0 : 1", 3.0),
    RATIO_3_5("3.5 : 1", 3.5)
}

enum class ComponentKind { PRODUCT, ADDITIVE }

data class FinalComponent(val name: String, val amountTotal: Double, val unit: String, val kind: ComponentKind)

data class NutrientValues(val fromWater: Double, val fromFertilizer: Double, val total: Double)

data class NutrientAnalysis(
    val n: NutrientValues,
    val p: NutrientValues,
    val k: NutrientValues,
    val ca: NutrientValues,
    val mg: NutrientValues,
    val finalRatio: Double
)

data class WaterAnalysisReport(
    val fertMg: Double,
    val epsomMg: Double,
    val totalMg: Double,
    val targetCa: Double,
    val extraCaNeeded: Double,
    val finalCaDosage: Double
)

data class CalculationResult(
    val totalEc: Double,
    val components: List<FinalComponent>,
    val analysis: NutrientAnalysis,
    val waterAnalysis: WaterAnalysisReport,
    val isFlush: Boolean,
    val mode: CaMgMode
)

data class WaterInput(val ca: Double, val mg: Double, val ec: Double)

object NutrientCalculator {

    /** Name of the dosable calcium product used for ratio correction. */
    const val CALCIUM = "Calcium"

    /** Bittersalz (MgSO4 · 7 H2O) contains ~9.8 % Mg, i.e. 98 mg Mg per gram. */
    const val EPSOM_MG_PER_GRAM = 98.0

    /** Minimum magnesium level the ratio mode corrects to (mg/L). */
    const val MIN_MG = 20.0

    // Ratio correction is only possible if the program contains a dosable calcium product
    fun supportsRatioMode(program: FeedProgram?): Boolean =
        program?.weeks?.any { week -> week.components.any { it.name == CALCIUM } } == true

    /**
     * Calculates the nutrient solution for one week.
     *
     * @param volumeLiters mixing volume, must be > 0
     */
    fun calculate(
        program: FeedProgram,
        week: FeedWeek,
        water: WaterInput,
        volumeLiters: Double,
        requestedMode: CaMgMode
    ): CalculationResult {
        val mode = if (supportsRatioMode(program)) requestedMode else CaMgMode.MANUFACTURER
        val isFlush = week.components.isEmpty()
        val calciumComp = week.components.find { it.name == CALCIUM }

        // 1. Baseline contribution of the fertilizers (without the calcium correction product)
        var fertMg = 0.0
        var tableCa = 0.0
        week.components.filter { it.name != CALCIUM }.forEach { comp ->
            fertMg += comp.mgPerLiter(comp.dosage, comp.profile.mg)
            tableCa += comp.mgPerLiter(comp.dosage, comp.profile.ca)
        }

        var epsomDosage = 0.0
        var caDosage = calciumComp?.dosage ?: 0.0
        var extraCaNeeded = 0.0
        var totalMgVal = water.mg + fertMg
        val targetCaVal: Double

        if (!isFlush && mode.ratio != null && calciumComp != null) {
            // Magnesium correction
            if (totalMgVal < MIN_MG) {
                epsomDosage = (MIN_MG - totalMgVal) / EPSOM_MG_PER_GRAM
                totalMgVal = MIN_MG
            }

            // Calcium dosage based on target ratio
            targetCaVal = totalMgVal * mode.ratio
            extraCaNeeded = (targetCaVal - (water.ca + tableCa)).coerceAtLeast(0.0)
            val caPerUnit = calciumComp.mgPerLiter(1.0, calciumComp.profile.ca)
            caDosage = if (caPerUnit > 0) extraCaNeeded / caPerUnit else 0.0
        } else {
            // Manufacturer mode or flush: use table dosage
            val tableCalcium = calciumComp?.let { it.mgPerLiter(caDosage, it.profile.ca) } ?: 0.0
            targetCaVal = water.ca + tableCa + tableCalcium
        }

        // 2. Build final list and sum all nutrients
        val finalComponents = mutableListOf<FinalComponent>()
        var sumN = 0.0; var sumP = 0.0; var sumK = 0.0; var sumCa = 0.0; var sumMg = 0.0

        if (epsomDosage > 0) {
            finalComponents.add(FinalComponent("Bittersalz", epsomDosage * volumeLiters, "g", ComponentKind.ADDITIVE))
            sumMg += epsomDosage * EPSOM_MG_PER_GRAM
        }

        week.components.forEach { comp ->
            val dosage = if (comp.name == CALCIUM) caDosage else comp.dosage
            if (dosage > 0) {
                finalComponents.add(FinalComponent(comp.name, dosage * volumeLiters, comp.unit, ComponentKind.PRODUCT))
                sumN += comp.mgPerLiter(dosage, comp.profile.n)
                sumP += comp.mgPerLiter(dosage, comp.profile.p2o5)
                sumK += comp.mgPerLiter(dosage, comp.profile.k2o)
                sumCa += comp.mgPerLiter(dosage, comp.profile.ca)
                sumMg += comp.mgPerLiter(dosage, comp.profile.mg)
            }
        }

        val totalCa = water.ca + sumCa
        val totalMg = water.mg + sumMg

        return CalculationResult(
            totalEc = water.ec + week.targetEc,
            components = finalComponents,
            isFlush = isFlush,
            mode = mode,
            waterAnalysis = WaterAnalysisReport(fertMg, epsomDosage * EPSOM_MG_PER_GRAM, totalMgVal, targetCaVal, extraCaNeeded, caDosage),
            analysis = NutrientAnalysis(
                n = NutrientValues(0.0, sumN, sumN),
                p = NutrientValues(0.0, sumP, sumP),
                k = NutrientValues(0.0, sumK, sumK),
                ca = NutrientValues(water.ca, sumCa, totalCa),
                mg = NutrientValues(water.mg, sumMg, totalMg),
                finalRatio = if (totalMg > 0) totalCa / totalMg else 0.0
            )
        )
    }

    /**
     * mg/L of a nutrient for a dosage per liter.
     * Percentages are by weight, so liquid products (ml) are converted to grams via their density.
     * 1 g/L at 1 % = 10 mg/L.
     */
    private fun NutrientComponent.mgPerLiter(dosage: Double, percent: Double): Double =
        dosage * density * 10.0 * percent
}
