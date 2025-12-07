package com.example.pockettherapist

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import java.util.Calendar

class StepCheckService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastSavedSteps = 0

    private val hourlyTask = object : Runnable {
        override fun run() {

            // If user logged out, stop service
            if (!UserStore.isLoggedIn()) {
                stopSelf()
                return
            }

            UserStore.getTodaySteps { todaySteps ->
                val stepsThisHour = todaySteps - lastSavedSteps
                lastSavedSteps = todaySteps

                UserStore.saveLastHourSteps(stepsThisHour)

                if (stepsThisHour < 50) {
                    NotificationHelper.sendInactivityNotification(this@StepCheckService)
                }

                handler.postDelayed(this, 60 * 60 * 1000) // repeat each hour
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(hourlyTask)
    }

    override fun onDestroy() {
        handler.removeCallbacks(hourlyTask)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
