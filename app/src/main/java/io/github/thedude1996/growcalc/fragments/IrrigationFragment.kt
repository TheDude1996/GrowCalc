package io.github.thedude1996.growcalc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.DripperPreset
import io.github.thedude1996.growcalc.calculation.DurationUnit
import io.github.thedude1996.growcalc.calculation.IrrigationCalculator
import io.github.thedude1996.growcalc.calculation.IrrigationResult
import io.github.thedude1996.growcalc.calculation.WateringInput
import io.github.thedude1996.growcalc.settings.IrrigationPreset
import io.github.thedude1996.growcalc.settings.SharedPrefsIrrigationStore
import io.github.thedude1996.growcalc.viewmodel.IrrigationUiState
import io.github.thedude1996.growcalc.viewmodel.IrrigationViewModel

/** Drip irrigation: amount per pot from dripper flow, dripper count and run times. */
class IrrigationFragment : Fragment() {

    private val viewModel: IrrigationViewModel by viewModels {
        viewModelFactory {
            initializer { IrrigationViewModel(SharedPrefsIrrigationStore(requireContext().applicationContext)) }
        }
    }

    private class WateringRow(
        val time: MaterialButton,
        val layout: TextInputLayout,
        val edit: TextInputEditText,
        val unit: MaterialButton,
        val remove: ImageButton
    )

    private val chipIds = mapOf(
        DripperPreset.PURPLE to R.id.chipPurple,
        DripperPreset.BLUE to R.id.chipBlue,
        DripperPreset.BROWN to R.id.chipBrown,
        DripperPreset.GREEN to R.id.chipGreen,
        DripperPreset.RED to R.id.chipRed
    )

    private lateinit var chipGroupFlow: ChipGroup
    private lateinit var layoutCustomFlow: TextInputLayout
    private lateinit var editCustomFlow: TextInputEditText
    private lateinit var textDrippers: TextView
    private lateinit var btnDrippersMinus: MaterialButton
    private lateinit var btnDrippersPlus: MaterialButton
    private lateinit var wateringsContainer: LinearLayout
    private lateinit var btnAddWatering: MaterialButton
    private lateinit var layoutPots: TextInputLayout
    private lateinit var editPots: TextInputEditText
    private lateinit var layoutTank: TextInputLayout
    private lateinit var editTank: TextInputEditText
    private lateinit var cardResult: MaterialCardView
    private lateinit var textPerPotDay: TextView
    private lateinit var textResultDetails: TextView
    private lateinit var textTotalDay: TextView
    private lateinit var textPots: TextView
    private lateinit var tileTank: View
    private lateinit var textTankDays: TextView
    private lateinit var layoutTarget: TextInputLayout
    private lateinit var editTarget: TextInputEditText
    private lateinit var textReverseResult: TextView
    private lateinit var btnUseAsWatering: MaterialButton
    private lateinit var textPresetsEmpty: TextView
    private lateinit var chipGroupPresets: ChipGroup

    private var rows = emptyList<WateringRow>()
    private var renderedRevision = -1
    private var renderedPresets: List<IrrigationPreset>? = null

