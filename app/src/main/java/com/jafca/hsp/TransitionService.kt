package com.jafca.hsp

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

class TransitionService : JobIntentService() {
    private lateinit var pendingIntent: PendingIntent

    companion object {
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, TransitionService::class.java, 1001, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            Intent(applicationContext, TransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (intent.extras != null) {
            if (intent.extras!!.getBoolean("start"))
                requestActivityTransitionUpdates()
            else
                removeActivityTransitionUpdates()
        }
    }

    private fun requestActivityTransitionUpdates() {
        val activities = intArrayOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING
        )

        val transitions = arrayListOf<ActivityTransition>()
        for (activity in activities) {
            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )

            transitions.add(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
        }

        val request = ActivityTransitionRequest(transitions)
        val task =
            ActivityRecognition.getClient(applicationContext).requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            Toast.makeText(
                applicationContext,
                "Started parking auto-detection",
                Toast.LENGTH_SHORT
            )
                .show()
        }

        task.addOnFailureListener {
            Toast.makeText(
                applicationContext,
                "Failed to start parking auto-detection",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    private fun removeActivityTransitionUpdates() {
        val task = ActivityRecognition.getClient(applicationContext).removeActivityTransitionUpdates(pendingIntent)

        task.addOnSuccessListener {
            Toast.makeText(
                applicationContext,
                "Stopped parking auto-detection",
                Toast.LENGTH_SHORT
            )
                .show()

        }

        task.addOnFailureListener {
            Toast.makeText(
                applicationContext,
                "Failed to stop parking auto-detection",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}