package io.github.thedude1996.growcalc.calculation

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

/** Common drippers, named after their color code. */
enum class DripperPreset(val flowLph: Double) {
    PURPLE(1.1),
    BLUE(2.2),
    BROWN(3.2),
    GREEN(4.0),
    RED(8.0)
}

enum class DurationUnit(val seconds: Int) {
    SECONDS(1),
    MINUTES(60);

    fun other() = if (this == SECONDS) MINUTES else SECONDS
}

/** One watering per day, e.g. 08:00 for 2 min. [timeOfDay] is minutes after midnight and only informational. */
data class WateringInput(
    val durationText: String = "",
    val unit: DurationUnit = DurationUnit.MINUTES,
    val timeOfDay: Int? = null
)

/** Raw user input as typed; parsed by [IrrigationCalculator.evaluate]. [preset] null means custom flow. */
data class IrrigationInput(
    val preset: DripperPreset? = DripperPreset.BLUE,
    val customFlowText: String = "",
    val drippers: Int = 1,
    val waterings: List<WateringInput> = listOf(WateringInput()),
    val potsText: String = "",
    val tankText: String = "",
    val targetText: String = ""
)

data class IrrigationResult(
    /** Flow per dripper in L/h, null if missing or invalid. */
    val flowLph: Double?,
    val flowInvalid: Boolean,
    /** ml per pot for each watering, null if that row is empty or invalid. */
    val wateringMl: List<Double?>,
    val wateringInvalid: List<Boolean>,
    /** Sum over all waterings for one pot; null unless at least one row is valid and none is invalid. */
    val perPotDayMl: Double?,
    val pots: Int,
    val potsInvalid: Boolean,
    val totalDayMl: Double?,
    /** How many days the tank lasts; null without a valid tank volume. */
    val tankDays: Double?,
    val tankInvalid: Boolean,
    /** Run time needed for the target amount per pot (reverse mode). */
    val targetSeconds: Double?,
    val targetInvalid: Boolean
)

/**
 * Drip irrigation: amount per pot = flow (L/h) × drippers × run time.
 * Internally everything is in seconds and ml: ml = L/h × drippers × s / 3.6.
 */
object IrrigationCalculator {

    const val MIN_DRIPPERS = 1
    const val MAX_DRIPPERS = 10
    const val MAX_WATERINGS = 24
    const val MAX_POTS = 999
    private const val MAX_FLOW_LPH = 1_000.0
    private const val MAX_SECONDS = 24 * 3_600.0
    private const val ML_PER_LITER = 1_000.0
    private const val SECONDS_PER_HOUR_PER_ML = 3.6 // (s/h) / (ml/L)

    private val GERMAN = DecimalFormatSymbols(Locale.GERMANY)

    fun mlPerPot(flowLph: Double, drippers: Int, seconds: Double): Double =
        flowLph * drippers * seconds / SECONDS_PER_HOUR_PER_ML

    /** Reverse mode: run time in seconds for [targetMl] per pot. */
    fun secondsForAmount(targetMl: Double, flowLph: Double, drippers: Int): Double =
        targetMl * SECONDS_PER_HOUR_PER_ML / (flowLph * drippers)

    /** Calibration: real flow per dripper from a measuring-cup test. */
    fun flowFromMeasurement(measuredMl: Double, seconds: Double, drippers: Int): Double =
        measuredMl * SECONDS_PER_HOUR_PER_ML / (seconds * drippers)

    fun tankDays(tankLiters: Double, dailyMl: Double): Double = tankLiters * ML_PER_LITER / dailyMl

