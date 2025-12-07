package com.example.pockettherapist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID_FOREGROUND = "step_tracker_channel"
    private const val CHANNEL_ID_INACTIVITY = "inactivity_alert_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val fg = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Step Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val inactivity = NotificationChannel(
                CHANNEL_ID_INACTIVITY,
                "Inactivity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(fg)
            manager.createNotificationChannel(inactivity)
        }
    }

    fun createForegroundNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
            .setContentTitle("Tracking your steps")
            .setContentText("PocketTherapist step counter is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    fun sendInactivityNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INACTIVITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to Move!")
            .setContentText("You walked less than 50 steps in the last hour.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        manager.notify(2002, notification)
    }
}
