package com.jafca.hsp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_help.*

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        setSupportActionBar(findViewById(R.id.helpToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val helpItems = arrayListOf<Pair<Int, String>>()
        helpItems.add(Pair(R.drawable.pin_drop, "Tap to save your current location onto the map"))
        helpItems.add(Pair(R.drawable.delete, "Tap then confirm to remove your parked location"))
        helpItems.add(Pair(R.drawable.add_alarm, "Tap and set an alarm to remind you when the time limit expires"))
        helpItems.add(Pair(R.drawable.add_note, "Tap and set a note to help locate your car"))
        helpItems.add(Pair(R.drawable.add_photo, "Tap and take a photo to help locate your car"))
        helpItems.add(Pair(R.drawable.view_photo, "Tap to view the photo you took"))
        helpItems.add(Pair(R.drawable.share, "Share your parked location"))
        helpItems.add(Pair(R.drawable.my_location, "Tap to find your current location on the map"))
        helpItems.add(Pair(R.drawable.menu, "Tap to open the menu"))
        helpItems.add(Pair(R.drawable.close, "Tap to close the menu"))
        helpItems.add(Pair(R.drawable.find_parking, "Tap to find nearby parking"))
        helpItems.add(
            Pair(
                R.drawable.history, "Tap to see your parking history\n" +
                        "- Tap a location to see it on the map\n" +
                        "- Tap the note button to edit the note\n" +
                        "- Tap the star to star the location\n" +
                        "- Tap the delete button then confirm to delete all non-starred locations"
            )
        )
        helpItems.add(
            Pair(
                R.drawable.settings, "Tap to open Settings\n" +
                        "- Smart Reminders are sent earlier, the further away you are from your car\n" +
                        "- Auto-detect will send a notification once you have parked"
            )
        )
        helpItems.add(Pair(R.drawable.reset, "Tap to reset settings to default values"))
        helpItems.add(Pair(R.drawable.help, "Tap to open this Help page"))
        helpItems.add(Pair(R.drawable.info, "Tap to learn about this parking app"))
        helpItems.add(Pair(R.drawable.red_marker, "Tap to show information about the location and a route to it"))

        helpRecyclerView?.addItemDecoration(
            DividerItemDecoration(
                this@HelpActivity,
                DividerItemDecoration.VERTICAL
            )
        )
        helpRecyclerView?.layoutManager = LinearLayoutManager(this@HelpActivity)
        helpRecyclerView.adapter = HelpAdapter(helpItems)
    }
}