    fun evaluate(input: IrrigationInput): IrrigationResult {
        val flowText = if (input.preset == null) input.customFlowText else null
        val flow = input.preset?.flowLph ?: parsePositive(flowText, MAX_FLOW_LPH)
        val flowInvalid = flowText != null && flow == null && flowText.isNotBlank()
        val drippers = input.drippers.coerceIn(MIN_DRIPPERS, MAX_DRIPPERS)

        val seconds = input.waterings.map { w ->
            parsePositive(w.durationText, MAX_SECONDS / w.unit.seconds)?.times(w.unit.seconds)
        }
        val wateringInvalid = input.waterings.mapIndexed { i, w -> w.durationText.isNotBlank() && seconds[i] == null }
        val wateringMl = seconds.map { s -> if (flow != null && s != null) mlPerPot(flow, drippers, s) else null }

        val perPotDayMl = if (wateringInvalid.none { it } && wateringMl.any { it != null }) {
            wateringMl.sumOf { it ?: 0.0 }
        } else null

        val potsParsed = parsePositiveInt(input.potsText, MAX_POTS)
        val potsInvalid = input.potsText.isNotBlank() && potsParsed == null
        val pots = potsParsed ?: 1
        val totalDayMl = if (potsInvalid) null else perPotDayMl?.times(pots)

        val tank = parsePositive(input.tankText)
        val tankInvalid = input.tankText.isNotBlank() && tank == null
        val tankDays = if (tank != null && totalDayMl != null) tankDays(tank, totalDayMl) else null

        val target = parsePositive(input.targetText)
        val targetInvalid = input.targetText.isNotBlank() && target == null
        val targetSeconds = if (target != null && flow != null) secondsForAmount(target, flow, drippers) else null

        return IrrigationResult(
            flowLph = flow,
            flowInvalid = flowInvalid,
            wateringMl = wateringMl,
            wateringInvalid = wateringInvalid,
            perPotDayMl = perPotDayMl,
            pots = pots,
            potsInvalid = potsInvalid,
            totalDayMl = totalDayMl,
            tankDays = tankDays,
            tankInvalid = tankInvalid,
            targetSeconds = targetSeconds,
            targetInvalid = targetInvalid
        )
    }

    /** Parses a number > 0 (comma or dot); null if empty, invalid or above [max]. */
    fun parsePositive(text: String?, max: Double = Double.MAX_VALUE): Double? {
        val value = text?.trim()?.replace(',', '.')?.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value > 0 && value <= max) value else null
    }

    fun parsePositiveInt(text: String?, max: Int = Int.MAX_VALUE): Int? {
        val value = text?.trim()?.toIntOrNull() ?: return null
        return if (value in 1..max) value else null
    }

    /** "220 ml", "7,5 ml", from 1000 ml on in liters: "1,32 L". */
    fun formatVolume(ml: Double): String = when {
        ml >= ML_PER_LITER -> DecimalFormat("#,##0.##", GERMAN).format(ml / ML_PER_LITER) + " L"
        ml < 10 -> DecimalFormat("0.#", GERMAN).format(ml) + " ml"
        else -> "${ml.roundToLong()} ml"
    }

    /** "45 s", "1:30 min", "1:02:05 h" (rounded to whole seconds). */
    fun formatDuration(seconds: Double): String {
        val total = seconds.roundToLong().coerceAtLeast(1)
        val h = total / 3_600
        val m = (total % 3_600) / 60
        val s = total % 60
        return when {
            h > 0 -> "%d:%02d:%02d h".format(Locale.GERMANY, h, m, s)
            m > 0 -> "%d:%02d min".format(Locale.GERMANY, m, s)
            else -> "$s s"
        }
    }

    /** "4,5 Tage", "1 Tag", below one day in hours: "18 Std.". */
    fun formatTankDuration(days: Double): String {
        if (days < 1) return "${(days * 24).roundToLong().coerceAtLeast(1)} Std."
        val text = DecimalFormat("#,##0.#", GERMAN).format(days)
        return if (text == "1") "1 Tag" else "$text Tage"
    }

    /** Flow for display and text fields: "2,2", "2,45". */
    fun formatNumber(value: Double): String = DecimalFormat("0.##", GERMAN).format(value)

    fun formatTime(minutesOfDay: Int): String = "%02d:%02d".format(Locale.GERMANY, minutesOfDay / 60, minutesOfDay % 60)
}
