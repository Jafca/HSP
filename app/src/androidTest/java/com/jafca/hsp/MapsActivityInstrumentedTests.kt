package com.jafca.hsp

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@LargeTest
class MapsActivityInstrumentedTests {
    @get:Rule
    var activityRule = ActivityTestRule(MapsActivity::class.java, false, false)

    @Test
    fun stateOfButtonsWhenNoLocationSaved() {
        // Make sure the database is empty to start with
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        activityRule.launchActivity(null)

        var button = Espresso.onView(withId(R.id.addLocationButton))
        button.check(matches(isDisplayed()))
        button.check(matches(isEnabled()))
        button.check(matches(withTagValue(equalTo(R.drawable.pin_drop))))

        button = Espresso.onView(withId(R.id.findParkingButton))
        button.check(matches(isDisplayed()))
        button.check(matches(isEnabled()))
        button.check(matches(withTagValue(equalTo(R.string.parking_show_tag))))

        button = Espresso.onView(withId(R.id.addAlarmButton))
        button.check(matches(isDisplayed()))
        button.check(matches(not(isEnabled())))
        button.check(matches(withTagValue(equalTo(R.drawable.add_alarm_grey))))

        button = Espresso.onView(withId(R.id.addPhotoButton))
        button.check(matches(isDisplayed()))
        button.check(matches(not(isEnabled())))
        button.check(matches(withTagValue(equalTo(R.drawable.add_photo_grey))))

        button = Espresso.onView(withId(R.id.addNoteButton))
        button.check(matches(isDisplayed()))
        button.check(matches(not(isEnabled())))
        button.check(matches(withTagValue(equalTo(R.drawable.add_note_grey))))
    }

    @Test
    fun addLocationButtonInstrumentedTest() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        activityRule.launchActivity(null)

        val addLocationButton = Espresso.onView(withId(R.id.addLocationButton))
        addLocationButton.check(matches(isEnabled()))
        addLocationButton.check(matches(withTagValue(equalTo(R.drawable.pin_drop))))
        addLocationButton.perform(click())

        var locationsList = mDb?.parkedLocationDao()?.getAll()
        assertTrue(locationsList != null && locationsList.isNotEmpty())
        assertEquals(1, activityRule.activity.markerMap.size)
        addLocationButton.check(matches(withTagValue(equalTo(R.drawable.done))))

        addLocationButton.perform(click())

        onView(withText(R.string.remove_pin_text)).check(matches(isDisplayed()))
        // Select CANCEL
        onView(withId(android.R.id.button2)).perform(click())
        locationsList = mDb?.parkedLocationDao()?.getAll()
        assertTrue(locationsList != null && locationsList.isNotEmpty())
        addLocationButton.check(matches(withTagValue(equalTo(R.drawable.done))))

        addLocationButton.perform(click())

        onView(withText(R.string.remove_pin_text)).check(matches(isDisplayed()))
        // Select YES
        onView(withId(android.R.id.button1)).perform(click())
        locationsList = mDb?.parkedLocationDao()?.getAll()
        assertTrue(locationsList != null && locationsList.isNotEmpty())
        assertEquals(true, activityRule.activity.markerMap.isEmpty())
        addLocationButton.check(matches(withTagValue(equalTo(R.drawable.pin_drop))))
    }

    @Test
    fun findParkingButtonInstrumentedTest() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        activityRule.launchActivity(null)

        val findParkingButton = Espresso.onView(withId(R.id.findParkingButton))
        findParkingButton.perform(click())

        assertEquals(true, activityRule.activity.markerMap.isNotEmpty())
        findParkingButton.check(matches(withTagValue(equalTo(R.string.parking_hide_tag))))

        findParkingButton.perform(click())

        assertEquals(true, activityRule.activity.markerMap.isEmpty())
        findParkingButton.check(matches(withTagValue(equalTo(R.string.parking_show_tag))))
    }

    @Test
    fun addNoteButtonInstrumentedTest() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        activityRule.launchActivity(null)

        Espresso.onView(withId(R.id.addLocationButton)).perform(click())

        val sampleText = "A simple note"
        val addNoteButton = Espresso.onView(withId(R.id.addNoteButton))
        addNoteButton.check(matches(isEnabled()))
        addNoteButton.check(matches(withTagValue(equalTo(R.drawable.add_note))))

        addNoteButton.perform(click())

        onView(withHint(R.string.note_placeholder)).check(matches(isDisplayed()))
        onView(withId(R.id.editNoteText)).perform(replaceText(sampleText))
        // Select CANCEL
        onView(withId(android.R.id.button2)).perform(click())

        addNoteButton.perform(click())

        onView(withId(R.id.editNoteText)).check(matches(withText("")))
        onView(withHint(R.string.note_placeholder)).check(matches(isDisplayed()))
        onView(withId(R.id.editNoteText)).perform(replaceText(sampleText))
        // Select SAVE
        onView(withId(android.R.id.button1)).perform(click())

        addNoteButton.perform(click())

        onView(withId(R.id.editNoteText)).check(matches(isDisplayed()))
        onView(withId(R.id.editNoteText)).check(matches(withText(sampleText)))
    }
}