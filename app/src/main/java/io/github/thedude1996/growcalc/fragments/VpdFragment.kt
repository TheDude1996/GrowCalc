package io.github.thedude1996.growcalc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.ClimateCalculator
import io.github.thedude1996.growcalc.calculation.VpdStatus
import io.github.thedude1996.growcalc.ui.StatusTone
import io.github.thedude1996.growcalc.ui.showStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VpdFragment : Fragment() {

    private lateinit var layoutTempAir: TextInputLayout
    private lateinit var editTempAir: TextInputEditText
    private lateinit var layoutHumidity: TextInputLayout
    private lateinit var editHumidity: TextInputEditText
    private lateinit var layoutLeafOffset: TextInputLayout
    private lateinit var editLeafOffset: TextInputEditText
    private lateinit var cardErgebnis: MaterialCardView
    private lateinit var textStatus: TextView
    private lateinit var resTempLeaf: TextView
    private lateinit var resVpdAir: TextView
    private lateinit var resVpdLeaf: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_vpd, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupListeners(view)
    }

    private fun setupViews(view: View) {
        layoutTempAir = view.findViewById(R.id.layoutTempAir)
        editTempAir = view.findViewById(R.id.editTempAir)
        layoutHumidity = view.findViewById(R.id.layoutHumidity)
        editHumidity = view.findViewById(R.id.editHumidity)
        layoutLeafOffset = view.findViewById(R.id.layoutLeafOffset)
        editLeafOffset = view.findViewById(R.id.editLeafOffset)
        cardErgebnis = view.findViewById(R.id.cardErgebnis)
        textStatus = view.findViewById(R.id.textStatus)
        resTempLeaf = view.findViewById(R.id.resTempLeaf)
        resVpdAir = view.findViewById(R.id.resVpdAir)
        resVpdLeaf = view.findViewById(R.id.resVpdLeaf)
    }

    private fun setupListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.btnBerechnen).setOnClickListener {
            berechneVpd()
        }

        view.findViewById<MaterialButton>(R.id.btnZuruecksetzen).setOnClickListener {
            zuruecksetzen()
        }

        listOf(editTempAir, editHumidity, editLeafOffset).forEach { edit ->
            edit.doAfterTextChanged {
                layoutTempAir.error = null
                layoutHumidity.error = null
                layoutLeafOffset.error = null
            }
        }
    }

    private fun berechneVpd() {
        val tAirStr = editTempAir.text?.toString()?.trim()?.replace(",", ".")
        val rhStr = editHumidity.text?.toString()?.trim()?.replace(",", ".")
        val offsetStr = editLeafOffset.text?.toString()?.trim()?.replace(",", ".")

        if (tAirStr.isNullOrEmpty()) {
            layoutTempAir.error = getString(R.string.fehler_leer)
            return
        }
        val tAir = tAirStr.toDoubleOrNull()
        if (tAir == null) {
            layoutTempAir.error = getString(R.string.fehler_ungueltig)
            return
        }
        if (tAir !in TEMP_RANGE) {
            layoutTempAir.error = getString(R.string.fehler_temp_range)
            return
        }

        if (rhStr.isNullOrEmpty()) {
            layoutHumidity.error = getString(R.string.fehler_leer)
            return
        }
        val rh = rhStr.toDoubleOrNull()
        if (rh == null || rh < 0 || rh > 100) {
            layoutHumidity.error = getString(R.string.fehler_humidity)
            return
        }

        val offset = if (offsetStr.isNullOrEmpty()) 0.0 else offsetStr.toDoubleOrNull()
        if (offset == null) {
            layoutLeafOffset.error = getString(R.string.fehler_ungueltig)
            return
        }
        if (tAir + offset !in TEMP_RANGE) {
            layoutLeafOffset.error = getString(R.string.fehler_temp_range)
            return
        }

        val result = ClimateCalculator.vpd(tAir, rh, offset)

        // Ergebnisse anzeigen
        resTempLeaf.text = getString(R.string.temp_format, result.leafTemp)
        resVpdAir.text = getString(R.string.kpa_format, result.vpdAir)
        resVpdLeaf.text = getString(R.string.kpa_format, result.vpdLeaf)

        val (statusText, tone) = when (result.status) {
            VpdStatus.LOW -> R.string.status_low to StatusTone.INFO
            VpdStatus.IDEAL -> R.string.status_ideal to StatusTone.GOOD
            VpdStatus.HIGH -> R.string.status_high to StatusTone.WARN
            VpdStatus.DANGER -> R.string.status_danger to StatusTone.BAD
        }
        textStatus.showStatus(getString(statusText), tone)

        cardErgebnis.visibility = View.VISIBLE
        editTempAir.clearFocus()
        editHumidity.clearFocus()
        editLeafOffset.clearFocus()
    }

    private fun zuruecksetzen() {
        editTempAir.text?.clear()
        editHumidity.text?.clear()
        editLeafOffset.text?.clear()
        layoutTempAir.error = null
        layoutHumidity.error = null
        layoutLeafOffset.error = null
        cardErgebnis.visibility = View.GONE
    }

    companion object {
        private val TEMP_RANGE = -50.0..60.0
    }
}
