package io.github.thedude1996.growcalc.calculation

import kotlin.math.PI
import kotlin.math.exp

enum class VpdStatus { LOW, IDEAL, HIGH, DANGER }

enum class DliLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

data class VpdResult(val leafTemp: Double, val vpdAir: Double, val vpdLeaf: Double, val status: VpdStatus)

object ClimateCalculator {

    /** Air volume flow in m³/h for a round duct. */
    fun volumeFlow(diameterMm: Int, velocityMs: Double): Double {
        val radiusM = diameterMm / 2000.0
        return PI * radiusM * radiusM * velocityMs * 3600.0
    }

    /** Saturation vapour pressure in kPa (Tetens equation). */
    fun saturationVaporPressure(temp: Double): Double =
        0.61078 * exp((17.27 * temp) / (temp + 237.3))

    fun vpd(airTemp: Double, humidityPercent: Double, leafOffset: Double): VpdResult {
        val leafTemp = airTemp + leafOffset
        val avp = saturationVaporPressure(airTemp) * (humidityPercent / 100.0)
        val vpdAir = saturationVaporPressure(airTemp) - avp
        val vpdLeaf = saturationVaporPressure(leafTemp) - avp
        val status = when {
            vpdLeaf < 0.4 -> VpdStatus.LOW
            vpdLeaf <= 1.2 -> VpdStatus.IDEAL
            vpdLeaf <= 1.6 -> VpdStatus.HIGH
            else -> VpdStatus.DANGER
        }
        return VpdResult(leafTemp, vpdAir, vpdLeaf, status)
    }

    /** Daily light integral in mol/m²/day: PPFD × hours × 3600 / 1 000 000. */
    fun dli(ppfd: Double, hours: Double): Double = ppfd * hours * 0.0036

    fun dliLevel(dli: Double): DliLevel = when {
        dli < 10 -> DliLevel.LOW
        dli <= 20 -> DliLevel.MEDIUM
        dli <= 40 -> DliLevel.HIGH
        else -> DliLevel.VERY_HIGH
    }
}
