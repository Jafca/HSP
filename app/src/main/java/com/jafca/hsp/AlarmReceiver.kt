package com.jafca.hsp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val defPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val timestamp = defPrefs.getLong("timestamp", 0)

        val service = Intent()
        service.putExtra("reason", "notification")
        service.putExtra("timestamp", timestamp)
        NotificationService.enqueueWork(context, service)
    }
}