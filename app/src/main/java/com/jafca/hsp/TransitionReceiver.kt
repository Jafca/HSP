package com.jafca.hsp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

class TransitionReceiver : BroadcastReceiver() {
    lateinit var mContext: Context

    override fun onReceive(context: Context, intent: Intent) {
        mContext = context

        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            var message = "Events:\n"
            for (event in result!!.transitionEvents) {
                val activityType = toActivityString(event.activityType)
                val transitionType = toTransitionString(event.transitionType)
                message += "activityType: $activityType, transitionType: $transitionType\n"

                if (event.activityType == DetectedActivity.IN_VEHICLE && event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_EXIT) {
                    NotificationService.enqueueWork(context, Intent())
                }
            }

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toActivityString(activity: Int): String {
        return when (activity) {
            DetectedActivity.IN_VEHICLE -> mContext.resources.getString(R.string.in_vehicle)
            DetectedActivity.ON_BICYCLE -> mContext.resources.getString(R.string.on_bicycle)
            DetectedActivity.STILL -> mContext.resources.getString(R.string.still)
            DetectedActivity.WALKING -> mContext.resources.getString(R.string.walking)
            DetectedActivity.RUNNING -> mContext.resources.getString(R.string.running)
            else -> "UNKNOWN"
        }
    }

    private fun toTransitionString(transitionType: Int): String {
        return when (transitionType) {
            ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ENTER"
            ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "EXIT"
            else -> "UNKNOWN"
        }
    }
}