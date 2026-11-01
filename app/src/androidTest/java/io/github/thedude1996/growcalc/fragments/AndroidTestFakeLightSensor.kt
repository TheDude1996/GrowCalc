package io.github.thedude1996.growcalc.fragments

import io.github.thedude1996.growcalc.sensor.LightSensorSource

/** Fake light sensor for instrumented tests; readings are pushed with [emit] on the main thread. */
class AndroidTestFakeLightSensor(
    override val isAvailable: Boolean = true,
    override val maxRange: Float = 40_000f
) : LightSensorSource {

    @Volatile
    private var listener: ((Float, Long) -> Unit)? = null

    val isRunning: Boolean get() = listener != null

    override fun start(listener: (lux: Float, timeMs: Long) -> Unit) {
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }

    fun emit(lux: Float, timeMs: Long) {
        listener?.invoke(lux, timeMs)
    }
}
