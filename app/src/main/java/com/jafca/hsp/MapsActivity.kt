package com.jafca.hsp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.File
import java.io.IOException
import java.util.*

class MapsActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private lateinit var mMap: GoogleMap
    var markerMap: MutableMap<Marker, Int> = mutableMapOf()
        private set(value) {
            field = value
        }
    var polylineDrawn = false
    private var polylineDestination: Marker? = null
    private lateinit var polyline: Polyline
    private var fabOpen = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mDb: ParkedLocationDatabase? = null
    private var currentParkedLocation: ParkedLocation? = null
    private lateinit var mDbWorkerThread: DbWorkerThread
    private val mUiHandler = Handler()
    private lateinit var model: SharedViewModel

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_TAKE_PHOTO = 2
        private const val CURRENT_PARKING = 1
        private const val NEARBY_PARKING = 2
    }

    interface RunnableListener {
        fun onResult(result: Any)
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

        setCurrentParkedLocation(null)
        findParkingButton.tag = R.string.parking_show_tag

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
            addPhotoButton.tag = R.drawable.view_photo
        }
        menuFab.compatElevation = 20f
        menuFab.setOnClickListener {
            if (!fabOpen) {
                showFABMenu()
            } else {
                closeFABMenu()
            }
        }
        historyFab.setOnClickListener {
            val runnableListener = object : MapsActivity.RunnableListener {
                override fun onResult(result: Any) {
                    startHistoryActivity(result as LatLng)
                }
            }
            getCurrentLocation(runnableListener)
        }

        model = this.run {
            ViewModelProviders.of(this).get(SharedViewModel::class.java)
        }

        model.reminderTime.observe(this, androidx.lifecycle.Observer<Pair<Int, Int>> { hourMinute ->
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

    private fun startHistoryActivity(latLng: LatLng) {
        val intent = Intent(this, HistoryActivity::class.java)
        intent.putExtra("lat", latLng.latitude)
        intent.putExtra("lon", latLng.longitude)
        startActivity(intent)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)

        val runnableListener = object : MapsActivity.RunnableListener {
            override fun onResult(result: Any) {
                val parkedLocation = result as ParkedLocation
                setCurrentParkedLocation(result)

                val markerOptions = MarkerOptions().position(LatLng(parkedLocation.lat, parkedLocation.lon))
                markerMap[mMap.addMarker(markerOptions)] = CURRENT_PARKING

                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(parkedLocation.lat, parkedLocation.lon),
                        12f
                    )
                )
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
        // TODO: If location in shared preferences not found, set it to null
        val task = Runnable {
            val parkedLocations = mDb?.parkedLocationDao()?.getAll()
            mUiHandler.post {
                if (parkedLocations != null && parkedLocations.isNotEmpty()) {
                    runnableListener.onResult(parkedLocations.last())
                }
            }
        }
        mDbWorkerThread.postTask(task)
    }

    private fun onAddLocationButtonClick() {
        if (currentParkedLocation == null) {
            val runnableListener = object : MapsActivity.RunnableListener {
                override fun onResult(result: Any) {
                    val currentLatLng = result as LatLng
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                    insertParkedLocationInDb(
                        ParkedLocation(
                            null,
                            currentLatLng.latitude,
                            currentLatLng.longitude,
                            Date()
                        )
                    )

                    val markerOptions = MarkerOptions().position(currentLatLng)
                    markerMap[mMap.addMarker(markerOptions)] = CURRENT_PARKING
                }
            }

            getCurrentLocation(runnableListener)
        } else {
            val builder = AlertDialog.Builder(this@MapsActivity)
            builder.setTitle("Remove Pin")
            builder.setMessage(R.string.remove_pin_text)

            builder.setPositiveButton("YES") { _, _ ->
                val file = getPhoto()
                file.delete()
                deleteParkedLocationInDb()
                NotificationUtils().cancelAlarms(this@MapsActivity)
            }

            builder.setNegativeButton("CANCEL") { _, _ -> }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun onFindParkingButtonClick() {
        if (findParkingButton.tag == R.string.parking_hide_tag) {
            findParkingButton.tag = R.string.parking_show_tag

            val iterator = markerMap.iterator()
            iterator.forEach {
                if (it.value == NEARBY_PARKING) {
                    it.key.remove()
                    iterator.remove()
                }
            }

            if (polylineDrawn) {
                polyline.remove()
                polylineDrawn = false
                polylineDestination = null
            }
        } else {
            val runnableListener = object : MapsActivity.RunnableListener {
                override fun onResult(result: Any) {
                    val currentLatLng = result as LatLng

                    val builder = StringBuilder()
                    builder.append("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
                        .append("location=" + currentLatLng.latitude + "," + currentLatLng.longitude)
                        .append("&radius=10000")
                        .append("&type=parking")
                        .append("&key=" + getString(R.string.google_maps_key))
                    val url = builder.toString()

                    val runnableListener2 = object : MapsActivity.RunnableListener {
                        override fun onResult(result: Any) {
                            val apiDataParser = ApiDataParser()
                            val nearbyPlaceList: List<HashMap<String, String>> =
                                apiDataParser.parsePlaces(result as String)
                            showNearbyPlaces(nearbyPlaceList, currentLatLng)
                        }
                    }
                    val apiDataRequest = ApiDataRequest()
                    apiDataRequest.execute(runnableListener2, url, applicationContext)
                    findParkingButton.tag = R.string.parking_hide_tag
                }
            }

            getCurrentLocation(runnableListener)
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

        builder.setNegativeButton("CANCEL") { _, _ -> }
        builder.show()
    }

    private fun onAddPhotoButtonClick() {
        if (addPhotoButton.tag == R.drawable.add_photo) {
            dispatchTakePictureIntent()
        } else {
            setPic(findViewById(R.id.photoImageView), getPhoto().absolutePath)
            addPhotoButton.setImageResource(R.drawable.add_photo)
            addPhotoButton.tag = R.drawable.add_photo
        }
    }

    private fun showFABMenu() {
        fabOpen = true
        val fabHeight = menuFab.height.toFloat()
        val miniFabHeight = parkingFab.height.toFloat()
        parkingFab.animate().translationY(-fabHeight)
        historyFab.animate().translationY(-fabHeight - miniFabHeight)
        settingsFab.animate().translationY(-fabHeight - miniFabHeight * 2)
        helpFab.animate().translationY(-fabHeight - miniFabHeight * 3)
        infoFab.animate().translationY(-fabHeight - miniFabHeight * 4)
        menuFab.setImageResource(R.drawable.close)
    }

    private fun closeFABMenu() {
        fabOpen = false
        parkingFab.animate().translationY(0f)
        historyFab.animate().translationY(0f)
        settingsFab.animate().translationY(0f)
        helpFab.animate().translationY(0f)
        infoFab.animate().translationY(0f)
        menuFab.setImageResource(R.drawable.menu)
    }

    private fun showNearbyPlaces(nearbyPlaceList: List<HashMap<String, String>>, currentLatLng: LatLng) {
        Log.i("Distance count", nearbyPlaceList.size.toString())
        Log.i("Distance info", currentLatLng.toString())

        for (i in 0 until nearbyPlaceList.size) {
            val markerOptions = MarkerOptions()
            val googlePlace = nearbyPlaceList[i]

            val placeName = googlePlace["place_name"]
            val vicinity = googlePlace["vicinity"]
            val lat = java.lang.Double.parseDouble(googlePlace["lat"])
            val lng = java.lang.Double.parseDouble(googlePlace["lng"])

            val latLng = LatLng(lat, lng)
            markerOptions.position(latLng)
            markerOptions.title("$placeName: $vicinity")
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))

            markerMap[mMap.addMarker(markerOptions)] = NEARBY_PARKING

            val result = FloatArray(1)
            Location.distanceBetween(
                currentLatLng.latitude,
                currentLatLng.longitude,
                latLng.latitude,
                latLng.longitude,
                result
            )
        }

        val bounds = markerMap.keys.fold(LatLngBounds.builder()) { builder, it -> builder.include(it.position) }.build()
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val padding = width * 0.2
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds,
                width,
                height,
                padding.toInt()
            )
        )
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (polylineDrawn) {
            polyline.remove()
            polylineDrawn = false
        }

        if (marker == polylineDestination) {
            polylineDestination = null
        } else {
            val runnableListener = object : MapsActivity.RunnableListener {
                override fun onResult(result: Any) {
                    val currentLatLng = result as LatLng

                    val builder = StringBuilder()
                    builder.append("https://maps.googleapis.com/maps/api/directions/json?")
                        .append("origin=" + currentLatLng.latitude + "," + currentLatLng.longitude)
                        .append("&destination=" + marker.position.latitude + "," + marker.position.longitude)
                        .append("&mode=walking")
                        .append("&key=" + getString(R.string.google_maps_key))
                    val urlString = builder.toString()

                    val runnableListener2 = object : MapsActivity.RunnableListener {
                        override fun onResult(result: Any) {
                            val apiDataParser = ApiDataParser()
                            val path = apiDataParser.parseDirections(result as String)
                            val polylineOptions = PolylineOptions()
                            for (i in 0 until path.size) {
                                polylineOptions.addAll(path[i])
                            }

                            polyline = mMap.addPolyline(polylineOptions.color(Color.RED))
                            polylineDrawn = true
                            polylineDestination = marker
                            marker.showInfoWindow()

                            val boundsBuilder = LatLngBounds.builder()
                            boundsBuilder.include(marker.position)
                                .include(currentLatLng)

                            val bounds = boundsBuilder.build()

                            val width = resources.displayMetrics.widthPixels
                            val height = resources.displayMetrics.heightPixels
                            val padding = width * 0.2
                            mMap.animateCamera(
                                CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    width,
                                    height,
                                    padding.toInt()
                                )
                            )

                            val toast = Toast.makeText(
                                applicationContext,
                                "Walking directions are in beta. Use caution – This route may be missing sidewalks or pedestrian paths.",
                                Toast.LENGTH_LONG
                            )
                            toast.setGravity(Gravity.BOTTOM, 0, linearLayout.height)
                            toast.show()
                        }
                    }
                    val apiDataRequest = ApiDataRequest()
                    apiDataRequest.execute(runnableListener2, urlString, applicationContext)
                }
            }

            getCurrentLocation(runnableListener)
        }
        return true
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
                    addPhotoButton.tag = R.drawable.view_photo
                } else {
                    addPhotoButton.setImageResource(R.drawable.add_photo)
                    addPhotoButton.tag = R.drawable.add_photo
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

    private fun getCurrentLocation(runnableListener: MapsActivity.RunnableListener) {
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
                runnableListener.onResult(currentLatLng)
            }
        }
    }

    private fun setCurrentParkedLocation(parkedLocation: ParkedLocation?) {
        currentParkedLocation = parkedLocation
        photoImageView.visibility = View.INVISIBLE
        if (parkedLocation == null) {
            addLocationButton.setImageResource(R.drawable.pin_drop)
            addAlarmButton.setImageResource(R.drawable.add_alarm_grey)
            addNoteButton.setImageResource(R.drawable.add_note_grey)
            addPhotoButton.setImageResource(R.drawable.add_photo_grey)

            addLocationButton.tag = R.drawable.pin_drop
            addAlarmButton.tag = R.drawable.add_alarm_grey
            addNoteButton.tag = R.drawable.add_note_grey
            addPhotoButton.tag = R.drawable.add_photo_grey

            addAlarmButton.isEnabled = false
            addNoteButton.isEnabled = false
            addPhotoButton.isEnabled = false

            if (polylineDrawn) {
                polyline.remove()
                polylineDrawn = false
                polylineDestination = null
            }
        } else {
            addLocationButton.setImageResource(R.drawable.delete)
            addAlarmButton.setImageResource(R.drawable.add_alarm)
            addNoteButton.setImageResource(R.drawable.add_note)
            addLocationButton.tag = R.drawable.delete
            addAlarmButton.tag = R.drawable.add_alarm
            addNoteButton.tag = R.drawable.add_note

            if (getPhoto().exists()) {
                addPhotoButton.setImageResource(R.drawable.view_photo)
                addPhotoButton.tag = R.drawable.view_photo
            } else {
                addPhotoButton.setImageResource(R.drawable.add_photo)
                addPhotoButton.tag = R.drawable.add_photo
            }

            addAlarmButton.isEnabled = true
            addNoteButton.isEnabled = true
            addPhotoButton.isEnabled = true
        }
    }

    private fun insertParkedLocationInDb(parkedLocation: ParkedLocation) {
        // TODO: Set shared preferences to this location
        setCurrentParkedLocation(parkedLocation)
        val task = Runnable { mDb?.parkedLocationDao()?.insert(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    private fun updateParkedLocationInDb(parkedLocation: ParkedLocation) {
        val task = Runnable { mDb?.parkedLocationDao()?.update(parkedLocation) }
        mDbWorkerThread.postTask(task)
    }

    private fun deleteParkedLocationInDb() {
        // TODO: Don't delete all, set shared preferences to null
        setCurrentParkedLocation(null)
        val iterator = markerMap.iterator()
        iterator.forEach {
            if (it.value == CURRENT_PARKING) {
                it.key.remove()
                iterator.remove()
            }
        }

        val task = Runnable { mDb?.parkedLocationDao()?.deleteAll() }
        mDbWorkerThread.postTask(task)
    }

    override fun onDestroy() {
        ParkedLocationDatabase.destroyInstance()
        mDbWorkerThread.quit()
        super.onDestroy()
    }
}