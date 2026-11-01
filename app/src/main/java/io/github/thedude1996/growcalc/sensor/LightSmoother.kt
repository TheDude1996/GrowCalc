package io.github.thedude1996.growcalc.sensor

/**
 * Smooths raw light sensor readings with a moving average over [windowMs], limits how often a
 * new value is published ([emitIntervalMs]) and detects a saturated (clipping) sensor.
 * Pure Kotlin, timestamps are passed in so it can be tested without a device.
 */
class LightSmoother(
    private val windowMs: Long = 1_000,
    private val emitIntervalMs: Long = 250,
    private val saturationHoldMs: Long = 3_000,
    private val saturationRatio: Double = 0.98
) {
    private val samples = ArrayDeque<Pair<Long, Float>>()
    private var lastEmitMs: Long? = null
    private var saturatedSinceMs: Long? = null
    private var maxRange: Float = 0f
    private var latestMs: Long = 0

    /** Clears all state. [maxRange] is the sensor's maximum reading (0 = unknown). */
    fun reset(maxRange: Float) {
        samples.clear()
        lastEmitMs = null
        saturatedSinceMs = null
        latestMs = 0
        this.maxRange = maxRange
    }

    /**
     * Adds a reading. Returns the smoothed value if a new value should be shown now,
     * otherwise null (throttled).
     */
    fun add(lux: Float, timeMs: Long): Double? {
        samples.addLast(timeMs to lux)
        latestMs = timeMs
        while (samples.isNotEmpty() && samples.first().first <= timeMs - windowMs) {
            samples.removeFirst()
        }

        if (maxRange > 0f && lux >= maxRange * saturationRatio) {
            if (saturatedSinceMs == null) saturatedSinceMs = timeMs
        } else {
            saturatedSinceMs = null
        }

        val last = lastEmitMs
        if (last != null && timeMs - last < emitIntervalMs) return null
        lastEmitMs = timeMs
        return average()
    }

    /** Current moving average, or null if no readings were added yet. */
    fun average(): Double? =
        if (samples.isEmpty()) null else samples.sumOf { it.second.toDouble() } / samples.size

    /** True if the readings have stayed at the sensor maximum for at least [saturationHoldMs]. */
    fun isSaturated(): Boolean {
        val since = saturatedSinceMs ?: return false
        return latestMs - since >= saturationHoldMs
    }
}
