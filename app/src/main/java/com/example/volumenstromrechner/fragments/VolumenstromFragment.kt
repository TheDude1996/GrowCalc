package com.example.volumenstromrechner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.example.volumenstromrechner.R
import com.example.volumenstromrechner.calculation.ClimateCalculator
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class VolumenstromFragment : Fragment() {

    private val durchmesserListeMm = listOf(125, 140, 150, 160, 180, 200, 224, 250, 280, 300)
    private var selectedDurchmesserIndex = 0

    private lateinit var dropdownDurchmesser: MaterialAutoCompleteTextView
    private lateinit var layoutGeschwindigkeit: TextInputLayout
    private lateinit var editGeschwindigkeit: TextInputEditText
    private lateinit var cardErgebnis: MaterialCardView
    private lateinit var textErgebnis: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_volumenstrom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedDurchmesserIndex = savedInstanceState?.getInt(KEY_DURCHMESSER) ?: 0
        setupViews(view)
        setupDropdown()
        setupListeners(view)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_DURCHMESSER, selectedDurchmesserIndex)
    }

    private fun setupViews(view: View) {
        dropdownDurchmesser = view.findViewById(R.id.dropdownDurchmesser)
        layoutGeschwindigkeit = view.findViewById(R.id.layoutGeschwindigkeit)
        editGeschwindigkeit = view.findViewById(R.id.editGeschwindigkeit)
        cardErgebnis = view.findViewById(R.id.cardErgebnis)
        textErgebnis = view.findViewById(R.id.textErgebnis)
    }

    private fun setupDropdown() {
        val durchmesserStrings = durchmesserListeMm.map { "$it mm" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, durchmesserStrings)
        dropdownDurchmesser.setAdapter(adapter)
        dropdownDurchmesser.setText(durchmesserStrings[selectedDurchmesserIndex], false)
        dropdownDurchmesser.setOnItemClickListener { _, _, position, _ ->
            selectedDurchmesserIndex = position
        }
    }

    private fun setupListeners(view: View) {
        view.findViewById<MaterialButton>(R.id.btnBerechnen).setOnClickListener {
            berechne()
        }

        view.findViewById<MaterialButton>(R.id.btnZuruecksetzen).setOnClickListener {
            zuruecksetzen()
        }

        editGeschwindigkeit.doAfterTextChanged { layoutGeschwindigkeit.error = null }
    }

    private fun berechne() {
        val rohtext = editGeschwindigkeit.text?.toString()?.trim().orEmpty()

        if (rohtext.isEmpty()) {
            layoutGeschwindigkeit.error = getString(R.string.fehler_leer)
            cardErgebnis.visibility = View.GONE
            return
        }

        val v = rohtext.replace(",", ".").toDoubleOrNull()

        if (v == null) {
            layoutGeschwindigkeit.error = getString(R.string.fehler_ungueltig)
            cardErgebnis.visibility = View.GONE
            return
        }

        if (v <= 0) {
            layoutGeschwindigkeit.error = getString(R.string.fehler_negativ)
            cardErgebnis.visibility = View.GONE
            return
        }

        val q = ClimateCalculator.volumeFlow(durchmesserListeMm[selectedDurchmesserIndex], v)

        textErgebnis.text = getString(R.string.ergebnis_format, q)
        cardErgebnis.visibility = View.VISIBLE
        editGeschwindigkeit.clearFocus()
    }

    private fun zuruecksetzen() {
        selectedDurchmesserIndex = 0
        dropdownDurchmesser.setText("${durchmesserListeMm[0]} mm", false)
        editGeschwindigkeit.text?.clear()
        layoutGeschwindigkeit.error = null
        cardErgebnis.visibility = View.GONE
        editGeschwindigkeit.clearFocus()
    }

    companion object {
        private const val KEY_DURCHMESSER = "durchmesser_index"
    }
}
