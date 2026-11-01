package io.github.thedude1996.growcalc

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thedude1996.growcalc.fragments.AndroidTestFakeLightSensor
import io.github.thedude1996.growcalc.fragments.PpfdFragment
import io.github.thedude1996.growcalc.sensor.AndroidLightSensorSource
import io.github.thedude1996.growcalc.settings.AppSettings
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PpfdFragmentTest {

    private val fakeSensor = AndroidTestFakeLightSensor()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        PpfdFragment.sensorSourceFactory = { fakeSensor }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.navigateToPpfd() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @After
    fun tearDown() {
        scenario.close()
        PpfdFragment.sensorSourceFactory = { AndroidLightSensorSource(it) }
    }

    private fun emit(lux: Float, timeMs: Long) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync { fakeSensor.emit(lux, timeMs) }

    @Test
    fun measuresLiveOnOpen() {
        assertTrue(fakeSensor.isRunning)
        onView(withId(R.id.textPpfd)).check(matches(withText("–")))
        onView(withId(R.id.textStatus)).check(matches(withText("LIVE")))

        emit(30_000f, 0)
        onView(withId(R.id.textPpfd)).check(matches(withText("540")))
        onView(withId(R.id.textLux)).check(matches(withText("≈ 30.000 lx")))
        onView(withId(R.id.textLightSource)).check(matches(withText("Sonne / Tageslicht")))
    }

    @Test
    fun lowLightShowsOneDecimal() {
        emit(161f, 0)
        onView(withId(R.id.textPpfd)).check(matches(withText("2,9")))
    }

    @Test
    fun changeLightSourceByTapAndSwipe() {
        emit(10_000f, 0)
        onView(withId(R.id.textPpfd)).check(matches(withText("180")))

        onView(withId(R.id.btnLightSource)).perform(click())
        onView(withId(R.id.textLightSource)).check(matches(withText("LED Vollspektrum")))
        onView(withId(R.id.textPpfd)).check(matches(withText("140")))

        onView(withId(R.id.meterPanel)).perform(swipeLeft())
        onView(withId(R.id.textLightSource)).check(matches(withText("Natriumdampf (HPS)")))

        onView(withId(R.id.meterPanel)).perform(swipeRight())
        onView(withId(R.id.meterPanel)).perform(swipeRight())
        onView(withId(R.id.textLightSource)).check(matches(withText("Sonne / Tageslicht")))

        onView(withContentDescription("Lichtquelle Leuchtstofflampe")).perform(click())
        onView(withId(R.id.textPpfd)).check(matches(withText("130")))
    }

    @Test
    fun holdAndMeasureAgain() {
        emit(20_000f, 0)
        emit(30_000f, 300)
        onView(withId(R.id.textPpfd)).check(matches(withText("450")))

        onView(withId(R.id.btnMain)).perform(click())
        assertFalse(fakeSensor.isRunning)
        onView(withId(R.id.textStatus)).check(matches(withText("GEHALTEN")))
        onView(withId(R.id.textPpfd)).check(matches(withText("450")))

        onView(withId(R.id.btnMain)).perform(click())
        assertTrue(fakeSensor.isRunning)
        onView(withId(R.id.textStatus)).check(matches(withText("LIVE")))
    }

    @Test
    fun saturationWarning() {
        onView(withId(R.id.textSaturated)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        (0..12).forEach { emit(40_000f, it * 250L) }
        onView(withId(R.id.textSaturated)).check(matches(isDisplayed()))
    }

    @Test
    fun infoShowsExplanation() {
        onView(withId(R.id.btnInfo)).perform(click())
        onView(withText("So funktioniert die Messung")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun settingsButtonOpensSettings() {
        onView(withId(R.id.btnSettings)).perform(click())
        onView(withId(R.id.editCalibration)).check(matches(isDisplayed()))
        assertFalse("sensor stops when leaving the meter", fakeSensor.isRunning)
    }
}
