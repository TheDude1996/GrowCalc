package io.github.thedude1996.growcalc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.thedude1996.growcalc.calculation.DripperPreset
import io.github.thedude1996.growcalc.calculation.DurationUnit
import io.github.thedude1996.growcalc.calculation.IrrigationCalculator
import io.github.thedude1996.growcalc.calculation.IrrigationInput
import io.github.thedude1996.growcalc.calculation.IrrigationResult
import io.github.thedude1996.growcalc.calculation.WateringInput
import io.github.thedude1996.growcalc.settings.IrrigationPreset
import io.github.thedude1996.growcalc.settings.IrrigationStore
import kotlin.math.ceil

data class IrrigationUiState(
    val input: IrrigationInput,
    val result: IrrigationResult,
    val presets: List<IrrigationPreset>,
    /**
     * Bumped whenever the input changes from anything but typing (preset loaded, rows added, unit toggled …),
     * so the screen knows when it has to rewrite its text fields.
     */
    val inputRevision: Int
)

class IrrigationViewModel(private val store: IrrigationStore) : ViewModel() {

    private var input = store.loadLast() ?: IrrigationInput()
    private var presets = store.loadPresets()
    private var revision = 0

    private val _state = MutableLiveData(buildState())
    val state: LiveData<IrrigationUiState> = _state

    // --- Dripper -------------------------------------------------------------------------------

    fun selectPreset(preset: DripperPreset?) = update(external = false) { it.copy(preset = preset) }

    fun setCustomFlow(text: String) = update(external = false) { it.copy(customFlowText = text) }

    fun incrementDrippers() = update(external = false) {
        it.copy(drippers = (it.drippers + 1).coerceAtMost(IrrigationCalculator.MAX_DRIPPERS))
    }

    fun decrementDrippers() = update(external = false) {
        it.copy(drippers = (it.drippers - 1).coerceAtLeast(IrrigationCalculator.MIN_DRIPPERS))
    }

    /** Uses the measured real flow per dripper as custom value. */
    fun applyCalibratedFlow(flowLph: Double) = update(external = true) {
        it.copy(preset = null, customFlowText = IrrigationCalculator.formatNumber(flowLph))
    }

    // --- Waterings -----------------------------------------------------------------------------

    fun setWateringDuration(index: Int, text: String) = updateWatering(index, external = false) {
        it.copy(durationText = text)
    }

    /** Switches between seconds and minutes; the typed number stays (30 → 30 s, not 1800 s). */
    fun toggleWateringUnit(index: Int) = updateWatering(index, external = false) { it.copy(unit = it.unit.other()) }

    /** Sets the time of day and keeps the list sorted by time (rows without time last). */
    fun setWateringTime(index: Int, minutesOfDay: Int) = update(external = true) { current ->
        val list = current.waterings.toMutableList()
        if (index !in list.indices) return@update current
        list[index] = list[index].copy(timeOfDay = minutesOfDay)
        current.copy(waterings = list.sortedWith(compareBy(nullsLast()) { it.timeOfDay }))
    }

    fun addWatering() = update(external = true) {
        if (it.waterings.size >= IrrigationCalculator.MAX_WATERINGS) it
        else it.copy(waterings = it.waterings + WateringInput(unit = it.waterings.last().unit))
    }

    fun removeWatering(index: Int) = update(external = true) {
        if (it.waterings.size <= 1 || index !in it.waterings.indices) it
        else it.copy(waterings = it.waterings.filterIndexed { i, _ -> i != index })
    }

    // --- Pots, tank, reverse mode --------------------------------------------------------------

    fun setPots(text: String) = update(external = false) { it.copy(potsText = text) }

    fun setTank(text: String) = update(external = false) { it.copy(tankText = text) }

    fun setTarget(text: String) = update(external = false) { it.copy(targetText = text) }

    /** Adds the run time from the reverse mode as a new watering (whole seconds, rounded up). */
    fun addWateringFromTarget(): Boolean {
        val seconds = IrrigationCalculator.evaluate(input).targetSeconds ?: return false
        if (input.waterings.size >= IrrigationCalculator.MAX_WATERINGS &&
            input.waterings.none { it.durationText.isBlank() }
        ) return false
        update(external = true) { current ->
            val watering = WateringInput(
                durationText = ceil(seconds).toLong().toString(),
                unit = DurationUnit.SECONDS
            )
            val emptyIndex = current.waterings.indexOfFirst { it.durationText.isBlank() }
            val list = current.waterings.toMutableList()
            if (emptyIndex >= 0) list[emptyIndex] = watering.copy(timeOfDay = list[emptyIndex].timeOfDay)
            else list += watering
            current.copy(waterings = list)
        }
        return true
    }

    // --- Presets -------------------------------------------------------------------------------

    /** Saves the current input under [name]; an existing preset with that name is replaced. */
    fun savePreset(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val preset = IrrigationPreset(trimmed, input.copy(targetText = ""))
        val index = presets.indexOfFirst { it.name.equals(trimmed, ignoreCase = true) }
        presets = if (index >= 0) presets.toMutableList().also { it[index] = preset } else presets + preset
        store.savePresets(presets)
        publish()
        return true
    }

    fun loadPreset(preset: IrrigationPreset) = update(external = true) { preset.input }

    fun deletePreset(preset: IrrigationPreset) {
        presets = presets.filterNot { it.name == preset.name }
        store.savePresets(presets)
        publish()
    }

    fun reset() = update(external = true) { IrrigationInput() }

    // --- Internals -----------------------------------------------------------------------------

    private inline fun updateWatering(index: Int, external: Boolean, crossinline change: (WateringInput) -> WateringInput) =
        update(external) { current ->
            if (index !in current.waterings.indices) current
            else current.copy(waterings = current.waterings.toMutableList().also { it[index] = change(it[index]) })
        }

    private inline fun update(external: Boolean, change: (IrrigationInput) -> IrrigationInput) {
        val newInput = change(input)
        if (newInput == input) return
        input = newInput
        if (external) revision++
        store.saveLast(input)
        publish()
    }

    private fun publish() {
        _state.value = buildState()
    }

    private fun buildState() = IrrigationUiState(input, IrrigationCalculator.evaluate(input), presets, revision)
}
