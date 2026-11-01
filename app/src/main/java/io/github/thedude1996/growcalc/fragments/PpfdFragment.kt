package io.github.thedude1996.growcalc.fragments

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.widget.TextViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.ConversionDirection
import io.github.thedude1996.growcalc.calculation.LightSource
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.sensor.AndroidLightSensorSource
import io.github.thedude1996.growcalc.sensor.LightSensorSource
import io.github.thedude1996.growcalc.settings.AppSettings
import io.github.thedude1996.growcalc.ui.StatusTone
import io.github.thedude1996.growcalc.ui.showStatus
import io.github.thedude1996.growcalc.viewmodel.PpfdUiState
import io.github.thedude1996.growcalc.viewmodel.PpfdViewModel

class PpfdFragment : Fragment() {

    private val viewModel: PpfdViewModel by viewModels {
        viewModelFactory {
            initializer {
                val appContext = requireContext().applicationContext
                val settings = AppSettings(appContext)
                PpfdViewModel(sensorSourceFactory(appContext), { settings.lightCalibrationFactor })
            }
        }
    }

    private lateinit var textDirection: TextView
    private lateinit var layoutValue: TextInputLayout
    private lateinit var editValue: TextInputEditText
    private lateinit var dropdownLightSource: MaterialAutoCompleteTextView
    private lateinit var btnMeasure: MaterialButton
    private lateinit var btnHold: MaterialButton
    private lateinit var btnSwap: MaterialButton
    private lateinit var textMeasureNote: TextView
    private lateinit var textMeasureGuide: TextView
    private lateinit var textResultLabel: TextView
    private lateinit var textLive: TextView
    private lateinit var textErgebnis: TextView
    private lateinit var textUnit: TextView
    private lateinit var textSaturated: TextView

    private lateinit var lightSourceLabels: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ppfd, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupListeners()
        buildTypicalTable(view.findViewById(R.id.tableTypical))

        viewModel.state.observe(viewLifecycleOwner, ::render)

