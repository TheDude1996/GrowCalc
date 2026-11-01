package io.github.thedude1996.growcalc.settings

import android.content.Context
import androidx.core.content.edit
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter

/** App-wide settings stored in SharedPreferences. */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Factor the light sensor reading is multiplied with (default 1.0). */
    var lightCalibrationFactor: Double
        get() {
            if (!prefs.contains(KEY_LIGHT_CALIBRATION)) return LuxPpfdConverter.DEFAULT_CALIBRATION
            val value = Double.fromBits(prefs.getLong(KEY_LIGHT_CALIBRATION, 0L))
            return if (LuxPpfdConverter.isValidCalibration(value)) value else LuxPpfdConverter.DEFAULT_CALIBRATION
        }
        set(value) {
            prefs.edit { putLong(KEY_LIGHT_CALIBRATION, value.toRawBits()) }
        }

    companion object {
        const val PREFS_NAME = "growcalc_settings"
        private const val KEY_LIGHT_CALIBRATION = "light_calibration_factor"
    }
}
