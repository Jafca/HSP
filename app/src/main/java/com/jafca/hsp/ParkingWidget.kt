package com.jafca.hsp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.RemoteViews

class ParkingWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.parking_widget)
            if (PreferenceManager.getDefaultSharedPreferences(context).getLong(
                    context.resources.getString(R.string.pref_locationId),
                    -1
                ) != -1L
            ) {
                views.setImageViewResource(R.id.widgetImageView, R.drawable.ic_delete_widget)
            } else {
                views.setImageViewResource(R.id.widgetImageView, R.drawable.ic_pin_drop_widget)
            }

            views.setOnClickPendingIntent(R.id.widgetImageView, getPendingIntent(context))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getPendingIntent(context: Context): PendingIntent {
            return if (PreferenceManager.getDefaultSharedPreferences(context).getLong(
                    context.resources.getString(R.string.pref_locationId),
                    -1
                ) != -1L
            ) {
                val intent = Intent(context, MapsActivity::class.java)
                intent.action = "DELETE_LOCATION"
                PendingIntent.getActivity(context, 2, intent, 0)
            } else {
                val intent = Intent(context, TransitionReceiver::class.java)
                PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
    }
}