package com.jafca.hsp

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.preference.PreferenceManager

class NotificationUtils {
    fun setNotification(context: Context, timeInMilliSeconds: Long, lat: Double, lon: Double) {
        if (timeInMilliSeconds > 0) {
            cancelAlarms(context)
            val alarmManager = context.getSystemService(Activity.ALARM_SERVICE) as AlarmManager

            val defPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            with(defPrefs.edit()) {
                putLong("timestamp", timeInMilliSeconds)
                putString("lat", lat.toString())
                putString("lon", lon.toString())
                apply()
            }

            val reminderIntent = Intent(context.applicationContext, AlarmReceiver::class.java)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                timeInMilliSeconds,
                PendingIntent.getBroadcast(context, 0, reminderIntent, 0)
            )

            val checkerIntent = Intent(context.applicationContext, CheckerReceiver::class.java)
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(),
                5 * 60 * 1000, // 5 minutes
                PendingIntent.getBroadcast(context, 1, checkerIntent, 0)
            )
        }
    }

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var alarmIntent = Intent(context, AlarmReceiver::class.java)

        var pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0)
        alarmManager.cancel(pendingIntent)

        alarmIntent = Intent(context, CheckerReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(context, 1, alarmIntent, 0)
        alarmManager.cancel(pendingIntent)
    }
}