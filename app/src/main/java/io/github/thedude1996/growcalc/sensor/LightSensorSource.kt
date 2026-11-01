package io.github.thedude1996.growcalc.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Source of ambient light readings in lux. Replaceable by a fake in tests. */
interface LightSensorSource {
    /** Whether the device has a light sensor. */
    val isAvailable: Boolean

    /** Maximum value the sensor can report in lux (0 if unknown). */
    val maxRange: Float

    /** Starts delivering readings (lux, timestamp in ms) on the main thread. */
    fun start(listener: (lux: Float, timeMs: Long) -> Unit)

    fun stop()
}

/**
 * [LightSensorSource] backed by [Sensor.TYPE_LIGHT]. The light sensor only reports changes,
 * so the last value is repeated periodically to keep smoothing and saturation detection going.
 */
class AndroidLightSensorSource(context: Context) : LightSensorSource {

    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val handler = Handler(Looper.getMainLooper())

    private var listener: ((Float, Long) -> Unit)? = null
    private var lastValue: Float? = null

    override val isAvailable: Boolean get() = sensor != null
    override val maxRange: Float get() = sensor?.maximumRange ?: 0f

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values.firstOrNull() ?: return
            lastValue = value
            listener?.invoke(value, SystemClock.elapsedRealtime())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val repeater = object : Runnable {
        override fun run() {
            val current = listener ?: return
            lastValue?.let { current(it, SystemClock.elapsedRealtime()) }
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    override fun start(listener: (lux: Float, timeMs: Long) -> Unit) {
        val lightSensor = sensor ?: return
        if (this.listener != null) return
        this.listener = listener
        lastValue = null
        sensorManager?.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        handler.postDelayed(repeater, REPEAT_INTERVAL_MS)
    }

    override fun stop() {
        listener = null
        sensorManager?.unregisterListener(sensorListener)
        handler.removeCallbacks(repeater)
    }

    private companion object {
        const val REPEAT_INTERVAL_MS = 250L
    }
}
