package com.jafca.hsp

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var markers: MutableList<Marker> = mutableListOf()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mDb: ParkedLocationDatabase? = null
    private var currentParkedLocation: ParkedLocation? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    private val mUiHandler = Handler()

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

        fab.setOnClickListener {
            onFabClick()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val runnableListener = object : RunnableListener {
            override fun onResult(result: ParkedLocation) {
                currentParkedLocation = result
                fab.setImageResource(R.drawable.delete)

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

    private fun fetchParkedLocationDataFromDb(runnableListener: RunnableListener) {
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

    private fun onFabClick() {
        if (currentParkedLocation == null) {
            getCurrentLocation()
            fab.setImageResource(R.drawable.delete)
        } else {
            val builder = AlertDialog.Builder(this@MapsActivity)
            builder.setTitle("Remove Pin")
            builder.setMessage("Are you sure you want to remove the pin?")

            builder.setPositiveButton("YES") { _, _ ->
                deleteParkedLocationInDb()
                fab.setImageResource(R.drawable.pin_drop)
                Toast.makeText(applicationContext, "Pin removed", Toast.LENGTH_SHORT).show()
            }

            builder.setNeutralButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
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

                Snackbar.make(
                    findViewById(R.id.myConstraintLayout),
                    "Your current location (${currentLatLng.latitude},${currentLatLng.longitude}) has been saved",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Action", null)
                    .show()
            }
        }
    }

    private fun insertParkedLocationInDb(parkedLocation: ParkedLocation) {
        currentParkedLocation = parkedLocation
        val task = Runnable { mDb?.parkedLocationDao()?.insert(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    private fun deleteParkedLocationInDb() {
        currentParkedLocation = null
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
