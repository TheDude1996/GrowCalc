package io.github.thedude1996.growcalc.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
    private val lux get() = state.lux?.roundToLong()
    private val ppfd get() = state.ppfd?.roundToLong()

    @Test
    fun `measurement starts automatically, value appears with the first reading`() {
        assertTrue(state.isMeasuring)
        assertEquals(LightSource.SUNLIGHT, state.lightSource)
        assertNull(state.lux)
        assertNull(state.ppfd)

        viewModel.setScreenVisible(true)
        assertTrue(sensor.isRunning)
        sensor.emit(30_000f, 0)
        assertEquals(30_000L, lux)
        assertEquals(540L, ppfd)
    }

    @Test
    fun `readings are smoothed and throttled`() {
        viewModel.setScreenVisible(true)
        sensor.emit(20_000f, 0)
        assertEquals(360L, ppfd)
        sensor.emit(22_000f, 100) // throttled, not shown yet
        assertEquals(20_000L, lux)
        sensor.emit(24_000f, 250) // average of 20000, 22000, 24000
        assertEquals(22_000L, lux)
        assertEquals(396L, ppfd)
    }

    @Test
    fun `changing the light source recalculates the value`() {
        viewModel.setScreenVisible(true)
        sensor.emit(10_000f, 0)
        assertEquals(180L, ppfd)
        viewModel.selectLightSource(LightSource.FLUORESCENT)
        assertEquals(130L, ppfd)
        viewModel.nextLightSource()
        assertEquals(LightSource.SUNLIGHT, state.lightSource)
        viewModel.previousLightSource()
        assertEquals(LightSource.FLUORESCENT, state.lightSource)
    }

    @Test
    fun `calibration factor is applied to sensor values`() {
        calibration = 1.5
        viewModel.setScreenVisible(true)
        sensor.emit(10_000f, 0)
        assertEquals(15_000L, lux)
        assertEquals(270L, ppfd)
    }

    @Test
    fun `hold freezes the current value and stops the sensor`() {
        viewModel.setScreenVisible(true)
        sensor.emit(10_000f, 0)
        sensor.emit(12_000f, 100) // not yet shown, but part of the average
        viewModel.toggleMeasurement()

        assertFalse(state.isMeasuring)
        assertFalse(sensor.isRunning)
        assertEquals(11_000L, lux)

        sensor.emit(50_000f, 400) // ignored after hold
        assertEquals(11_000L, lux)
    }

    @Test
    fun `measuring again after hold restarts the sensor`() {
        viewModel.setScreenVisible(true)
        sensor.emit(10_000f, 0)
        viewModel.toggleMeasurement()
        viewModel.toggleMeasurement()
        assertTrue(state.isMeasuring)
        assertTrue(sensor.isRunning)
        assertEquals("held value stays until a new reading", 10_000L, lux)
        sensor.emit(20_000f, 1_000)
        assertEquals(20_000L, lux)
    }

    @Test
    fun `sensor only runs while the screen is visible`() {
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
    fun `sensor stays off while a value is held`() {
        viewModel.toggleMeasurement()
        viewModel.setScreenVisible(true)
        assertFalse(sensor.isRunning)
    }

    @Test
    fun `no sensor means no measurement`() {
        val noSensor = FakeLightSensorSource(isAvailable = false)
        val vm = PpfdViewModel(noSensor)
        vm.setScreenVisible(true)
        vm.toggleMeasurement()
        val s = vm.state.value!!
        assertFalse(s.sensorAvailable)
        assertFalse(s.isMeasuring)
        assertFalse(noSensor.isRunning)
        assertNull(s.ppfd)
    }

    @Test
    fun `saturated sensor is flagged and cleared on hold`() {
        viewModel.setScreenVisible(true)
        (0..12).forEach { sensor.emit(40_000f, it * 250L) }
        assertTrue(state.isSaturated)
        viewModel.toggleMeasurement()
        assertFalse(state.isSaturated)
    }
}