    /** True while the screen writes into its own fields, so text watchers don't echo back. */
    private var updatingUi = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_irrigation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupListeners(view)
        renderedRevision = -1
        renderedPresets = null
        viewModel.state.observe(viewLifecycleOwner, ::render)
    }

    private fun setupViews(view: View) {
        chipGroupFlow = view.findViewById(R.id.chipGroupFlow)
        layoutCustomFlow = view.findViewById(R.id.layoutCustomFlow)
        editCustomFlow = view.findViewById(R.id.editCustomFlow)
        textDrippers = view.findViewById(R.id.textDrippers)
        btnDrippersMinus = view.findViewById(R.id.btnDrippersMinus)
        btnDrippersPlus = view.findViewById(R.id.btnDrippersPlus)
        wateringsContainer = view.findViewById(R.id.wateringsContainer)
        btnAddWatering = view.findViewById(R.id.btnAddWatering)
        layoutPots = view.findViewById(R.id.layoutPots)
        editPots = view.findViewById(R.id.editPots)
        layoutTank = view.findViewById(R.id.layoutTank)
        editTank = view.findViewById(R.id.editTank)
        cardResult = view.findViewById(R.id.cardResult)
        textPerPotDay = view.findViewById(R.id.textPerPotDay)
        textResultDetails = view.findViewById(R.id.textResultDetails)
        textTotalDay = view.findViewById(R.id.textTotalDay)
        textPots = view.findViewById(R.id.textPots)
        tileTank = view.findViewById(R.id.tileTank)
        textTankDays = view.findViewById(R.id.textTankDays)
        layoutTarget = view.findViewById(R.id.layoutTarget)
        editTarget = view.findViewById(R.id.editTarget)
        textReverseResult = view.findViewById(R.id.textReverseResult)
        btnUseAsWatering = view.findViewById(R.id.btnUseAsWatering)
        textPresetsEmpty = view.findViewById(R.id.textPresetsEmpty)
        chipGroupPresets = view.findViewById(R.id.chipGroupPresets)
    }

    private fun setupListeners(view: View) {
        chipGroupFlow.setOnCheckedStateChangeListener { _, checkedIds ->
            if (updatingUi) return@setOnCheckedStateChangeListener
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            viewModel.selectPreset(chipIds.entries.firstOrNull { it.value == id }?.key)
            if (id == R.id.chipCustom) editCustomFlow.requestFocus()
        }
        editCustomFlow.onUserText(viewModel::setCustomFlow)
        editPots.onUserText(viewModel::setPots)
        editTank.onUserText(viewModel::setTank)
        editTarget.onUserText(viewModel::setTarget)

        btnDrippersMinus.setOnClickListener { viewModel.decrementDrippers() }
        btnDrippersPlus.setOnClickListener { viewModel.incrementDrippers() }
        btnAddWatering.setOnClickListener {
            viewModel.addWatering()
            rows.lastOrNull()?.edit?.requestFocus()
        }
        btnUseAsWatering.setOnClickListener {
            if (viewModel.addWateringFromTarget()) editTarget.clearFocus()
        }

        view.findViewById<View>(R.id.btnFlowInfo).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.flow_info_title)
                .setMessage(R.string.flow_info_text)
                .setPositiveButton(R.string.btn_ok, null)
                .show()
        }
        view.findViewById<View>(R.id.btnCalibrateFlow).setOnClickListener { showCalibrationDialog() }
        view.findViewById<View>(R.id.btnSavePreset).setOnClickListener { showSavePresetDialog() }
        view.findViewById<View>(R.id.btnReset).setOnClickListener { viewModel.reset() }
    }

    private fun TextInputEditText.onUserText(action: (String) -> Unit) {
        doAfterTextChanged { if (!updatingUi) action(it?.toString().orEmpty()) }
    }

    // --- Rendering -----------------------------------------------------------------------------

    private fun render(state: IrrigationUiState) {
        updatingUi = true
        val input = state.input
        val result = state.result

        if (state.inputRevision != renderedRevision) {
            editCustomFlow.setTextKeepingCursor(input.customFlowText)
            editPots.setTextKeepingCursor(input.potsText)
            editTank.setTextKeepingCursor(input.tankText)
            editTarget.setTextKeepingCursor(input.targetText)
            rebuildRows(input.waterings)
            renderedRevision = state.inputRevision
        }

        val chipId = input.preset?.let { chipIds.getValue(it) } ?: R.id.chipCustom
        if (chipGroupFlow.checkedChipId != chipId) chipGroupFlow.check(chipId)
        layoutCustomFlow.visibility = if (input.preset == null) View.VISIBLE else View.GONE
        layoutCustomFlow.showError(result.flowInvalid)

        textDrippers.text = input.drippers.toString()
        btnDrippersMinus.isEnabled = input.drippers > IrrigationCalculator.MIN_DRIPPERS
        btnDrippersPlus.isEnabled = input.drippers < IrrigationCalculator.MAX_DRIPPERS

        input.waterings.forEachIndexed { i, watering -> renderRow(rows[i], watering, result, i, input.waterings.size) }
        btnAddWatering.isEnabled = input.waterings.size < IrrigationCalculator.MAX_WATERINGS

        layoutPots.showError(result.potsInvalid, R.string.fehler_pots)
        layoutTank.showError(result.tankInvalid)
        layoutTarget.showError(result.targetInvalid)

        renderResult(result)
        renderReverse(result)
        if (state.presets != renderedPresets) renderPresets(state.presets)
        updatingUi = false
    }

    private fun rebuildRows(waterings: List<WateringInput>) {
        wateringsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        rows = waterings.indices.map { index ->
            val view = inflater.inflate(R.layout.item_watering, wateringsContainer, false)
            val row = WateringRow(
                time = view.findViewById(R.id.btnTime),
                layout = view.findViewById(R.id.layoutDuration),
                edit = view.findViewById(R.id.editDuration),
                unit = view.findViewById(R.id.btnUnit),
                remove = view.findViewById(R.id.btnRemove)
            )
            // Rows share view ids, so let the ViewModel restore their text instead of the framework
            row.edit.isSaveEnabled = false
            row.edit.setText(waterings[index].durationText)
            row.edit.onUserText { viewModel.setWateringDuration(index, it) }
            row.unit.setOnClickListener { viewModel.toggleWateringUnit(index) }
            row.remove.setOnClickListener { viewModel.removeWatering(index) }
            row.time.setOnClickListener { showTimePicker(index, viewModel.state.value?.input?.waterings?.getOrNull(index)) }
            wateringsContainer.addView(view)
            row
        }
    }

    private fun renderRow(row: WateringRow, watering: WateringInput, result: IrrigationResult, index: Int, count: Int) {
        val time = watering.timeOfDay
        row.time.text = time?.let { IrrigationCalculator.formatTime(it) } ?: getString(R.string.time_none)
        row.time.contentDescription = time?.let { IrrigationCalculator.formatTime(it) } ?: getString(R.string.btn_set_time)

        val unitLabel = getString(if (watering.unit == DurationUnit.SECONDS) R.string.unit_seconds_short else R.string.unit_minutes_short)
        row.unit.text = unitLabel
        row.unit.contentDescription = getString(R.string.btn_toggle_unit, unitLabel)
        row.remove.visibility = if (count > 1) View.VISIBLE else View.INVISIBLE

        if (result.wateringInvalid[index]) {
            row.layout.showError(true)
        } else {
            row.layout.showError(false)
            val ml = result.wateringMl[index]
            val helper = ml?.let { getString(R.string.watering_amount_format, IrrigationCalculator.formatVolume(it)) }
            if (row.layout.helperText?.toString() != helper) row.layout.helperText = helper
        }
    }

    private fun renderResult(result: IrrigationResult) {
        val perPotDay = result.perPotDayMl
        cardResult.visibility = if (perPotDay != null) View.VISIBLE else View.GONE
        if (perPotDay == null) return

        textPerPotDay.text = IrrigationCalculator.formatVolume(perPotDay)
        val flow = IrrigationCalculator.formatNumber(result.flowLph ?: 0.0)
        val drippers = viewModel.state.value?.input?.drippers ?: 1
        val count = result.wateringMl.count { it != null }
        textResultDetails.text = if (count == 1) {
            getString(R.string.watering_single_details_format, drippers, flow)
        } else {
            getString(R.string.waterings_details_format, count, drippers, flow)
        }

        textTotalDay.text = result.totalDayMl?.let { IrrigationCalculator.formatVolume(it) } ?: getString(R.string.result_placeholder)
        textPots.text = if (result.pots == 1) getString(R.string.pot_single) else getString(R.string.pots_format, result.pots)

        val tankDays = result.tankDays
        tileTank.visibility = if (tankDays != null) View.VISIBLE else View.GONE
        textTankDays.text = tankDays?.let { IrrigationCalculator.formatTankDuration(it) }
    }

    private fun renderReverse(result: IrrigationResult) {
        val seconds = result.targetSeconds
        val visibility = if (seconds != null) View.VISIBLE else View.GONE
        textReverseResult.visibility = visibility
        btnUseAsWatering.visibility = visibility
        if (seconds != null) {
            textReverseResult.text = getString(R.string.reverse_result_format, IrrigationCalculator.formatDuration(seconds))
        }
    }

    private fun renderPresets(presets: List<IrrigationPreset>) {
        renderedPresets = presets
        textPresetsEmpty.visibility = if (presets.isEmpty()) View.VISIBLE else View.GONE
        chipGroupPresets.visibility = if (presets.isEmpty()) View.GONE else View.VISIBLE
        chipGroupPresets.removeAllViews()
        presets.forEach { preset ->
            val chip = Chip(requireContext()).apply {
                text = preset.name
                isCloseIconVisible = true
                closeIconContentDescription = getString(R.string.btn_delete)
                setOnClickListener {
                    viewModel.loadPreset(preset)
                    Toast.makeText(requireContext(), getString(R.string.preset_loaded, preset.name), Toast.LENGTH_SHORT).show()
                }
                setOnCloseIconClickListener { confirmDeletePreset(preset) }
            }
            chipGroupPresets.addView(chip)
        }
    }

    // --- Dialogs -------------------------------------------------------------------------------

    private fun showTimePicker(index: Int, watering: WateringInput?) {
        val current = watering?.timeOfDay ?: DEFAULT_TIME
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(current / 60)
            .setMinute(current % 60)
            .setTitleText(R.string.btn_set_time)
            .build()
        picker.addOnPositiveButtonClickListener { viewModel.setWateringTime(index, picker.hour * 60 + picker.minute) }
        picker.show(childFragmentManager, TAG_TIME_PICKER)
    }

    private fun showCalibrationDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_flow_calibration, null)
        val editMl = content.findViewById<TextInputEditText>(R.id.editMeasuredMl)
        val editSeconds = content.findViewById<TextInputEditText>(R.id.editMeasureSeconds)
        val editDrippers = content.findViewById<TextInputEditText>(R.id.editMeasureDrippers)
        val textResult = content.findViewById<TextView>(R.id.textCalibrationResult)
        editSeconds.setText(DEFAULT_MEASURE_SECONDS.toString())
        editDrippers.setText("1")

        fun measuredFlow(): Double? {
            val ml = IrrigationCalculator.parsePositive(editMl.text?.toString()) ?: return null
            val seconds = IrrigationCalculator.parsePositive(editSeconds.text?.toString()) ?: return null
            val drippers = IrrigationCalculator.parsePositiveInt(editDrippers.text?.toString(), MAX_CUP_DRIPPERS) ?: return null
            return IrrigationCalculator.flowFromMeasurement(ml, seconds, drippers)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.calibration_dialog_title)
            .setView(content)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_apply) { _, _ -> measuredFlow()?.let(viewModel::applyCalibratedFlow) }
            .show()

        fun update() {
            val flow = measuredFlow()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = flow != null
            textResult.visibility = if (flow != null) View.VISIBLE else View.INVISIBLE
            if (flow != null) {
                textResult.text = getString(R.string.calibration_result_format, IrrigationCalculator.formatNumber(flow))
            }
        }
        listOf(editMl, editSeconds, editDrippers).forEach { it.doAfterTextChanged { update() } }
        update()
        editMl.requestFocus()
    }

    private fun showSavePresetDialog() {
        val content = layoutInflater.inflate(R.layout.dialog_preset_name, null)
        val editName = content.findViewById<TextInputEditText>(R.id.editPresetName)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.preset_name_title)
            .setView(content)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_save) { _, _ -> viewModel.savePreset(editName.text?.toString().orEmpty()) }
            .show()
        val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        saveButton.isEnabled = false
        editName.doAfterTextChanged { saveButton.isEnabled = !it.isNullOrBlank() }
        editName.requestFocus()
    }

    private fun confirmDeletePreset(preset: IrrigationPreset) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.preset_delete_title)
            .setMessage(getString(R.string.preset_delete_message, preset.name))
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ -> viewModel.deletePreset(preset) }
            .show()
    }

    // --- Helpers -------------------------------------------------------------------------------

    private fun TextInputEditText.setTextKeepingCursor(value: String) {
        if (text?.toString() == value) return
        setText(value)
        setSelection(value.length)
    }

    private fun TextInputLayout.showError(invalid: Boolean, message: Int = R.string.fehler_positive) {
        val text = if (invalid) getString(message) else null
        if (error?.toString() != text) error = text
    }

    private companion object {
        const val TAG_TIME_PICKER = "watering_time"
        const val DEFAULT_TIME = 8 * 60
        const val MAX_CUP_DRIPPERS = 99
        const val DEFAULT_MEASURE_SECONDS = 60
    }
}
