package com.jafca.hsp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager

class CheckerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val defPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lat = defPrefs.getString("lat", "").toDouble()
        val lon = defPrefs.getString("lon", "").toDouble()
        val timestamp = defPrefs.getLong("timestamp", 0)

        val service = Intent()
        service.putExtra("reason", "checker")
        service.putExtra("lat", lat)
        service.putExtra("lon", lon)
        service.putExtra("timestamp", timestamp)
        NotificationService.enqueueWork(context, service)
    }
}