        // Register the sensor only while this screen is visible (saves battery)
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) = viewModel.setScreenVisible(true)
            override fun onPause(owner: LifecycleOwner) = viewModel.setScreenVisible(false)
        })
    }

    private fun setupViews(view: View) {
        textDirection = view.findViewById(R.id.textDirection)
        layoutValue = view.findViewById(R.id.layoutValue)
        editValue = view.findViewById(R.id.editValue)
        dropdownLightSource = view.findViewById(R.id.dropdownLightSource)
        btnMeasure = view.findViewById(R.id.btnMeasure)
        btnHold = view.findViewById(R.id.btnHold)
        btnSwap = view.findViewById(R.id.btnSwap)
        textMeasureNote = view.findViewById(R.id.textMeasureNote)
        textMeasureGuide = view.findViewById(R.id.textMeasureGuide)
        textResultLabel = view.findViewById(R.id.textResultLabel)
        textLive = view.findViewById(R.id.textLive)
        textErgebnis = view.findViewById(R.id.textErgebnis)
        textUnit = view.findViewById(R.id.textUnit)
        textSaturated = view.findViewById(R.id.textSaturated)

        lightSourceLabels = LightSource.entries.map { getString(it.labelRes()) }
        dropdownLightSource.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lightSourceLabels)
        )

        textLive.showStatus(getString(R.string.status_live), StatusTone.GOOD)
        textSaturated.showStatus(getString(R.string.warn_sensor_saturated), StatusTone.WARN)
    }

    private fun setupListeners() {
        editValue.doAfterTextChanged { viewModel.onInputChanged(it?.toString().orEmpty()) }
        dropdownLightSource.setOnItemClickListener { _, _, position, _ ->
            viewModel.onLightSourceSelected(LightSource.entries[position])
        }
        btnMeasure.setOnClickListener { viewModel.startMeasurement() }
        btnHold.setOnClickListener { viewModel.holdValue() }
        btnSwap.setOnClickListener {
            editValue.clearFocus()
            viewModel.swapDirection()
        }
    }

    private fun render(state: PpfdUiState) {
        val luxToPpfd = state.direction == ConversionDirection.LUX_TO_PPFD

        textDirection.setText(if (luxToPpfd) R.string.direction_lux_to_ppfd else R.string.direction_ppfd_to_lux)
        layoutValue.hint = getString(if (luxToPpfd) R.string.hint_lux else R.string.hint_ppfd)

        if (editValue.text?.toString() != state.input) {
            editValue.setText(state.input)
            editValue.setSelection(state.input.length)
        }
        // The sensor writes into the field while measuring, so manual typing is paused
        layoutValue.isEnabled = !state.isMeasuring

        val label = lightSourceLabels[state.lightSource.ordinal]
        if (dropdownLightSource.text?.toString() != label) dropdownLightSource.setText(label, false)

        btnMeasure.isEnabled = state.canMeasure
        btnHold.isEnabled = state.isMeasuring

        val note = when {
            !state.sensorAvailable -> getString(R.string.measure_no_sensor)
            !luxToPpfd -> getString(R.string.measure_only_lux)
            else -> null
        }
        textMeasureNote.text = note
        textMeasureNote.visibility = if (note != null) View.VISIBLE else View.GONE
        textMeasureGuide.visibility = if (state.sensorAvailable) View.VISIBLE else View.GONE

        textResultLabel.setText(if (luxToPpfd) R.string.label_result_ppfd else R.string.label_result_lux)
        textUnit.setText(if (luxToPpfd) R.string.unit_ppfd else R.string.unit_lux)
        textErgebnis.text = state.result?.let { LuxPpfdConverter.formatRounded(it) }
            ?: getString(R.string.result_placeholder)
        // INVISIBLE instead of GONE keeps the card from jumping when the measurement starts
        textLive.visibility = if (state.isMeasuring) View.VISIBLE else View.INVISIBLE
        textSaturated.visibility = if (state.isMeasuring && state.isSaturated) View.VISIBLE else View.GONE
    }

    private fun buildTypicalTable(table: TableLayout) {
        val context = requireContext()
        val header = TableRow(context).apply {
            addView(cell(context, getString(R.string.table_header_lux), header = true, alignEnd = false))
            LightSource.entries.forEach { addView(cell(context, getString(it.shortLabelRes()), header = true)) }
        }
        table.addView(header)

        LuxPpfdConverter.typicalTable().forEach { (lux, values) ->
            val row = TableRow(context).apply {
                addView(cell(context, LuxPpfdConverter.formatRounded(lux.toDouble()), header = true, alignEnd = false))
                values.forEach { addView(cell(context, LuxPpfdConverter.formatRounded(it))) }
            }
            table.addView(row)
        }
    }

    private fun cell(context: Context, text: String, header: Boolean = false, alignEnd: Boolean = true): TextView {
        val density = resources.displayMetrics.density
        return TextView(context).apply {
            this.text = text
            TextViewCompat.setTextAppearance(
                this,
                if (header) com.google.android.material.R.style.TextAppearance_Material3_LabelLarge
                else com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            )
            gravity = if (alignEnd) Gravity.END else Gravity.START
            val h = (12 * density).toInt()
            val v = (8 * density).toInt()
            setPadding(h, v, h, v)
        }
    }

    companion object {
        /** Creates the light sensor; replaced by a fake in instrumented tests. */
        @VisibleForTesting
        var sensorSourceFactory: (Context) -> LightSensorSource = { AndroidLightSensorSource(it) }
    }
}

@StringRes
private fun LightSource.labelRes(): Int = when (this) {
    LightSource.SUNLIGHT -> R.string.light_source_sun
    LightSource.LED_FULL_SPECTRUM -> R.string.light_source_led
    LightSource.HPS -> R.string.light_source_hps
    LightSource.FLUORESCENT -> R.string.light_source_fluorescent
}

@StringRes
private fun LightSource.shortLabelRes(): Int = when (this) {
    LightSource.SUNLIGHT -> R.string.light_source_sun_short
    LightSource.LED_FULL_SPECTRUM -> R.string.light_source_led_short
    LightSource.HPS -> R.string.light_source_hps_short
    LightSource.FLUORESCENT -> R.string.light_source_fluorescent_short
}
