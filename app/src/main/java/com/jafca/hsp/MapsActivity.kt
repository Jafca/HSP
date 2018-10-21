package com.jafca.hsp

import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mDb: ParkedLocationDatabase? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    private val mUiHandler = Handler()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
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

        val fab: View = findViewById(R.id.fab)
        fab.setOnClickListener {
            getCurrentLocation()
        }
        fetchParkedLocationDataFromDb()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
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
                insertParkedLocationInDb(ParkedLocation(currentLatLng))

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

    private fun fetchParkedLocationDataFromDb() {
        val task = Runnable {
            val parkedLocation = mDb?.parkedLocationDao()?.getAll()
            mUiHandler.post {
                val str = if (parkedLocation == null || parkedLocation.isEmpty()) {
                    "No previous locations saved in database"
                } else {
                    "Last saved location is ${parkedLocation[0].lat}, ${parkedLocation[0].lon}"
                }
                Toast.makeText(this, str, Toast.LENGTH_LONG).show()
            }
        }
        mDbWorkerThread.postTask(task)
    }

    private fun insertParkedLocationInDb(parkedLocation: ParkedLocation) {
        val task = Runnable { mDb?.parkedLocationDao()?.insert(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    override fun onDestroy() {
        ParkedLocationDatabase.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
    }
}
