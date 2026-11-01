package io.github.thedude1996.growcalc.calculation

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/** Light source with its approximate lux → PPFD conversion factor (µmol/m²/s per lx). */
enum class LightSource(val factor: Double) {
    SUNLIGHT(0.018),
    LED_FULL_SPECTRUM(0.014),
    HPS(0.014),
    FLUORESCENT(0.013);

    fun next(): LightSource = entries[(ordinal + 1) % entries.size]
    fun previous(): LightSource = entries[(ordinal - 1 + entries.size) % entries.size]
}

/** UI-independent conversion from illuminance (lux) to PPFD (µmol/m²/s). */
object LuxPpfdConverter {

    const val DEFAULT_CALIBRATION = 1.0
    const val MIN_CALIBRATION = 0.1
    const val MAX_CALIBRATION = 10.0

    /** Below this PPFD one decimal is shown, above it whole numbers. */
    private const val DECIMAL_THRESHOLD = 100.0

    fun luxToPpfd(lux: Double, source: LightSource): Double = lux * source.factor

    /** Multiplies a raw sensor reading with the user's calibration factor. */
    fun applyCalibration(rawLux: Double, calibrationFactor: Double): Double = rawLux * calibrationFactor

    fun isValidCalibration(factor: Double?): Boolean =
        factor != null && factor.isFinite() && factor in MIN_CALIBRATION..MAX_CALIBRATION

    /** Parses a calibration factor ("1,15" or "1.15"); null if invalid or out of range. */
    fun parseCalibration(text: String?): Double? =
        text?.trim()?.replace(",", ".")?.toDoubleOrNull()?.takeIf { isValidCalibration(it) }

    /** Rounds to a whole number with thousands separators of [locale] ("30.000" in German, "30,000" in English). */
    fun formatRounded(value: Double, locale: Locale = Locale.GERMANY): String =
        NumberFormat.getIntegerInstance(locale).format(value.roundToLong())

    /** PPFD for display: one decimal for low values ("2,9"), whole numbers above ("1.350"). */
    fun formatPpfd(ppfd: Double, locale: Locale = Locale.GERMANY): String =
        if (ppfd < DECIMAL_THRESHOLD) DecimalFormat("0.0", DecimalFormatSymbols(locale)).format(ppfd)
        else formatRounded(ppfd, locale)
}
