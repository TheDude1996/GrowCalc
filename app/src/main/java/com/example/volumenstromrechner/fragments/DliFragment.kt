package com.example.volumenstromrechner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.volumenstromrechner.R
import com.example.volumenstromrechner.calculation.ClimateCalculator
import com.example.volumenstromrechner.calculation.DliLevel
import com.example.volumenstromrechner.ui.StatusTone
import com.example.volumenstromrechner.ui.showStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class DliFragment : Fragment() {

    private lateinit var layoutPpfd: TextInputLayout
    private lateinit var editPpfd: TextInputEditText
    private lateinit var layoutHours: TextInputLayout
    private lateinit var editHours: TextInputEditText
    private lateinit var chipGroupPresets: ChipGroup
    private lateinit var cardErgebnis: MaterialCardView
    private lateinit var textStatus: TextView
    private lateinit var textErgebnis: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dli, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupListeners(view)
    }

    private fun setupViews(view: View) {
        layoutPpfd = view.findViewById(R.id.layoutPpfd)
        editPpfd = view.findViewById(R.id.editPpfd)
        layoutHours = view.findViewById(R.id.layoutHours)
        editHours = view.findViewById(R.id.editHours)
        chipGroupPresets = view.findViewById(R.id.chipGroupPresets)
        cardErgebnis = view.findViewById(R.id.cardErgebnis)
        textStatus = view.findViewById(R.id.textStatus)
        textErgebnis = view.findViewById(R.id.textErgebnis)
    }

    private fun setupListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.btnBerechnen).setOnClickListener {
            berechneDli()
        }

        view.findViewById<MaterialButton>(R.id.btnZuruecksetzen).setOnClickListener {
            zuruecksetzen()
        }

        listOf(editPpfd, editHours).forEach { edit ->
            edit.doAfterTextChanged {
                layoutPpfd.error = null
                layoutHours.error = null
            }
        }

        chipGroupPresets.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                val hours = chip.text.toString().replace("h", "").toDoubleOrNull()
                if (hours != null) {
                    editHours.setText(hours.toString())
                    layoutHours.error = null
                }
            }
        }
    }

    private fun berechneDli() {
        val ppfdStr = editPpfd.text?.toString()?.trim()?.replace(",", ".")
        val hoursStr = editHours.text?.toString()?.trim()?.replace(",", ".")

        if (ppfdStr.isNullOrEmpty()) {
            layoutPpfd.error = getString(R.string.fehler_leer)
            return
        }
        val ppfd = ppfdStr.toDoubleOrNull()
        if (ppfd == null) {
            layoutPpfd.error = getString(R.string.fehler_ungueltig)
            return
        }
        if (ppfd < 0) {
            layoutPpfd.error = getString(R.string.fehler_ppfd_negativ)
            return
        }

        if (hoursStr.isNullOrEmpty()) {
            layoutHours.error = getString(R.string.fehler_leer)
            return
        }
        val hours = hoursStr.toDoubleOrNull()
        if (hours == null || hours < 0 || hours > 24) {
            layoutHours.error = getString(R.string.fehler_hours_range)
            return
        }

        val dli = ClimateCalculator.dli(ppfd, hours)

        textErgebnis.text = getString(R.string.res_dli_format, dli)

        val (statusText, tone) = when (ClimateCalculator.dliLevel(dli)) {
            DliLevel.LOW -> R.string.dli_low to StatusTone.INFO
            DliLevel.MEDIUM -> R.string.dli_medium to StatusTone.GOOD
            DliLevel.HIGH -> R.string.dli_high to StatusTone.GOOD
            DliLevel.VERY_HIGH -> R.string.dli_very_high to StatusTone.WARN
        }
        textStatus.showStatus(getString(statusText), tone)

        cardErgebnis.visibility = View.VISIBLE
        editPpfd.clearFocus()
        editHours.clearFocus()
    }

    private fun zuruecksetzen() {
        editPpfd.text?.clear()
        editHours.text?.clear()
        layoutPpfd.error = null
        layoutHours.error = null
        chipGroupPresets.clearCheck()
        cardErgebnis.visibility = View.GONE
    }
}
