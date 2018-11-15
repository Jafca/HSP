package com.jafca.hsp

import android.arch.lifecycle.ViewModelProviders
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.widget.EditText
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*


class MapsActivity : FragmentActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var markers: MutableList<Marker> = mutableListOf()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mDb: ParkedLocationDatabase? = null
    private var currentParkedLocation: ParkedLocation? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    private val mUiHandler = Handler()
    private lateinit var model: SharedViewModel

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    interface RunnableListener {
        fun onResult(result: ParkedLocation)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mDbWorkerThread = DbWorkerThread("dbWorkerThread")
        mDbWorkerThread.start()
        mDb = ParkedLocationDatabase.getInstance(this)

        addLocationButton.setOnClickListener {
            onAddLocationButtonClick()
        }
        addAlarmButton.setOnClickListener {
            onAddAlarmButtonClick()
        }
        addNoteButton.setOnClickListener {
            onAddNoteButtonClick()
        }

        model = this.run {
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
        }

        model.reminderTime.observe(this, android.arch.lifecycle.Observer<Pair<Int, Int>> { hourMinute ->
            if (currentParkedLocation != null) {
                val calendar: Calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.SECOND, 0)
                    if (hourMinute != null) {
                        set(Calendar.HOUR_OF_DAY, hourMinute.first)
                    }
                    if (hourMinute != null) {
                        set(Calendar.MINUTE, hourMinute.second)
                    }
                }
                val mNotificationTime = calendar.timeInMillis
                NotificationUtils().setNotification(mNotificationTime, this@MapsActivity)
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val runnableListener = object : MapsActivity.RunnableListener {
            override fun onResult(result: ParkedLocation) {
                setCurrentParkedLocation(result)

                val markerOptions = MarkerOptions().position(LatLng(result.lat, result.lon))
                markers.add(mMap.addMarker(markerOptions))

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(result.lat, result.lon), 12f))
            }
        }

        fetchParkedLocationDataFromDb(runnableListener)

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        mMap.isMyLocationEnabled = true
    }

    private fun fetchParkedLocationDataFromDb(runnableListener: MapsActivity.RunnableListener) {
        val task = Runnable {
            val parkedLocations = mDb?.parkedLocationDao()?.getAll()
            mUiHandler.post {
                if (parkedLocations != null && parkedLocations.isNotEmpty()) {
                    runnableListener.onResult(parkedLocations[0])
                }
            }
        }
        mDbWorkerThread.postTask(task)
    }

    private fun onAddLocationButtonClick() {
        if (currentParkedLocation == null) {
            getCurrentLocation()
        } else {
            val builder = AlertDialog.Builder(this@MapsActivity)
            builder.setTitle("Remove Pin")
            builder.setMessage("Are you sure you want to remove the pin?")

            builder.setPositiveButton("YES") { _, _ ->
                deleteParkedLocationInDb()
                NotificationUtils().cancelAlarms(this@MapsActivity)
            }

            builder.setNeutralButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onAddAlarmButtonClick() {
        TimePickerFragment().show(supportFragmentManager, "timePicker")
    }

    private fun onAddNoteButtonClick() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Parking Note")
        val dialogLayout = inflater.inflate(R.layout.alert_dialog_note, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.editNoteText)
        if (!currentParkedLocation?.note.isNullOrBlank())
            editText.setText(currentParkedLocation?.note)
        builder.setView(dialogLayout)

        builder.setPositiveButton("SAVE") { _, _ ->
            currentParkedLocation?.note = editText.text.toString()
            val location = currentParkedLocation
            if (location != null)
                updateParkedLocationInDb(location)
        }

        builder.setNeutralButton("CANCEL") { _, _ -> }
        builder.show()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                deleteParkedLocationInDb()
                insertParkedLocationInDb(ParkedLocation(currentLatLng))

                val markerOptions = MarkerOptions().position(currentLatLng)
                markers.add(mMap.addMarker(markerOptions))
            }
        }
    }

    private fun setCurrentParkedLocation(parkedLocation: ParkedLocation?) {
        currentParkedLocation = parkedLocation
        if (parkedLocation == null) {
            addLocationButton.setImageResource(R.drawable.pin_drop)
            addAlarmButton.setImageResource(R.drawable.add_alarm_grey)
            addAlarmButton.isEnabled = false
            addNoteButton.setImageResource(R.drawable.add_note_grey)
            addNoteButton.isEnabled = false
        } else {
            addLocationButton.setImageResource(R.drawable.delete)
            addAlarmButton.setImageResource(R.drawable.add_alarm)
            addAlarmButton.isEnabled = true
            addNoteButton.setImageResource(R.drawable.add_note)
            addNoteButton.isEnabled = true
        }
    }

    private fun insertParkedLocationInDb(parkedLocation: ParkedLocation) {
        setCurrentParkedLocation(parkedLocation)
        val task = Runnable { mDb?.parkedLocationDao()?.insert(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    private fun updateParkedLocationInDb(parkedLocation: ParkedLocation) {
        val task = Runnable { mDb?.parkedLocationDao()?.update(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    private fun deleteParkedLocationInDb() {
        setCurrentParkedLocation(null)
        markers.forEach { marker -> marker.remove() }

        val task = Runnable { mDb?.parkedLocationDao()?.deleteAll() }
        mDbWorkerThread.postTask(task)
    }

    override fun onDestroy() {
        ParkedLocationDatabase.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
    }
}
