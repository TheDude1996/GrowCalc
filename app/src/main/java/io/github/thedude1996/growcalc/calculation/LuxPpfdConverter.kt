package io.github.thedude1996.growcalc.calculation

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/** Light source with its approximate lux → PPFD conversion factor (µmol/m²/s per lx). */
enum class LightSource(val factor: Double) {
    SUNLIGHT(0.018),
    LED_FULL_SPECTRUM(0.014),
    HPS(0.014),
    FLUORESCENT(0.013)
}

enum class ConversionDirection {
    LUX_TO_PPFD,
    PPFD_TO_LUX;

    fun swapped(): ConversionDirection = if (this == LUX_TO_PPFD) PPFD_TO_LUX else LUX_TO_PPFD
}

/** UI-independent conversion between illuminance (lux) and PPFD (µmol/m²/s). */
object LuxPpfdConverter {

    const val DEFAULT_CALIBRATION = 1.0
    const val MIN_CALIBRATION = 0.1
    const val MAX_CALIBRATION = 10.0

    /** Lux values shown in the "typical conversions" table. */
    val TYPICAL_LUX_VALUES = listOf(5_000, 10_000, 15_000, 20_000, 30_000, 40_000, 50_000, 75_000, 100_000)

    private val THOUSANDS_GROUPED = Regex("""^\d{1,3}(\.\d{3})+$""")

    fun luxToPpfd(lux: Double, source: LightSource): Double = lux * source.factor

    fun ppfdToLux(ppfd: Double, source: LightSource): Double = ppfd / source.factor

    fun convert(value: Double, direction: ConversionDirection, source: LightSource): Double =
        when (direction) {
            ConversionDirection.LUX_TO_PPFD -> luxToPpfd(value, source)
            ConversionDirection.PPFD_TO_LUX -> ppfdToLux(value, source)
        }

    /** Multiplies a raw sensor reading with the user's calibration factor. */
    fun applyCalibration(rawLux: Double, calibrationFactor: Double): Double = rawLux * calibrationFactor

    fun isValidCalibration(factor: Double?): Boolean =
        factor != null && factor.isFinite() && factor in MIN_CALIBRATION..MAX_CALIBRATION

    /** Parses a calibration factor ("1,15" or "1.15"); null if invalid or out of range. */
    fun parseCalibration(text: String?): Double? =
        text?.trim()?.replace(",", ".")?.toDoubleOrNull()?.takeIf { isValidCalibration(it) }

    /**
     * Parses user input. Accepts comma or dot as decimal separator and German thousands
     * grouping such as "30.000". Returns null for empty, invalid, negative or non-finite input.
     */
    fun parseInput(text: String?): Double? {
        val trimmed = text?.trim()?.replace(" ", "")?.replace(" ", "")
        if (trimmed.isNullOrEmpty()) return null
        val normalized = if (THOUSANDS_GROUPED.matches(trimmed)) {
            trimmed.replace(".", "")
        } else {
            trimmed.replace(",", ".")
        }
        val value = normalized.toDoubleOrNull() ?: return null
        return if (value.isFinite() && value >= 0) value else null
    }

    /** Converts [input] text; null if the input is not a valid value. */
    fun convertInput(input: String?, direction: ConversionDirection, source: LightSource): Double? =
        parseInput(input)?.let { convert(it, direction, source) }

    /** Rounds to a whole number and formats it with German thousands separators (the app is German-only). */
    fun formatRounded(value: Double, locale: Locale = Locale.GERMANY): String =
        NumberFormat.getIntegerInstance(locale).format(value.roundToLong())

    /** Rows of the typical conversions table: lux value to PPFD for each light source. */
    fun typicalTable(): List<Pair<Int, List<Double>>> =
        TYPICAL_LUX_VALUES.map { lux -> lux to LightSource.entries.map { luxToPpfd(lux.toDouble(), it) } }
}
