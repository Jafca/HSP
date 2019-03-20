package com.jafca.hsp

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.equalTo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

@LargeTest
class FabInstrumentedTests {
    @get:Rule
    var activityRule = ActivityTestRule(MapsActivity::class.java, false, false)

    @Before
    fun before() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun menuFabInstrumentedTest() {
        activityRule.launchActivity(null)

        val menuFab = onView(withId(R.id.menuFab))
        menuFab.check(matches(withTagValue(equalTo(R.drawable.menu))))

        menuFab.perform(click())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.close))))

        menuFab.perform(click())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.menu))))
    }

    @Test
    fun parkingFabInstrumentedTest() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        activityRule.launchActivity(null)

        val menuFab = onView(withId(R.id.menuFab))
        menuFab.perform(click())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.close))))

        val parkingFab = onView(withId(R.id.parkingFab))
        parkingFab.perform(click())

        assertEquals(true, activityRule.activity.markerMap.isNotEmpty())
        parkingFab.check(matches(withTagValue(equalTo(R.string.parking_hide_tag))))
        menuFab.check(matches(withTagValue(equalTo(R.drawable.menu))))

        menuFab.perform(click())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.close))))
        parkingFab.perform(click())

        assertEquals(true, activityRule.activity.markerMap.isEmpty())
        parkingFab.check(matches(withTagValue(equalTo(R.string.parking_show_tag))))
        menuFab.check(matches(withTagValue(equalTo(R.drawable.menu))))
    }

    @Test
    fun historyFabInstrumentedTest() {
        activityRule.launchActivity(null)

        onView(withId(R.id.menuFab)).perform(click())
        val historyFab = onView(withId(R.id.historyFab))
        historyFab.perform(click())

        intended(hasComponent(HistoryActivity::class.java.name))
    }

    @Test
    fun settingsFabInstrumentedTest() {
        activityRule.launchActivity(null)

        onView(withId(R.id.menuFab)).perform(click())
        val settingsFab = onView(withId(R.id.settingsFab))
        settingsFab.perform(click())

        intended(hasComponent(SettingsActivity::class.java.name))
    }

    @Test
    fun helpFabInstrumentedTest() {
        activityRule.launchActivity(null)

        onView(withId(R.id.menuFab)).perform(click())
        val helpFab = onView(withId(R.id.helpFab))
        helpFab.perform(click())

        intended(hasComponent(HelpActivity::class.java.name))
    }

    @Test
    fun infoFabInstrumentedTest() {
        activityRule.launchActivity(null)

        val menuFab = onView(withId(R.id.menuFab))
        menuFab.perform(click())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.close))))
        val infoFab = onView(withId(R.id.infoFab))
        infoFab.perform(click())

        val expected = "Park, Save, Locate, Repeat\n\n" +
                "Version ${BuildConfig.VERSION_NAME}\n" +
                "Copyright \u00A9 ${Calendar.getInstance().get(Calendar.YEAR)} Jafca"
        onView(ViewMatchers.withText(expected)).check(matches(ViewMatchers.isDisplayed())).perform(pressBack())
        menuFab.check(matches(withTagValue(equalTo(R.drawable.menu))))
    }
}