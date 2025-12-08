package com.example.pockettherapist

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stores and retrieves daily activity data including calories, steps,
 * activity duration by state, and other metrics.
 */
object DailyActivityStore {

    private const val PREFS_NAME = "daily_activity_store"
    private const val KEY_DAILY_DATA = "daily_data"
    private const val KEY_CURRENT_DATE = "current_date"

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class DailyActivityData(
        val date: String,
        var totalCalories: Float = 0f,
        var totalSteps: Int = 0,
        var restingMinutes: Int = 0,      // "Resting" instead of Sedentary
        var lightMinutes: Int = 0,
        var moderateMinutes: Int = 0,
        var vigorousMinutes: Int = 0,
        var peakActivityLevel: Float = 0f,
        var averageActivityLevel: Float = 0f,
        var activitySamples: Int = 0,
        var lastUpdated: Long = System.currentTimeMillis()
    ) {
        fun getTotalActiveMinutes(): Int = lightMinutes + moderateMinutes + vigorousMinutes

        fun getMostActiveState(): String {
            return when {
                vigorousMinutes >= moderateMinutes && vigorousMinutes >= lightMinutes -> "Vigorous"
                moderateMinutes >= lightMinutes -> "Moderate"
                lightMinutes > 0 -> "Light"
                else -> "Resting"
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    /**
     * Get today's activity data, creating a new record if needed
     */
    fun getTodayData(context: Context): DailyActivityData {
        val prefs = getPrefs(context)
        val today = getTodayDate()
        val savedDate = prefs.getString(KEY_CURRENT_DATE, null)

        return if (savedDate == today) {
            // Load existing data
            val json = prefs.getString(KEY_DAILY_DATA, null)
            if (json != null) {
                try {
                    gson.fromJson(json, DailyActivityData::class.java)
                } catch (e: Exception) {
                    DailyActivityData(date = today)
                }
            } else {
                DailyActivityData(date = today)
            }
        } else {
            // New day - archive old data and start fresh
            archiveYesterdayData(context, savedDate)
            DailyActivityData(date = today)
        }
    }

    /**
     * Save today's activity data
     */
    fun saveTodayData(context: Context, data: DailyActivityData) {
        val prefs = getPrefs(context)
        val today = getTodayDate()

        prefs.edit()
            .putString(KEY_CURRENT_DATE, today)
            .putString(KEY_DAILY_DATA, gson.toJson(data.copy(lastUpdated = System.currentTimeMillis())))
            .apply()
    }

    /**
     * Add calories to today's total
     */
    fun addCalories(context: Context, calories: Float) {
        val data = getTodayData(context)
        data.totalCalories += calories
        saveTodayData(context, data)
    }

    /**
     * Update steps for today
     */
    fun updateSteps(context: Context, steps: Int) {
        val data = getTodayData(context)
        data.totalSteps = steps
        saveTodayData(context, data)
    }

    /**
     * Record activity sample and update stats
     */
    fun recordActivitySample(
        context: Context,
        activityLevel: Float,
        activityState: ActivityTracker.ActivityState,
        caloriesPerMinute: Float,
        durationSeconds: Float
    ) {
        val data = getTodayData(context)

        // Add calories for this duration
        val caloriesBurned = caloriesPerMinute * (durationSeconds / 60f)
        data.totalCalories += caloriesBurned

        // Track time in each activity state (convert seconds to minutes)
        val minutes = (durationSeconds / 60f).toInt().coerceAtLeast(0)
        when (activityState) {
            ActivityTracker.ActivityState.RESTING -> data.restingMinutes += minutes
            ActivityTracker.ActivityState.LIGHT -> data.lightMinutes += minutes
            ActivityTracker.ActivityState.MODERATE -> data.moderateMinutes += minutes
            ActivityTracker.ActivityState.VIGOROUS -> data.vigorousMinutes += minutes
        }

        // Update peak activity level
        if (activityLevel > data.peakActivityLevel) {
            data.peakActivityLevel = activityLevel
        }

        // Update running average
        data.activitySamples++
        data.averageActivityLevel =
            ((data.averageActivityLevel * (data.activitySamples - 1)) + activityLevel) / data.activitySamples

        saveTodayData(context, data)
    }

    /**
     * Archive data from previous day to history
     */
    private fun archiveYesterdayData(context: Context, yesterdayDate: String?) {
        if (yesterdayDate == null) return

        val prefs = getPrefs(context)
        val json = prefs.getString(KEY_DAILY_DATA, null) ?: return

        try {
            val yesterdayData = gson.fromJson(json, DailyActivityData::class.java)

            // Load history
            val historyJson = prefs.getString("activity_history", "[]")
            val historyType = object : TypeToken<MutableList<DailyActivityData>>() {}.type
            val history: MutableList<DailyActivityData> = gson.fromJson(historyJson, historyType)

            // Add yesterday's data
            history.add(yesterdayData)

            // Keep only last 30 days
            while (history.size > 30) {
                history.removeAt(0)
            }

            // Save history
            prefs.edit()
                .putString("activity_history", gson.toJson(history))
                .apply()

        } catch (e: Exception) {
            android.util.Log.e("DailyActivityStore", "Error archiving data", e)
        }
    }

    /**
     * Get historical data for past days
     */
    fun getHistory(context: Context): List<DailyActivityData> {
        val prefs = getPrefs(context)
        val historyJson = prefs.getString("activity_history", "[]")
        return try {
            val historyType = object : TypeToken<List<DailyActivityData>>() {}.type
            gson.fromJson(historyJson, historyType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get weekly summary
     */
    fun getWeeklySummary(context: Context): WeeklySummary {
        val history = getHistory(context)
        val today = getTodayData(context)

        val lastWeek = history.takeLast(6) + today

        return WeeklySummary(
            totalCalories = lastWeek.sumOf { it.totalCalories.toDouble() }.toFloat(),
            totalSteps = lastWeek.sumOf { it.totalSteps },
            totalActiveMinutes = lastWeek.sumOf { it.getTotalActiveMinutes() },
            averageActivityLevel = if (lastWeek.isNotEmpty())
                lastWeek.map { it.averageActivityLevel }.average().toFloat() else 0f,
            daysTracked = lastWeek.size
        )
    }

    data class WeeklySummary(
        val totalCalories: Float,
        val totalSteps: Int,
        val totalActiveMinutes: Int,
        val averageActivityLevel: Float,
        val daysTracked: Int
    )
}
