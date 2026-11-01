package io.github.thedude1996.growcalc.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import io.github.thedude1996.growcalc.R
import io.github.thedude1996.growcalc.calculation.CaMgMode
import io.github.thedude1996.growcalc.calculation.ComponentKind
import io.github.thedude1996.growcalc.calculation.NutrientCalculator
import io.github.thedude1996.growcalc.model.*
import io.github.thedude1996.growcalc.ui.StatusTone
import io.github.thedude1996.growcalc.ui.showStatus
import io.github.thedude1996.growcalc.viewmodel.NutrientViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class NutrientFragment : Fragment() {

    private val viewModel: NutrientViewModel by viewModels()

    private lateinit var dropdownManufacturer: MaterialAutoCompleteTextView
    private lateinit var layoutLine: TextInputLayout
    private lateinit var dropdownLine: MaterialAutoCompleteTextView
    private lateinit var layoutProgram: TextInputLayout
    private lateinit var dropdownProgram: MaterialAutoCompleteTextView
    private lateinit var layoutWeekSelector: View
    private lateinit var sliderWeek: Slider
    private lateinit var textSelectedWeek: TextView
    private lateinit var layoutRatioSelector: View
    private lateinit var sliderRatio: Slider
    private lateinit var textSelectedRatio: TextView
    private lateinit var chipManufacturerMode: Chip
    private lateinit var textRatioTitle: TextView

    private lateinit var editBaselineEc: TextInputEditText
    private lateinit var layoutVolume: TextInputLayout
    private lateinit var editVolume: TextInputEditText
    private lateinit var editWaterCa: TextInputEditText
    private lateinit var editWaterMg: TextInputEditText

    private lateinit var cardResult: MaterialCardView
    private lateinit var textTotalEc: TextView
    private lateinit var layoutComponents: LinearLayout
    private lateinit var textNotes: TextView

    // Detailed Result UI
    private lateinit var textWaterCaMg: TextView
    private lateinit var textWaterRatio: TextView
    private lateinit var textTotalN: TextView
    private lateinit var textTotalP: TextView
    private lateinit var textTotalK: TextView
    private lateinit var textTotalCa: TextView
    private lateinit var textTotalMg: TextView
    private lateinit var textTotalRatio: TextView
    private lateinit var textRatioAssessment: TextView

    // Water Analysis GHF UI
    private lateinit var layoutWaterAnalysis: View
    private lateinit var textResWaterCa: TextView
    private lateinit var textResWaterMg: TextView
    private lateinit var textResFertMg: TextView
    private lateinit var textResEpsomMg: TextView
    private lateinit var textResTotalMg: TextView
    private lateinit var textResTargetCa: TextView
    private lateinit var textResExtraCa: TextView
    private lateinit var textResFinalCa: TextView
    private lateinit var textWaterNote: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nutrient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        setupObservers()
        setupListeners()
        populateManufacturers()
        restoreSelection()
    }

    private fun setupViews(view: View) {
        dropdownManufacturer = view.findViewById(R.id.dropdownManufacturer)
        layoutLine = view.findViewById(R.id.layoutLine)
        dropdownLine = view.findViewById(R.id.dropdownLine)
        layoutProgram = view.findViewById(R.id.layoutProgram)
        dropdownProgram = view.findViewById(R.id.dropdownProgram)
        layoutWeekSelector = view.findViewById(R.id.layoutWeekSelector)
        sliderWeek = view.findViewById(R.id.sliderWeek)
        textSelectedWeek = view.findViewById(R.id.textSelectedWeek)
        layoutRatioSelector = view.findViewById(R.id.layoutRatioSelector)
        sliderRatio = view.findViewById(R.id.sliderRatio)
        textSelectedRatio = view.findViewById(R.id.textSelectedRatio)
        chipManufacturerMode = view.findViewById(R.id.chipManufacturerMode)
        textRatioTitle = view.findViewById(R.id.textRatioTitle)

        editBaselineEc = view.findViewById(R.id.editBaselineEc)
        layoutVolume = view.findViewById(R.id.layoutVolume)
        editVolume = view.findViewById(R.id.editVolume)
        editWaterCa = view.findViewById(R.id.editWaterCa)
        editWaterMg = view.findViewById(R.id.editWaterMg)

        cardResult = view.findViewById(R.id.cardResult)
        textTotalEc = view.findViewById(R.id.textTotalEc)
        layoutComponents = view.findViewById(R.id.layoutComponents)
        textNotes = view.findViewById(R.id.textNotes)

        // Detailed Water Analysis
        textWaterCaMg = view.findViewById(R.id.textWaterCaMg)
        textWaterRatio = view.findViewById(R.id.textWaterRatio)
        textTotalN = view.findViewById(R.id.textTotalN)
        textTotalP = view.findViewById(R.id.textTotalP)
        textTotalK = view.findViewById(R.id.textTotalK)
        textTotalCa = view.findViewById(R.id.textTotalCa)
        textTotalMg = view.findViewById(R.id.textTotalMg)
        textTotalRatio = view.findViewById(R.id.textTotalRatio)
        textRatioAssessment = view.findViewById(R.id.textRatioAssessment)

        // Water Analysis GHF
        layoutWaterAnalysis = view.findViewById(R.id.layoutWaterAnalysis)
        textResWaterCa = view.findViewById(R.id.textResWaterCa)
        textResWaterMg = view.findViewById(R.id.textResWaterMg)
        textResFertMg = view.findViewById(R.id.textResFertMg)
        textResEpsomMg = view.findViewById(R.id.textResEpsomMg)
        textResTotalMg = view.findViewById(R.id.textResTotalMg)
        textResTargetCa = view.findViewById(R.id.textResTargetCa)
        textResExtraCa = view.findViewById(R.id.textResExtraCa)
        textResFinalCa = view.findViewById(R.id.textResFinalCa)
        textWaterNote = view.findViewById(R.id.textWaterNote)
    }

    private fun setupObservers() {
        viewModel.calculationResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                cardResult.visibility = View.GONE
                return@observe
            }
            cardResult.visibility = View.VISIBLE
            
            // EC
            textTotalEc.text = getString(R.string.ec_format, result.totalEc)
            
            // Components
            layoutComponents.removeAllViews()
            val inflater = LayoutInflater.from(requireContext())
            result.components.forEach { comp ->
                val row = inflater.inflate(R.layout.item_component, layoutComponents, false)
                row.findViewById<TextView>(R.id.componentName).text = comp.name
                row.findViewById<TextView>(R.id.componentAmount).text =
                    getString(R.string.amount_format, comp.amountTotal, comp.unit)
                val dotColor = if (comp.kind == ComponentKind.PRODUCT) R.color.primary_green else R.color.tertiary_earth
                row.findViewById<View>(R.id.componentDot).backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), dotColor))
                layoutComponents.addView(row)
            }

            // Detailed Breakdown
            textWaterCaMg.text = getString(R.string.ca_mg_pair_format, result.analysis.ca.fromWater, result.analysis.mg.fromWater)
            val waterRatio = if (result.analysis.mg.fromWater > 0) result.analysis.ca.fromWater / result.analysis.mg.fromWater else 0.0
            textWaterRatio.text = getString(R.string.ratio_format, waterRatio)

            textTotalN.text = getString(R.string.value_format, result.analysis.n.total)
            textTotalP.text = getString(R.string.value_format, result.analysis.p.total)
            textTotalK.text = getString(R.string.value_format, result.analysis.k.total)
            textTotalCa.text = getString(R.string.value_format, result.analysis.ca.total)
            textTotalMg.text = getString(R.string.value_format, result.analysis.mg.total)
            textTotalRatio.text = getString(R.string.ratio_format, result.analysis.finalRatio)

            val (assessment, tone) = when {
                result.analysis.finalRatio < 2.5 -> R.string.ratio_mg_heavy to StatusTone.WARN
                result.analysis.finalRatio <= 4.0 -> R.string.ratio_balanced to StatusTone.GOOD
                else -> R.string.ratio_ca_heavy to StatusTone.WARN
            }
            textRatioAssessment.showStatus(getString(assessment), tone)

            // Manufacturer Info UI
            if (result.mode == CaMgMode.MANUFACTURER) {
                layoutWaterAnalysis.visibility = View.GONE
                chipManufacturerMode.visibility = View.VISIBLE
                chipManufacturerMode.text = getString(R.string.manufacturer_mode_format, viewModel.selectedManufacturer.value?.name.orEmpty())
            } else {
                chipManufacturerMode.visibility = View.GONE
                layoutWaterAnalysis.visibility = View.VISIBLE
                textResWaterCa.text = getString(R.string.mg_per_liter_format, result.analysis.ca.fromWater)
                textResWaterMg.text = getString(R.string.mg_per_liter_format, result.analysis.mg.fromWater)
                textResFertMg.text = getString(R.string.mg_per_liter_format, result.waterAnalysis.fertMg)
                textResEpsomMg.text = getString(R.string.g_per_liter_format, result.waterAnalysis.epsomMg / NutrientCalculator.EPSOM_MG_PER_GRAM)
                textResTotalMg.text = getString(R.string.mg_per_liter_format, result.waterAnalysis.totalMg)
                textResTargetCa.text = getString(R.string.target_ca_format, result.waterAnalysis.targetCa)
                textResExtraCa.text = getString(R.string.mg_per_liter_format, result.waterAnalysis.extraCaNeeded)
                textResFinalCa.text = getString(R.string.g_per_liter_format, result.waterAnalysis.finalCaDosage)
                textWaterNote.text = getString(R.string.note_ghf_calc_info, result.mode.label)
            }

            textNotes.text = viewModel.selectedProgram.value?.notes
        }
    }

    private fun setupListeners() {
        editWaterCa.doAfterTextChanged { onInputChanged() }
        editWaterMg.doAfterTextChanged { onInputChanged() }
        editBaselineEc.doAfterTextChanged { onInputChanged() }
        editVolume.doAfterTextChanged { onInputChanged() }

        // Slider positions map 1:1 to the CaMgMode order
        val currentMode = viewModel.caMgMode.value ?: CaMgMode.RATIO_3_0
        sliderRatio.value = currentMode.ordinal.toFloat()
        updateRatioText(currentMode)

        sliderRatio.addOnChangeListener { _, value, _ ->
            val mode = CaMgMode.entries.getOrElse(value.toInt()) { CaMgMode.RATIO_3_0 }
            viewModel.caMgMode.value = mode
            updateRatioText(mode)
            viewModel.calculate()
        }

        sliderWeek.addOnChangeListener { _, value, _ ->
            viewModel.selectedWeekIndex.value = value.toInt()
            val weeks = viewModel.selectedProgram.value?.weeks ?: return@addOnChangeListener
            if (value.toInt() in weeks.indices) {
                textSelectedWeek.text = getString(R.string.phase_format, weeks[value.toInt()].label)
            }
            viewModel.calculate()
        }
    }

    private fun populateManufacturers() {
        val manufacturers = NutrientDataProvider.manufacturers
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, manufacturers.map { it.name })
        dropdownManufacturer.setAdapter(adapter)
        dropdownManufacturer.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectManufacturer(manufacturers[position])
            updateLineDropdown()
        }
    }

    // Rebuilds the dropdowns from the ViewModel, e.g. after a configuration change
    private fun restoreSelection() {
        val manufacturer = viewModel.selectedManufacturer.value ?: return
        dropdownManufacturer.setText(manufacturer.name, false)
        updateLineDropdown()

        val line = viewModel.selectedLine.value ?: return
        dropdownLine.setText(line.name, false)
        updateProgramDropdown()

        val program = viewModel.selectedProgram.value ?: return
        dropdownProgram.setText(program.name, false)
        setupWeekSlider(program, viewModel.selectedWeekIndex.value ?: 0)
    }

    private fun updateLineDropdown() {
        val lines = viewModel.selectedManufacturer.value?.lines ?: return
        dropdownLine.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, lines.map { it.name }))
        layoutLine.visibility = View.VISIBLE
        dropdownLine.setText("", false)
        layoutProgram.visibility = View.GONE
        layoutWeekSelector.visibility = View.GONE
        layoutRatioSelector.visibility = View.GONE
        dropdownLine.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectLine(lines[position])
            updateProgramDropdown()
        }
    }

    private fun updateProgramDropdown() {
        val programs = viewModel.selectedLine.value?.programs ?: return
        dropdownProgram.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, programs.map { it.name }))
        layoutProgram.visibility = View.VISIBLE
        dropdownProgram.setText("", false)
        layoutWeekSelector.visibility = View.GONE
        layoutRatioSelector.visibility = View.GONE
        dropdownProgram.setOnItemClickListener { _, _, position, _ ->
            viewModel.selectProgram(programs[position])
            setupWeekSlider(programs[position], 0)
        }
    }

    private fun setupWeekSlider(program: FeedProgram, weekIndex: Int) {
        val index = weekIndex.coerceIn(program.weeks.indices)
        layoutWeekSelector.visibility = View.VISIBLE
        layoutRatioSelector.visibility = View.VISIBLE

        // Programs without a calcium product cannot be corrected to a target ratio
        val ratioVisibility = if (viewModel.supportsRatioMode(program)) View.VISIBLE else View.GONE
        textRatioTitle.visibility = ratioVisibility
        sliderRatio.visibility = ratioVisibility
        textSelectedRatio.visibility = ratioVisibility

        sliderWeek.valueFrom = 0f
        sliderWeek.valueTo = (program.weeks.size - 1).toFloat()
        sliderWeek.value = index.toFloat()
        viewModel.selectedWeekIndex.value = index
        textSelectedWeek.text = getString(R.string.phase_format, program.weeks[index].label)
        viewModel.calculate()
    }

    private fun updateRatioText(mode: CaMgMode) {
        textSelectedRatio.text = if (mode == CaMgMode.MANUFACTURER) getString(R.string.ratio_manufacturer) else getString(R.string.ratio_target_format, mode.label)
    }

    private fun onInputChanged() {
        viewModel.waterCa.value = editWaterCa.parseDouble() ?: 0.0
        viewModel.waterMg.value = editWaterMg.parseDouble() ?: 0.0
        viewModel.waterEc.value = editBaselineEc.parseDouble() ?: 0.0

        val volume = editVolume.parseDouble()
        if (volume == null || volume <= 0.0) {
            layoutVolume.error = getString(R.string.fehler_volumen)
            viewModel.mixingVolume.value = 0.0
        } else {
            layoutVolume.error = null
            viewModel.mixingVolume.value = volume
        }
        viewModel.calculate()
    }

    private fun TextInputEditText.parseDouble(): Double? =
        text?.toString()?.trim()?.replace(",", ".")?.toDoubleOrNull()
}
