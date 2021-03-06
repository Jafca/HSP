package com.jafca.hsp

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_history.*

class HistoryActivity : AppCompatActivity(), ParkedLocationAdapter.HistoryListener {
    private var mDb: ParkedLocationDatabase? = null
    private lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    private val mUiHandler = Handler()
    private lateinit var postViewModel: ParkedLocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        setSupportActionBar(findViewById(R.id.historyToolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mDb = ParkedLocationDatabase.getInstance(this)
        handlerThread = HandlerThread("DbHistoryThread")
        handlerThread.start()
        val looper = handlerThread.looper
        handler = Handler(looper)

        val mLinearLayoutManager = LinearLayoutManager(
            this@HistoryActivity,
            RecyclerView.VERTICAL, false
        )
        historyRecyclerView?.addItemDecoration(
            DividerItemDecoration(
                this@HistoryActivity,
                DividerItemDecoration.VERTICAL
            )
        )
        historyRecyclerView?.layoutManager = mLinearLayoutManager

        val task = Runnable {
            var parkedLocations = mDb?.parkedLocationDao()?.getAll()
            mUiHandler.post {
                if (parkedLocations == null) {
                    parkedLocations = mutableListOf()
                }
                val parkedLocationAdapter = ParkedLocationAdapter(
                    this,
                    parkedLocations ?: mutableListOf(),
                    LatLng(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lon", 0.0)),
                    this@HistoryActivity
                )
                historyRecyclerView?.adapter = parkedLocationAdapter

                val itemTouchHelper = ItemTouchHelper(
                    SwipeToDeleteCallback(
                        parkedLocationAdapter,
                        ContextCompat.getDrawable(applicationContext, R.drawable.delete)!!
                    )
                )
                itemTouchHelper.attachToRecyclerView(historyRecyclerView)

                postViewModel = ViewModelProviders.of(this).get(ParkedLocationViewModel::class.java)
                postViewModel.getAllParkedLocations()
                    .observe(this, Observer<List<ParkedLocation>> { posts -> parkedLocationAdapter.setData(posts) })
            }
        }
        handler.post(task)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.historymenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_delete -> {
            val builder = AlertDialog.Builder(this@HistoryActivity)
            builder.setTitle("Delete History")
            builder.setMessage(getString(R.string.delete_history_text))

            builder.setPositiveButton("YES") { _, _ ->
                val task = Runnable {
                    mDb?.parkedLocationDao()?.deleteNonStarred()

                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                    val parkedLocation = mDb?.parkedLocationDao()?.getById(
                        sharedPrefs.getLong(
                            getString(R.string.pref_locationId), -1
                        )
                    )

                    if (parkedLocation == null) {
                        with(sharedPrefs.edit()) {
                            putLong(getString(R.string.pref_locationId), -1)
                            apply()
                        }

                        NotificationUtils().cancelAlarms(applicationContext)

                        sendBroadcast(
                            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                                    ComponentName(applicationContext, ParkingWidget::class.java)
                                )
                            )
                        )
                    }
                }
                handler.post(task)
            }

            builder.setNegativeButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun update(parkedLocation: ParkedLocation) {
        postViewModel.updateParkedLocation(parkedLocation)
    }

    override fun delete(parkedLocation: ParkedLocation) {
        val task = Runnable {
            val id = parkedLocation.id
            mDb?.parkedLocationDao()?.delete(parkedLocation)
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            if (id == sharedPrefs.getLong(getString(R.string.pref_locationId), -1)) {
                with(sharedPrefs.edit()) {
                    putLong(getString(R.string.pref_locationId), -1)
                    apply()
                }

                NotificationUtils().cancelAlarms(applicationContext)

                sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_IDS,
                        AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                            ComponentName(applicationContext, ParkingWidget::class.java)
                        )
                    )
                )
            }
        }
        handler.post(task)
    }

    override fun returnLocation(lat: Double, lon: Double, title: String) {
        val data = Intent()
        data.putExtra("lat", lat)
        data.putExtra("lon", lon)
        data.putExtra("title", title)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    override fun onDestroy() {
        ParkedLocationDatabase.destroyInstance()
        handlerThread.quit()
        super.onDestroy()
    }
}