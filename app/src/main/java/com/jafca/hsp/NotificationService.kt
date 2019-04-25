package com.jafca.hsp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.preference.PreferenceManager
import androidx.core.app.JobIntentService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotificationService : JobIntentService() {
    private lateinit var mNotification: Notification

    companion object {
        private const val JOB_ID = 1000
        const val CHANNEL_ID = "hsp.jafca.com.CHANNEL_ID"
        const val CHANNEL_NAME = "Reminders"
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, NotificationService::class.java, JOB_ID, work)
        }
    }

    @SuppressLint("NewApi")
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel for API 26+
            val context = this.applicationContext
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableVibration(true)
            notificationChannel.setShowBadge(true)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.parseColor("#e8334a")
            notificationChannel.description = "Notifications sent when the parking time limit is about to expire."
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onHandleWork(intent: Intent) {
        createChannel()
        val defPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        if (intent.extras != null) {
            val timestamp = intent.extras!!.getLong("timestamp")
            if (timestamp > 0) {
                if (intent.extras!!.getString("reason") == "notification") {
                    sendNotification("Parking Time Limit", "Your time limit is about to expire")
                } else if (defPrefs.getBoolean(getString(R.string.pref_smart), true)) {
                    val runnableListener = object : MapsActivity.RunnableListener {
                        override fun onResult(result: Any) {
                            val currentLatLng = result as LatLng
                            val lat = intent.extras!!.getDouble("lat")
                            val lon = intent.extras!!.getDouble("lon")

                            val walkingSpeed = defPrefs.getString(getString(R.string.pref_speed), "5.0").toFloat()

                            var distance: Float
                            if (defPrefs.getBoolean(getString(R.string.pref_directDistance), true)) {
                                val directDistance = FloatArray(1)
                                Location.distanceBetween(
                                    currentLatLng.latitude,
                                    currentLatLng.longitude,
                                    lat,
                                    lon,
                                    directDistance
                                )
                                distance = directDistance[0] / 1000
                                sendDistanceNotification(distance, walkingSpeed, timestamp)
                            } else {
                                val runnableListener2 = object : MapsActivity.RunnableListener {
                                    override fun onResult(result: Any) {
                                        distance = result as Float
                                        sendDistanceNotification(distance, walkingSpeed, timestamp)
                                    }
                                }
                                getRouteLength(currentLatLng, lat, lon, runnableListener2)
                            }
                        }
                    }

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                    fusedLocationClient.lastLocation.addOnSuccessListener(
                        Executors.newSingleThreadExecutor(),
                        OnSuccessListener { location ->
                            if (location != null) {
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                runnableListener.onResult(currentLatLng)
                            }
                        })
                }
            } else if (intent.extras!!.getBoolean("widget")) {
                if (defPrefs.getLong(getString(R.string.pref_locationId), -1) == -1L) {
                    val mDb = ParkedLocationDatabase.getInstance(applicationContext)
                    val handlerThread = HandlerThread("DbThread")
                    handlerThread.start()
                    val dbHandler = Handler(handlerThread.looper)

                    val runnableListener = object : MapsActivity.RunnableListener {
                        override fun onResult(result: Any) {
                            val currentLatLng = result as LatLng
                            val parkedLocation = ParkedLocation(
                                null,
                                currentLatLng.latitude,
                                currentLatLng.longitude,
                                Date()
                            )
                            val task = Runnable {
                                val locationId = mDb?.parkedLocationDao()?.insert(parkedLocation)
                                with(PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()) {
                                    putLong(getString(R.string.pref_locationId), locationId!!)
                                    apply()
                                }
                                sendBroadcast(
                                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(
                                        AppWidgetManager.EXTRA_APPWIDGET_IDS,
                                        AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
                                            ComponentName(applicationContext, ParkingWidget::class.java)
                                        )
                                    )
                                )
                            }
                            dbHandler.post(task)

                            TransitionService.enqueueWork(applicationContext, Intent().putExtra("start", false))
                        }
                    }

                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                    fusedLocationClient.lastLocation.addOnSuccessListener(
                        Executors.newSingleThreadExecutor(),
                        OnSuccessListener { location ->
                            if (location != null) {
                                val currentLatLng = LatLng(location.latitude, location.longitude)
                                runnableListener.onResult(currentLatLng)
                            }
                        })
                }
            }
        } else if (defPrefs.getBoolean(getString(R.string.pref_detectParking), true)) {
            sendNotification("Parking Detected", "Do you want to save your parked location?", 1001)
        }
    }

    private fun sendDistanceNotification(distance: Float, walkingSpeed: Float, timestamp: Long) {
        val time = (distance / walkingSpeed) * 60 * 60 * 1000 // milliseconds
        val currentTime = Calendar.getInstance().timeInMillis
        if (currentTime + time > timestamp) {
            val timeStr = "${TimeUnit.MILLISECONDS.toMinutes(time.toLong()) + 1} minutes"
            val message =
                "You have $timeStr before your time limit expires.\nIf you start walking now, you will get there in time"
            sendNotification("Parking Time Limit", message)
        }
    }

    private fun sendNotification(title: String, message: String, mNotificationId: Int = 1000) {
        val context = this.applicationContext
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifyIntent = Intent(this, MapsActivity::class.java)

        notifyIntent.putExtra("title", title)
        notifyIntent.putExtra("message", message)
        notifyIntent.putExtra("notification", true)

        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val res = this.resources
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val icon = if (mNotificationId == 1000) R.drawable.alarm else R.drawable.pin_drop

        // Set the intent that will fire when the user taps the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotification = Notification.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setContentText(message).build()
        } else {
            mNotification = Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setSmallIcon(icon)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setSound(uri)
                .setContentText(message).build()
        }

        NotificationUtils().cancelAlarms(this.applicationContext)

        // mNotificationId must be a unique int for each notification
        notificationManager.notify(mNotificationId, mNotification)
    }

    fun getRouteLength(
        currentLatLng: LatLng, parkedLat: Double, parkedLng: Double,
        runnableListener: MapsActivity.RunnableListener
    ) {
        val builder = StringBuilder()
        builder.append("https://maps.googleapis.com/maps/api/directions/json?")
            .append("origin=" + currentLatLng.latitude + "," + currentLatLng.longitude)
            .append("&destination=$parkedLat,$parkedLng")
            .append("&mode=walking")
            .append("&key=" + getString(R.string.google_maps_key))
        val urlString = builder.toString()

        val runnableListener2 = object : MapsActivity.RunnableListener {
            override fun onResult(result: Any) {
                val apiDataParser = ApiDataParser()
                val pathLength = apiDataParser.parseDirectionsDistance(result as String)
                runnableListener.onResult(pathLength / 1000)
            }
        }
        val apiDataRequest = ApiDataRequest()
        apiDataRequest.execute(runnableListener2, urlString, applicationContext)
    }
}