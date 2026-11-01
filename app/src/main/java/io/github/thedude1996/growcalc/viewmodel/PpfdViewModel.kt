package io.github.thedude1996.growcalc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.thedude1996.growcalc.calculation.ConversionDirection
import io.github.thedude1996.growcalc.calculation.LightSource
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.sensor.LightSensorSource
import io.github.thedude1996.growcalc.sensor.LightSmoother
import kotlin.math.roundToLong

data class PpfdUiState(
    val input: String = DEFAULT_INPUT,
    val direction: ConversionDirection = ConversionDirection.LUX_TO_PPFD,
    val lightSource: LightSource = LightSource.SUNLIGHT,
    val sensorAvailable: Boolean = true,
    val isMeasuring: Boolean = false,
    val isSaturated: Boolean = false
) {
    /** Converted value, or null if the input is empty or invalid. */
    val result: Double? get() = LuxPpfdConverter.convertInput(input, direction, lightSource)

    val canMeasure: Boolean
        get() = sensorAvailable && direction == ConversionDirection.LUX_TO_PPFD && !isMeasuring

    companion object {
        const val DEFAULT_INPUT = "30000"
    }
}

class PpfdViewModel(
    private val sensor: LightSensorSource,
    private val calibrationFactor: () -> Double = { LuxPpfdConverter.DEFAULT_CALIBRATION },
    private val smoother: LightSmoother = LightSmoother()
) : ViewModel() {

    private val _state = MutableLiveData(PpfdUiState(sensorAvailable = sensor.isAvailable))
    val state: LiveData<PpfdUiState> = _state

    private val current: PpfdUiState get() = _state.value!!

    private var screenVisible = false
    private var sensorRunning = false

    fun onInputChanged(text: String) {
        if (text != current.input) _state.value = current.copy(input = text)
    }

    fun onLightSourceSelected(source: LightSource) {
        if (source != current.lightSource) _state.value = current.copy(lightSource = source)
    }

    /** Swaps lux ↔ PPFD; the previous result becomes the new input. Ends a running measurement. */
    fun swapDirection() {
        val state = current
        val newInput = state.result?.roundToLong()?.toString() ?: state.input
        _state.value = state.copy(
            direction = state.direction.swapped(),
            input = newInput,
            isMeasuring = false,
            isSaturated = false
        )
        updateSensorRegistration()
    }

    fun startMeasurement() {
        if (!current.canMeasure) return
        _state.value = current.copy(isMeasuring = true, isSaturated = false)
        updateSensorRegistration()
    }

    /** Freezes the current measured value and ends the measurement. */
    fun holdValue() {
        if (!current.isMeasuring) return
        val held = smoother.average()?.let { calibratedText(it) } ?: current.input
        _state.value = current.copy(input = held, isMeasuring = false, isSaturated = false)
        updateSensorRegistration()
    }

    /** Called from the screen's lifecycle (resume/pause); the sensor only runs while visible. */
    fun setScreenVisible(visible: Boolean) {
        screenVisible = visible
        updateSensorRegistration()
    }

    private fun updateSensorRegistration() {
        val shouldRun = screenVisible && current.isMeasuring
        if (shouldRun && !sensorRunning) {
            smoother.reset(sensor.maxRange)
            sensorRunning = true
            sensor.start(::onSensorValue)
        } else if (!shouldRun && sensorRunning) {
            sensorRunning = false
            sensor.stop()
        }
    }

    private fun onSensorValue(lux: Float, timeMs: Long) {
        if (!sensorRunning || !current.isMeasuring) return
        val smoothed = smoother.add(lux, timeMs) ?: return
        _state.value = current.copy(input = calibratedText(smoothed), isSaturated = smoother.isSaturated())
    }

    private fun calibratedText(rawLux: Double): String =
        LuxPpfdConverter.applyCalibration(rawLux, calibrationFactor()).roundToLong().toString()

    override fun onCleared() {
        if (sensorRunning) {
            sensorRunning = false
            sensor.stop()
        }
    }
}
