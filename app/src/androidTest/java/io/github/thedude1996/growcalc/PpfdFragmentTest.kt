package io.github.thedude1996.growcalc

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thedude1996.growcalc.fragments.AndroidTestFakeLightSensor
import io.github.thedude1996.growcalc.fragments.PpfdFragment
import io.github.thedude1996.growcalc.calculation.LuxPpfdConverter
import io.github.thedude1996.growcalc.sensor.AndroidLightSensorSource
import io.github.thedude1996.growcalc.settings.AppSettings
import org.hamcrest.Matchers.not
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

    // Thousands separator depends on the device locale
    private fun formatted(value: Int) = LuxPpfdConverter.formatRounded(value.toDouble())

    private fun emit(lux: Float, timeMs: Long) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync { fakeSensor.emit(lux, timeMs) }

    @Test
    fun showsDefaultResult() {
        onView(withId(R.id.editValue)).check(matches(withText("30000")))
        onView(withId(R.id.textErgebnis)).check(matches(withText("540")))
        onView(withId(R.id.textUnit)).check(matches(withText("µmol/m²/s")))
    }

    @Test
    fun enterValueChangeLightSourceAndSwapDirection() {
        onView(withId(R.id.editValue)).perform(replaceText("10000"))
        closeSoftKeyboard()
        onView(withId(R.id.textErgebnis)).check(matches(withText("180")))

        onView(withId(R.id.dropdownLightSource)).perform(click())
        onView(withText("Leuchtstofflampe")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.textErgebnis)).check(matches(withText("130")))

        onView(withId(R.id.btnSwap)).perform(click())
        onView(withId(R.id.textDirection)).check(matches(withText("PPFD → Lux")))
        onView(withId(R.id.editValue)).check(matches(withText("130")))
        onView(withId(R.id.textErgebnis)).check(matches(withText(formatted(10_000))))
        onView(withId(R.id.textUnit)).check(matches(withText("lx")))
        onView(withId(R.id.btnMeasure)).check(matches(not(isEnabled())))
    }

    @Test
    fun invalidInputShowsDash() {
        onView(withId(R.id.editValue)).perform(replaceText(""))
        closeSoftKeyboard()
        onView(withId(R.id.textErgebnis)).check(matches(withText("–")))
    }

    @Test
    fun liveMeasurementWithFakeSensor() {
        onView(withId(R.id.btnMeasure)).perform(click())
        assertTrue(fakeSensor.isRunning)
        onView(withId(R.id.textLive)).check(matches(isDisplayed()))

        emit(20_000f, 0)
        onView(withId(R.id.editValue)).check(matches(withText("20000")))
        onView(withId(R.id.textErgebnis)).check(matches(withText("360")))

        emit(30_000f, 300)
        onView(withId(R.id.editValue)).check(matches(withText("25000")))
        onView(withId(R.id.textErgebnis)).check(matches(withText("450")))

        onView(withId(R.id.btnHold)).perform(click())
        assertFalse(fakeSensor.isRunning)
        onView(withId(R.id.textLive)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
        onView(withId(R.id.editValue)).check(matches(withText("25000")))
        onView(withId(R.id.textErgebnis)).check(matches(withText("450")))
    }

    @Test
    fun swappingStopsMeasurement() {
        onView(withId(R.id.btnMeasure)).perform(click())
        emit(10_000f, 0)
        onView(withId(R.id.btnSwap)).perform(click())
        assertFalse(fakeSensor.isRunning)
        onView(withId(R.id.editValue)).check(matches(withText("180")))
        onView(withId(R.id.textErgebnis)).check(matches(withText(formatted(10_000))))
    }
}
