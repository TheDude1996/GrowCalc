package io.github.thedude1996.growcalc.sensor

/** Test double for [LightSensorSource]; readings are pushed manually with [emit]. */
class FakeLightSensorSource(
    override val isAvailable: Boolean = true,
    override val maxRange: Float = 40_000f
) : LightSensorSource {

    private var listener: ((Float, Long) -> Unit)? = null
    var startCount = 0
        private set

    val isRunning: Boolean get() = listener != null

    override fun start(listener: (lux: Float, timeMs: Long) -> Unit) {
        startCount++
        this.listener = listener
    }

    override fun stop() {
        listener = null
    }

    fun emit(lux: Float, timeMs: Long) {
        listener?.invoke(lux, timeMs)
    }
}
