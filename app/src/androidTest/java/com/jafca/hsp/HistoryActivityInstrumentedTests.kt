package com.jafca.hsp

import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.util.*

@LargeTest
class HistoryActivityInstrumentedTests {
    @get:Rule
    var activityRule = ActivityTestRule(HistoryActivity::class.java, false, false)

    @Test
    fun historyWithOneItem() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        mDb?.parkedLocationDao()?.insert(ParkedLocation(null, 1.0, 1.0, Date()))
        activityRule.launchActivity(null)

        var button = onView(withId(R.id.action_delete))
        button.check(matches(isDisplayed()))
        button.check(matches(isEnabled()))

        val layout = onView(withId(R.id.history_item_layout))
        layout.check(matches(isDisplayed()))
        layout.check(matches(isEnabled()))

        var textView = onView(withId(R.id.locationTextView))
        textView.check(matches(isDisplayed()))
        textView.check(matches(isEnabled()))

        textView = onView(withId(R.id.distanceDateTimeTextView))
        textView.check(matches(isDisplayed()))
        textView.check(matches(isEnabled()))

        button = onView(withId(R.id.editNoteButton))
        button.check(matches(isDisplayed()))
        button.check(matches(isEnabled()))

        button = onView(withId(R.id.starButton))
        button.check(matches(isDisplayed()))
        button.check(matches(isEnabled()))
        button.check(matches(withTagValue(equalTo(R.drawable.star_border))))

        textView = Espresso.onView(withId(R.id.noteTextView))
        textView.check(matches(isDisplayed()))
        textView.check(matches(isEnabled()))
    }

    @Test
    fun deletingHistoryItems() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        val note = "Starred"
        mDb?.parkedLocationDao()?.insert(ParkedLocation(null, 1.0, 1.0, Date(), note, true))
        mDb?.parkedLocationDao()?.insert(ParkedLocation(null, 2.0, 2.0, Date()))
        activityRule.launchActivity(null)

        val historyRecyclerView = Espresso.onView(withId(R.id.historyRecyclerView))
        historyRecyclerView.check(matches(hasChildCount(2)))

        val starButton = onView(allOf(withId(R.id.starButton), hasSibling(withText(note))))
        starButton.check(matches(isDisplayed()))
        starButton.check(matches(isEnabled()))
        starButton.check(matches(withTagValue(equalTo(R.drawable.star))))

        val resetButton = Espresso.onView(withId(R.id.action_delete))
        resetButton.check(matches(isDisplayed()))
        resetButton.check(matches(isEnabled()))
        resetButton.perform(click())

        onView(withText(R.string.delete_history_text)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button2)).perform(click())
        historyRecyclerView.check(matches(hasChildCount(2)))

        resetButton.perform(click())
        onView(withText(R.string.delete_history_text)).check(matches(isDisplayed()))
        onView(withId(android.R.id.button1)).perform(click())
        historyRecyclerView.check(matches(hasChildCount(1)))

        starButton.perform(click())
        starButton.check(matches(withTagValue(equalTo(R.drawable.star_border))))
        starButton.perform(ViewActions.swipeLeft())
        historyRecyclerView.check(matches(hasChildCount(0)))
    }

    @Test
    fun editHistoryItem() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        val note = "Starred"
        val newNote = "Not starred"
        mDb?.parkedLocationDao()?.insert(ParkedLocation(null, 1.0, 1.0, Date(), note, true))
        activityRule.launchActivity(null)

        val textView = Espresso.onView(withId(R.id.noteTextView))
        textView.check(matches(withText(note)))

        val editNoteButton = onView(withId(R.id.editNoteButton))
        editNoteButton.perform(click())
        onView(withId(R.id.editNoteText)).check(matches(withText(note)))
        onView(withId(R.id.editNoteText)).perform(ViewActions.replaceText(newNote))
        onView(withId(android.R.id.button2)).perform(click())
        textView.check(matches(withText(note)))

        val starButton = onView(withId(R.id.starButton))
        starButton.check(matches(withTagValue(equalTo(R.drawable.star))))
        starButton.perform(click())
        starButton.check(matches(withTagValue(equalTo(R.drawable.star_border))))

        editNoteButton.perform(click())
        onView(withId(R.id.editNoteText)).check(matches(withText(note)))
        onView(withId(R.id.editNoteText)).perform(ViewActions.replaceText(newNote))
        onView(withId(android.R.id.button1)).perform(click())
        textView.check(matches(withText(newNote)))
    }

    @Test
    fun tapHistoryItem() {
        val mDb = ParkedLocationDatabase.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
        mDb?.parkedLocationDao()?.deleteAll()
        mDb?.parkedLocationDao()?.insert(ParkedLocation(null, 1.0, 1.0, Date()))

        val mapsActivityRule = IntentsTestRule(MapsActivity::class.java, false, false)
        mapsActivityRule.launchActivity(null)
        mapsActivityRule.activity.startActivityForResult(
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                HistoryActivity::class.java
            ), 3
        )

        val data = Intent()
            .putExtra("lat", 1.0)
            .putExtra("lon", 1.0)
            .putExtra("title", "My Location")
        intending(hasComponent(HistoryActivity::class.java.name)).respondWith(
            ActivityResult(
                3,
                data
            )
        )

        val layout = onView(withId(R.id.history_item_layout))
        layout.perform(click())
        Assert.assertEquals(1, mapsActivityRule.activity.markerMap.size)
        Intents.release()
    }
}