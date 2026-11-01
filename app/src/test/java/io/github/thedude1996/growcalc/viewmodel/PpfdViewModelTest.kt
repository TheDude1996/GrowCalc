package io.github.thedude1996.growcalc.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.github.thedude1996.growcalc.calculation.ConversionDirection
import io.github.thedude1996.growcalc.calculation.LightSource
import io.github.thedude1996.growcalc.sensor.FakeLightSensorSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToLong

class PpfdViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val sensor = FakeLightSensorSource()
    private var calibration = 1.0
    private val viewModel = PpfdViewModel(sensor, { calibration })

    private val state get() = viewModel.state.value!!
    private val result get() = state.result?.roundToLong()

    private fun measuringOnVisibleScreen() {
        viewModel.setScreenVisible(true)
        viewModel.startMeasurement()
    }

    @Test
    fun `defaults are 30000 lux sunlight giving 540`() {
        assertEquals("30000", state.input)
        assertEquals(LightSource.SUNLIGHT, state.lightSource)
        assertEquals(ConversionDirection.LUX_TO_PPFD, state.direction)
        assertEquals(540L, result)
    }

    @Test
    fun `result updates live with input and light source`() {
        viewModel.onInputChanged("10000")
        assertEquals(180L, result)
        viewModel.onLightSourceSelected(LightSource.FLUORESCENT)
        assertEquals(130L, result)
    }

    @Test
    fun `invalid input gives no result`() {
        viewModel.onInputChanged("")
        assertNull(state.result)
        viewModel.onInputChanged("abc")
        assertNull(state.result)
    }

    @Test
    fun `swapping uses the result as new input`() {
        viewModel.swapDirection()
        assertEquals(ConversionDirection.PPFD_TO_LUX, state.direction)
        assertEquals("540", state.input)
        assertEquals(30_000L, result)
        viewModel.swapDirection()
        assertEquals(ConversionDirection.LUX_TO_PPFD, state.direction)
        assertEquals("30000", state.input)
    }

    @Test
    fun `swapping with invalid input keeps the text`() {
        viewModel.onInputChanged("abc")
        viewModel.swapDirection()
        assertEquals("abc", state.input)
        assertNull(state.result)
    }

    @Test
    fun `measurement writes smoothed values into the input`() {
        measuringOnVisibleScreen()
        assertTrue(state.isMeasuring)
        assertTrue(sensor.isRunning)

        sensor.emit(20_000f, 0)
        assertEquals("20000", state.input)
        assertEquals(360L, result)

        sensor.emit(22_000f, 100) // throttled, not shown yet
        assertEquals("20000", state.input)
        sensor.emit(24_000f, 250) // average of 20000, 22000, 24000
        assertEquals("22000", state.input)
        assertEquals(396L, result)
    }

    @Test
    fun `calibration factor is applied to sensor values`() {
        calibration = 1.5
        measuringOnVisibleScreen()
        sensor.emit(10_000f, 0)
        assertEquals("15000", state.input)
        assertEquals(270L, result)
    }

    @Test
    fun `hold freezes the current value and stops the sensor`() {
        measuringOnVisibleScreen()
        sensor.emit(10_000f, 0)
        sensor.emit(12_000f, 100) // not yet shown, but part of the average
        viewModel.holdValue()

        assertFalse(state.isMeasuring)
        assertFalse(sensor.isRunning)
        assertEquals("11000", state.input)

        sensor.emit(50_000f, 400) // ignored after hold
        assertEquals("11000", state.input)
    }

    @Test
    fun `sensor only runs while the screen is visible`() {
        viewModel.startMeasurement()
        assertTrue(state.isMeasuring)
        assertFalse("not visible yet", sensor.isRunning)

        viewModel.setScreenVisible(true)
        assertTrue(sensor.isRunning)

        viewModel.setScreenVisible(false)
        assertFalse(sensor.isRunning)
        assertTrue("measurement resumes after returning", state.isMeasuring)

        viewModel.setScreenVisible(true)
        assertTrue(sensor.isRunning)
        assertEquals(2, sensor.startCount)
    }

    @Test
    fun `sensor is not registered without a measurement`() {
        viewModel.setScreenVisible(true)
        assertFalse(sensor.isRunning)
    }

    @Test
    fun `swapping ends the measurement`() {
        measuringOnVisibleScreen()
        sensor.emit(20_000f, 0)
        viewModel.swapDirection()
        assertFalse(state.isMeasuring)
        assertFalse(sensor.isRunning)
        assertEquals("360", state.input)
    }

    @Test
    fun `measuring is not possible from ppfd to lux`() {
        viewModel.swapDirection()
        assertFalse(state.canMeasure)
        measuringOnVisibleScreen()
        assertFalse(state.isMeasuring)
        assertFalse(sensor.isRunning)
    }

    @Test
    fun `measuring is not possible without a sensor`() {
        val noSensor = FakeLightSensorSource(isAvailable = false)
        val vm = PpfdViewModel(noSensor)
        val s = vm.state.value!!
        assertFalse(s.sensorAvailable)
        assertFalse(s.canMeasure)
        vm.setScreenVisible(true)
        vm.startMeasurement()
        assertFalse(vm.state.value!!.isMeasuring)
        assertFalse(noSensor.isRunning)
        vm.onInputChanged("10000")
        assertEquals(180L, vm.state.value!!.result!!.roundToLong())
    }

    @Test
    fun `saturated sensor is flagged`() {
        measuringOnVisibleScreen()
        (0..12).forEach { sensor.emit(40_000f, it * 250L) }
        assertTrue(state.isSaturated)
        viewModel.holdValue()
        assertFalse(state.isSaturated)
    }
}
