package io.github.thedude1996.growcalc.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.github.thedude1996.growcalc.calculation.LightSource
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.sensor.LightSensorSource
import io.github.thedude1996.growcalc.sensor.LightSmoother

data class PpfdUiState(
    val lightSource: LightSource = LightSource.SUNLIGHT,
    val sensorAvailable: Boolean = true,
    val isMeasuring: Boolean = false,
    val isSaturated: Boolean = false,
    /** Calibrated, smoothed illuminance; null until the first reading arrives. */
    val lux: Double? = null
) {
    val ppfd: Double? get() = lux?.let { LuxPpfdConverter.luxToPpfd(it, lightSource) }
}

/**
 * State of the PPFD meter. The measurement starts automatically when the screen opens;
 * the sensor itself only runs while the screen is visible and a measurement is active.
 */
class PpfdViewModel(
    private val sensor: LightSensorSource,
    private val calibrationFactor: () -> Double = { LuxPpfdConverter.DEFAULT_CALIBRATION },
    private val smoother: LightSmoother = LightSmoother()
) : ViewModel() {

    private val _state = MutableLiveData(
        PpfdUiState(sensorAvailable = sensor.isAvailable, isMeasuring = sensor.isAvailable)
    )
    val state: LiveData<PpfdUiState> = _state

    private val current: PpfdUiState get() = _state.value!!

    private var screenVisible = false
    private var sensorRunning = false

    fun selectLightSource(source: LightSource) {
        if (source != current.lightSource) _state.value = current.copy(lightSource = source)
    }

    fun nextLightSource() = selectLightSource(current.lightSource.next())

    fun previousLightSource() = selectLightSource(current.lightSource.previous())

    /** Round button: holds the value while measuring, otherwise starts a new measurement. */
    fun toggleMeasurement() {
        if (current.isMeasuring) holdValue() else startMeasurement()
    }

    fun startMeasurement() {
        if (!current.sensorAvailable || current.isMeasuring) return
        _state.value = current.copy(isMeasuring = true, isSaturated = false)
        updateSensorRegistration()
    }

    /** Freezes the current measured value and ends the measurement. */
    fun holdValue() {
        if (!current.isMeasuring) return
        val held = smoother.average()?.let { calibrated(it) } ?: current.lux
        _state.value = current.copy(lux = held, isMeasuring = false, isSaturated = false)
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
        _state.value = current.copy(lux = calibrated(smoothed), isSaturated = smoother.isSaturated())
    }

    private fun calibrated(rawLux: Double): Double =
        LuxPpfdConverter.applyCalibration(rawLux, calibrationFactor())

    override fun onCleared() {
        if (sensorRunning) {
            sensorRunning = false
            sensor.stop()
        }
    }
}
