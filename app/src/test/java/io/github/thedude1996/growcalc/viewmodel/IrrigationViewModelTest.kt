package io.github.thedude1996.growcalc.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.thedude1996.growcalc.calculation.DripperPreset
import io.github.thedude1996.growcalc.calculation.DurationUnit
import io.github.thedude1996.growcalc.calculation.IrrigationInput
import io.github.thedude1996.growcalc.calculation.WateringInput
import io.github.thedude1996.growcalc.settings.IrrigationPreset
import io.github.thedude1996.growcalc.settings.IrrigationStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IrrigationViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private class FakeStore(var last: IrrigationInput? = null) : IrrigationStore {
        var presets = emptyList<IrrigationPreset>()
        override fun loadLast() = last
        override fun saveLast(input: IrrigationInput) { last = input }
        override fun loadPresets() = presets
        override fun savePresets(presets: List<IrrigationPreset>) { this.presets = presets }
    }

    private val store = FakeStore()
    private val viewModel = IrrigationViewModel(store)
    private val state get() = viewModel.state.value!!

    @Test
    fun `live result while typing`() {
        assertNull(state.result.perPotDayMl)
        viewModel.incrementDrippers()
        viewModel.setWateringDuration(0, "3")
        assertEquals(220.0, state.result.perPotDayMl!!, 1e-9)
        assertEquals("typing does not rewrite the fields", 0, state.inputRevision)
    }

    @Test
    fun `drippers stay between 1 and 10`() {
        viewModel.decrementDrippers()
        assertEquals(1, state.input.drippers)
        repeat(15) { viewModel.incrementDrippers() }
        assertEquals(10, state.input.drippers)
    }

    @Test
    fun `toggling the unit keeps the typed number`() {
        viewModel.setWateringDuration(0, "90")
        assertEquals(3_300.0, state.result.perPotDayMl!!, 1e-9)
        viewModel.toggleWateringUnit(0)
        val watering = state.input.waterings.single()
        assertEquals(DurationUnit.SECONDS, watering.unit)
        assertEquals("90", watering.durationText)
        assertEquals(55.0, state.result.perPotDayMl!!, 1e-9)
        assertEquals("field text is untouched", 0, state.inputRevision)
    }

    @Test
    fun `waterings are added, sorted by time and removed`() {
        viewModel.setWateringDuration(0, "3")
        viewModel.addWatering()
        viewModel.setWateringDuration(1, "2")
        viewModel.setWateringTime(0, 14 * 60)
        viewModel.setWateringTime(1, 8 * 60)
        assertEquals(listOf("2", "3"), state.input.waterings.map { it.durationText })
        assertEquals(183.333, state.result.perPotDayMl!!, 1e-3)

        viewModel.removeWatering(0)
        assertEquals(listOf("3"), state.input.waterings.map { it.durationText })
        viewModel.removeWatering(0)
        assertEquals("the last row stays", 1, state.input.waterings.size)
    }

    @Test
    fun `reverse mode fills the empty row with the run time`() {
        viewModel.incrementDrippers()
        viewModel.setTarget("220")
        assertEquals(180.0, state.result.targetSeconds!!, 1e-9)
        assertTrue(viewModel.addWateringFromTarget())
        assertEquals(listOf(WateringInput("180", DurationUnit.SECONDS)), state.input.waterings)
        assertEquals(220.0, state.result.perPotDayMl!!, 1e-9)
    }

    @Test
    fun `calibrated flow becomes the custom value`() {
        viewModel.applyCalibratedFlow(2.4)
        assertNull(state.input.preset)
        assertEquals("2,4", state.input.customFlowText)
        assertEquals(2.4, state.result.flowLph!!, 1e-9)
    }

    @Test
    fun `presets are saved, replaced, loaded and deleted`() {
        viewModel.selectPreset(DripperPreset.GREEN)
        viewModel.setPots("4")
        assertTrue(viewModel.savePreset(" Zelt 1 "))
        assertFalse(viewModel.savePreset("  "))
        viewModel.setPots("6")
        viewModel.savePreset("zelt 1")
        assertEquals(1, store.presets.size)
        assertEquals("6", store.presets.single().input.potsText)

        viewModel.reset()
        assertEquals(DripperPreset.BLUE, state.input.preset)
        viewModel.loadPreset(state.presets.single())
        assertEquals(DripperPreset.GREEN, state.input.preset)
        assertEquals("6", state.input.potsText)

        viewModel.deletePreset(state.presets.single())
        assertTrue(state.presets.isEmpty())
    }

    @Test
    fun `last input is persisted and restored`() {
        viewModel.selectPreset(DripperPreset.RED)
        viewModel.setWateringDuration(0, "45")
        val restored = IrrigationViewModel(store)
        assertEquals(DripperPreset.RED, restored.state.value!!.input.preset)
        assertEquals("45", restored.state.value!!.input.waterings.single().durationText)
    }
}
