package com.jafca.hsp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
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
        recyclerView?.addItemDecoration(
            DividerItemDecoration(
                this@HistoryActivity,
                DividerItemDecoration.VERTICAL
            )
        )
        recyclerView?.layoutManager = mLinearLayoutManager

        val task = Runnable {
            var parkedLocations = mDb?.parkedLocationDao()?.getAll()
            mUiHandler.post {
                if (parkedLocations == null) {
                    parkedLocations = mutableListOf()
                }
                val mMailAdapter = ParkedLocationAdapter(
                    this,
                    parkedLocations ?: mutableListOf(),
                    LatLng(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lon", 0.0)),
                    this@HistoryActivity
                )
                recyclerView?.adapter = mMailAdapter

                val itemTouchHelper = ItemTouchHelper(
                    SwipeToDeleteCallback(
                        mMailAdapter,
                        ContextCompat.getDrawable(applicationContext, R.drawable.delete)!!
                    )
                )
                itemTouchHelper.attachToRecyclerView(recyclerView)

                postViewModel = ViewModelProviders.of(this).get(ParkedLocationViewModel::class.java)
                postViewModel.getAllParkedLocations()
                    .observe(this, Observer<List<ParkedLocation>> { posts -> mMailAdapter.setData(posts) })
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
                val task = Runnable { mDb?.parkedLocationDao()?.deleteNonStarred() }
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
        postViewModel.deleteParkedLocation(parkedLocation)
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