package io.github.thedude1996.growcalc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.settings.AppSettings
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class SettingsFragment : Fragment() {

    private lateinit var settings: AppSettings
    private lateinit var layoutCalibration: TextInputLayout
    private lateinit var editCalibration: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = AppSettings(requireContext())
        layoutCalibration = view.findViewById(R.id.layoutCalibration)
        editCalibration = view.findViewById(R.id.editCalibration)

        editCalibration.setText(formatFactor(settings.lightCalibrationFactor))

        // Valid values are saved immediately, there is no separate save button
        editCalibration.doAfterTextChanged { text ->
            val factor = LuxPpfdConverter.parseCalibration(text?.toString())
            if (factor != null) {
                layoutCalibration.error = null
                settings.lightCalibrationFactor = factor
            } else {
                layoutCalibration.error = getString(R.string.fehler_calibration_range)
            }
        }

        view.findViewById<MaterialButton>(R.id.btnResetCalibration).setOnClickListener {
            editCalibration.setText(formatFactor(LuxPpfdConverter.DEFAULT_CALIBRATION))
            editCalibration.clearFocus()
        }
    }

    private fun formatFactor(value: Double): String =
        DecimalFormat("0.0##", DecimalFormatSymbols(resources.configuration.locales[0])).format(value)
}
