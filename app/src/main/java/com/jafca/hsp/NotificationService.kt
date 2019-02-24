package com.jafca.hsp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.media.RingtoneManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.JobIntentService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*
import java.util.concurrent.Executors

class NotificationService : JobIntentService() {
    private lateinit var mNotification: Notification
    private val mNotificationId: Int = 1000

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
        if (intent.extras != null) {
            val timestamp = intent.extras!!.getLong("timestamp")
            if (timestamp > 0) {
                val defPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                if (intent.extras!!.getString("reason") == "notification") {
                    sendNotification("Parking Time Limit", "Your time limit is about to expire")
                } else if (defPrefs.getBoolean(getString(R.string.pref_smart), true)){
                    val runnableListener = object : MapsActivity.RunnableListener {
                        override fun onResult(result: Any) {
                            val currentLatLng = result as LatLng
                            val lat = intent.extras!!.getDouble("lat")
                            val lon = intent.extras!!.getDouble("lon")

                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                currentLatLng.latitude,
                                currentLatLng.longitude,
                                lat,
                                lon,
                                distance
                            )
                            distance[0] = distance[0] / 1000
                            val savedSpeed= defPrefs.getString(getString(R.string.pref_speed), "")
                            val walkingSpeed = savedSpeed.toFloatOrNull() ?: 5f

                            // TODO: Calculate length of Directions API data, add to settings (e.g. Use Sample Data switch)

                            val time = (distance[0] / walkingSpeed) * 60 * 60 * 1000 // milliseconds
                            val currentTime = Calendar.getInstance().timeInMillis
                            if (currentTime + time > timestamp) {
                                val message =
                                    "You have ${"%.2f".format(distance[0])}km to walk before your time limit expires"
                                sendNotification("Parking Time Limit", message)
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
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
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

        // Set the intent that will fire when the user taps the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotification = Notification.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.alarm)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle().bigText(message))
                .setContentText(message).build()
        } else {
            mNotification = Notification.Builder(this)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.alarm)
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
}