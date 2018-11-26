package com.jafca.hsp

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.EditText
import android.widget.ImageView
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
import java.io.File
import java.io.IOException
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
    private val REQUEST_TAKE_PHOTO = 1

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
        findParkingButton.setOnClickListener {
            onFindParkingButtonClick()
        }
        addAlarmButton.setOnClickListener {
            onAddAlarmButtonClick()
        }
        addNoteButton.setOnClickListener {
            onAddNoteButtonClick()
        }
        addPhotoButton.setOnClickListener {
            onAddPhotoButtonClick()
        }
        photoImageView.setOnClickListener {
            photoImageView.visibility = View.INVISIBLE
            addPhotoButton.setImageResource(R.drawable.view_photo)
            addPhotoButton.tag = "view"
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
                val file = getPhoto()
                file.delete()
                deleteParkedLocationInDb()
                NotificationUtils().cancelAlarms(this@MapsActivity)
            }

            builder.setNeutralButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onFindParkingButtonClick() {
        mMap.clear()
        val builder = StringBuilder()
        builder.append("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
            .append("location=" + currentParkedLocation!!.lat + "," + currentParkedLocation!!.lon)
            .append("&radius=10000")
            .append("&type=parking")
            .append("&sensor=true")
            .append("&key=" + getString(R.string.google_maps_key))

        val url = builder.toString()
        val getNearbyPlacesData = GetNearbyPlacesData()
        getNearbyPlacesData.execute(mMap, url, applicationContext)
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

    private fun onAddPhotoButtonClick() {
        if (addPhotoButton.tag == "add") {
            dispatchTakePictureIntent()
        } else {
            setPic(findViewById(R.id.photoImageView), getPhoto().absolutePath)
            addPhotoButton.setImageResource(R.drawable.add_photo)
            addPhotoButton.tag = "add"
        }
    }

    @Throws(IOException::class)
    private fun getPhoto(): File {
        return File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "HSP_${currentParkedLocation?.lat.toString() + currentParkedLocation?.lon.toString()}.jpg"
        )
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    getPhoto()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.jafca.hsp.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_TAKE_PHOTO -> {
                photoImageView.visibility = View.INVISIBLE
                if (getPhoto().exists()) {
                    addPhotoButton.setImageResource(R.drawable.view_photo)
                    addPhotoButton.tag = "view"
                } else {
                    addPhotoButton.setImageResource(R.drawable.add_photo)
                    addPhotoButton.tag = "add"
                }
            }
        }
    }

    private fun setPic(imgView: ImageView, filePath: String) {
        val targetW: Int = imgView.width
        val targetH: Int = imgView.height

        val bfOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, this)
            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = Math.min(photoW / targetW, photoH / targetH)

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }
        BitmapFactory.decodeFile(filePath, bfOptions)?.also { bitmap ->
            imgView.setImageBitmap(bitmap)
        }
        imgView.visibility = View.VISIBLE
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
        photoImageView.visibility = View.INVISIBLE
        if (parkedLocation == null) {
            addLocationButton.setImageResource(R.drawable.pin_drop)
            addAlarmButton.setImageResource(R.drawable.add_alarm_grey)
            addAlarmButton.isEnabled = false
            addNoteButton.setImageResource(R.drawable.add_note_grey)
            addNoteButton.isEnabled = false
            addPhotoButton.setImageResource(R.drawable.add_photo_grey)
            addPhotoButton.isEnabled = true
        } else {
            addLocationButton.setImageResource(R.drawable.delete)
            addAlarmButton.setImageResource(R.drawable.add_alarm)
            addAlarmButton.isEnabled = true
            addNoteButton.setImageResource(R.drawable.add_note)
            addNoteButton.isEnabled = true
            if (getPhoto().exists()) {
                addPhotoButton.setImageResource(R.drawable.view_photo)
                addPhotoButton.tag = "view"
            } else {
                addPhotoButton.setImageResource(R.drawable.add_photo)
                addPhotoButton.tag = "add"
            }
            addPhotoButton.isEnabled = true
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
