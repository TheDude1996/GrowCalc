package io.github.thedude1996.growcalc.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LightSmootherTest {

    private lateinit var smoother: LightSmoother

    @Before
    fun setUp() {
        smoother = LightSmoother()
        smoother.reset(maxRange = 40_000f)
    }

    @Test
    fun `first reading is published immediately`() {
        assertEquals(1_000.0, smoother.add(1_000f, 0)!!, 1e-9)
    }

    @Test
    fun `publishing is throttled to every 250 ms`() {
        assertNotNull(smoother.add(1_000f, 0))
        assertNull(smoother.add(1_000f, 50))
        assertNull(smoother.add(1_000f, 249))
        assertNotNull(smoother.add(1_000f, 250))
        assertNull(smoother.add(1_000f, 400))
        assertNotNull(smoother.add(1_000f, 500))
    }

    @Test
    fun `value is the moving average of the last second`() {
        smoother.add(1_000f, 0)
        smoother.add(2_000f, 100)
        smoother.add(3_000f, 200)
        assertEquals(2_000.0, smoother.add(2_000f, 300)!!, 1e-9)
    }

    @Test
    fun `readings older than one second are dropped`() {
        smoother.add(10_000f, 0)
        smoother.add(10_000f, 500)
        smoother.add(2_000f, 700)
        // At 1000 ms the reading from 0 ms is out of the window
        assertEquals((10_000.0 + 2_000.0 + 2_000.0) / 3, smoother.add(2_000f, 1_000)!!, 1e-9)
        assertEquals(2_000.0, smoother.add(2_000f, 1_600)!!, 1e-9)
    }

    @Test
    fun `sudden spike is smoothed`() {
        (0..9).forEach { smoother.add(1_000f, it * 100L) }
        smoother.add(11_000f, 1_000)
        val value = smoother.average()!!
        assertTrue("spike should be damped, was $value", value < 2_500)
    }

    @Test
    fun `saturation is reported after staying at maximum for three seconds`() {
        var t = 0L
        while (t < 3_000) {
            smoother.add(40_000f, t)
            assertFalse("not yet saturated at $t", smoother.isSaturated())
            t += 250
        }
        smoother.add(40_000f, 3_000)
        assertTrue(smoother.isSaturated())
    }

    @Test
    fun `saturation clears when the value drops`() {
        (0..12).forEach { smoother.add(40_000f, it * 250L) }
        assertTrue(smoother.isSaturated())
        smoother.add(20_000f, 3_250)
        assertFalse(smoother.isSaturated())
    }

    @Test
    fun `short peaks at maximum are not saturation`() {
        smoother.add(40_000f, 0)
        smoother.add(40_000f, 1_000)
        smoother.add(10_000f, 1_500)
        smoother.add(40_000f, 2_000)
        smoother.add(40_000f, 4_000)
        assertFalse(smoother.isSaturated())
    }

    @Test
    fun `unknown max range never reports saturation`() {
        smoother.reset(maxRange = 0f)
        (0..20).forEach { smoother.add(100_000f, it * 250L) }
        assertFalse(smoother.isSaturated())
    }

    @Test
    fun `reset clears readings`() {
        smoother.add(5_000f, 0)
        smoother.reset(40_000f)
        assertNull(smoother.average())
        assertEquals(100.0, smoother.add(100f, 10)!!, 1e-9)
    }
}
