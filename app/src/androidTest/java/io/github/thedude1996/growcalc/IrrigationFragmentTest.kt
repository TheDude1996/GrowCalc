package io.github.thedude1996.growcalc

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.thedude1996.growcalc.settings.AppSettings
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IrrigationFragmentTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(AppSettings.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.navigateToIrrigation() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @After
    fun tearDown() = scenario.close()

    /** The run-time field of the [index]-th watering row. */
    private fun durationField(index: Int): Matcher<View> = allOf(
        withId(R.id.editDuration),
        isDescendantOfA(nthChild(index))
    )

    private fun nthChild(index: Int): Matcher<View> = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("watering row $index")
        }

        override fun matchesSafely(view: View): Boolean {
            val parent = view.parent as? ViewGroup ?: return false
            return parent.id == R.id.wateringsContainer && parent.indexOfChild(view) == index
        }
    }

    @Test
    fun liveResultForTwoDrippersAndThreeMinutes() {
        onView(withId(R.id.cardResult)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withId(R.id.btnDrippersPlus)).perform(click())
        onView(durationField(0)).perform(replaceText("3"), closeSoftKeyboard())
        onView(withId(R.id.textPerPotDay)).perform(scrollTo()).check(matches(withText("220 ml")))
    }

    @Test
    fun severalWateringsPotsAndTank() {
        onView(durationField(0)).perform(replaceText("3"), closeSoftKeyboard())
        onView(withId(R.id.btnAddWatering)).perform(click())
        onView(durationField(1)).perform(replaceText("90"), closeSoftKeyboard())
        onView(allOf(withId(R.id.btnUnit), isDescendantOfA(nthChild(1)))).perform(click())
        onView(withId(R.id.editPots)).perform(scrollTo(), replaceText("4"), closeSoftKeyboard())
        onView(withId(R.id.editTank)).perform(scrollTo(), replaceText("10"), closeSoftKeyboard())

        // 110 ml + 55 ml per pot, 660 ml for 4 pots, 10 L tank
        onView(withId(R.id.textPerPotDay)).perform(scrollTo()).check(matches(withText("165 ml")))
        onView(withId(R.id.textTotalDay)).check(matches(withText("660 ml")))
        onView(withId(R.id.textTankDays)).check(matches(withText("15,2 Tage")))
    }

    @Test
    fun reverseModeAddsTheRunTime() {
        onView(withId(R.id.btnDrippersPlus)).perform(click())
        onView(withId(R.id.editTarget)).perform(scrollTo(), replaceText("220"), closeSoftKeyboard())
        onView(withId(R.id.textReverseResult)).check(matches(withText("Laufzeit: 3:00 min")))
        onView(withId(R.id.btnUseAsWatering)).perform(scrollTo(), click())
        onView(durationField(0)).check(matches(withText("180")))
        onView(withId(R.id.textPerPotDay)).perform(scrollTo()).check(matches(withText("220 ml")))
    }

    @Test
    fun calibrationSetsCustomFlow() {
        onView(withId(R.id.btnCalibrateFlow)).perform(click())
        onView(withId(R.id.editMeasuredMl)).inRoot(isDialog()).perform(replaceText("40"))
        onView(withText("Echter Durchfluss: 2,4 L/h pro Tropfer")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.btn_apply)).inRoot(isDialog()).perform(click())

        onView(withId(R.id.editCustomFlow)).check(matches(withText("2,4")))
        onView(durationField(0)).perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.textPerPotDay)).perform(scrollTo()).check(matches(withText("40 ml")))
    }

    @Test
    fun savedSetupCanBeLoadedAgain() {
        onView(withId(R.id.chipRed)).perform(click())
        onView(durationField(0)).perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.btnSavePreset)).perform(scrollTo(), click())
        onView(withId(R.id.editPresetName)).inRoot(isDialog()).perform(replaceText("Zelt 1"))
        onView(withText(R.string.btn_save)).inRoot(isDialog()).perform(click())

        onView(withId(R.id.btnReset)).perform(scrollTo(), click())
        onView(withId(R.id.cardResult)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        onView(allOf(withText("Zelt 1"), withParent(withId(R.id.chipGroupPresets)))).perform(scrollTo(), click())
        onView(withId(R.id.textPerPotDay)).perform(scrollTo()).check(matches(withText("133 ml")))
        onView(withId(R.id.chipRed)).check(matches(isChecked()))
    }
}
