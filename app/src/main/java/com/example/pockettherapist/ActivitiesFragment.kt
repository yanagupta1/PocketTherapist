package com.example.pockettherapist

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pockettherapist.adapters.AmenityAdapter
import com.example.pockettherapist.adapters.TMEventAdapter
import com.example.pockettherapist.databinding.FragmentActivitiesBinding
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ActivitiesFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentActivitiesBinding
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private lateinit var recommendationEngine: RecommendationEngine
    private val gson = Gson()

    private var initialSteps = -1f
    private var currentSteps = 0
    private var stepGoal = DEFAULT_STEP_GOAL

    // Cached data
    private var cachedAmenities: List<RecommendationEngine.ResourceDetail>? = null
    private var cachedEvents: List<RecommendationEngine.EventDetail>? = null

    private val activityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setupStepSensor()
        } else {
            binding.txtStepCount.text = "—"
            binding.txtProgressPercent.text = "Permission required for step counting"
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            loadUserLocationAndFetch(forceRefresh = false)
        } else {
            hideAllLoading()
            binding.layoutAmenitiesEmpty.visibility = View.VISIBLE
            binding.txtEventsEmpty.visibility = View.VISIBLE
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_INITIAL_STEPS = "initial_steps"
        private const val KEY_LAST_SAVE_DATE = "last_save_date"
        private const val DEFAULT_STEP_GOAL = 7000

        private const val CACHE_PREFS_NAME = "activities_cache"
        private const val KEY_CACHED_AMENITIES = "cached_amenities"
        private const val KEY_CACHED_EVENTS = "cached_events"
        private const val KEY_CACHE_LOCATION = "cache_location"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize recommendation engine
        recommendationEngine = RecommendationEngine(requireContext())

        // Setup horizontal RecyclerViews
        binding.recyclerEvents.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recyclerAmenities.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_purple)
        binding.swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.dark_card)
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }

        // Step counter setup
        loadStepGoal()
        setupEditGoalButton()
        updateStepUI()
        checkActivityPermissionAndSetupSensor()

        // Load from cache first
        loadCachedData()
    }

    // -------------------------------------------------
    // CACHING
    // -------------------------------------------------

    private fun loadCachedData() {
        val prefs = requireContext().getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)

        // Load cached events
        val cachedEventsJson = prefs.getString(KEY_CACHED_EVENTS, null)
        if (cachedEventsJson != null) {
            try {
                val type = object : TypeToken<List<RecommendationEngine.EventDetail>>() {}.type
                cachedEvents = gson.fromJson(cachedEventsJson, type)
                if (cachedEvents != null && cachedEvents!!.isNotEmpty()) {
                    displayEvents(cachedEvents!!)
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivitiesFragment", "Error parsing cached events", e)
            }
        }

        // Load cached amenities
        val cachedAmenitiesJson = prefs.getString(KEY_CACHED_AMENITIES, null)
        if (cachedAmenitiesJson != null) {
            try {
                val type = object : TypeToken<List<RecommendationEngine.ResourceDetail>>() {}.type
                cachedAmenities = gson.fromJson(cachedAmenitiesJson, type)
                if (cachedAmenities != null && cachedAmenities!!.isNotEmpty()) {
                    displayAmenities(cachedAmenities!!)
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivitiesFragment", "Error parsing cached amenities", e)
            }
        }

        // If no cache, fetch fresh data
        if (cachedEvents == null && cachedAmenities == null) {
            checkLocationPermissionAndLoad()
        }
    }

    private fun saveCache(events: List<RecommendationEngine.EventDetail>?, amenities: List<RecommendationEngine.ResourceDetail>?, location: String) {
        val prefs = requireContext().getSharedPreferences(CACHE_PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        if (events != null) {
            editor.putString(KEY_CACHED_EVENTS, gson.toJson(events))
        }
        if (amenities != null) {
            editor.putString(KEY_CACHED_AMENITIES, gson.toJson(amenities))
        }
        editor.putString(KEY_CACHE_LOCATION, location)
        editor.apply()
    }

    private fun displayEvents(events: List<RecommendationEngine.EventDetail>) {
        binding.layoutEventsLoading.visibility = View.GONE

        if (events.isNotEmpty()) {
            binding.recyclerEvents.visibility = View.VISIBLE
            binding.txtEventsEmpty.visibility = View.GONE
            binding.recyclerEvents.adapter = TMEventAdapter(events)
        } else {
            binding.recyclerEvents.visibility = View.GONE
            binding.txtEventsEmpty.visibility = View.VISIBLE
        }
    }

    private fun displayAmenities(amenities: List<RecommendationEngine.ResourceDetail>) {
        binding.layoutAmenitiesLoading.visibility = View.GONE

        if (amenities.isNotEmpty()) {
            binding.recyclerAmenities.visibility = View.VISIBLE
            binding.layoutAmenitiesEmpty.visibility = View.GONE
            binding.recyclerAmenities.adapter = AmenityAdapter(amenities)
        } else {
            binding.recyclerAmenities.visibility = View.GONE
            binding.layoutAmenitiesEmpty.visibility = View.VISIBLE
        }
    }

    private fun hideAllLoading() {
        binding.layoutEventsLoading.visibility = View.GONE
        binding.layoutAmenitiesLoading.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    // -------------------------------------------------
    // LOCATION & DATA LOADING
    // -------------------------------------------------

    private fun refreshData() {
        checkLocationPermissionAndLoad(forceRefresh = true)
    }

    private fun checkLocationPermissionAndLoad(forceRefresh: Boolean = false) {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                loadUserLocationAndFetch(forceRefresh)
            }
            else -> {
                locationPermissionLauncher.launch(permission)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadUserLocationAndFetch(forceRefresh: Boolean) {
        if (forceRefresh) {
            binding.swipeRefresh.isRefreshing = true
        } else {
            // Show loading only if no cached data
            if (cachedEvents == null) {
                binding.layoutEventsLoading.visibility = View.VISIBLE
                binding.recyclerEvents.visibility = View.GONE
                binding.txtEventsEmpty.visibility = View.GONE
            }
            if (cachedAmenities == null) {
                binding.layoutAmenitiesLoading.visibility = View.VISIBLE
                binding.recyclerAmenities.visibility = View.GONE
                binding.layoutAmenitiesEmpty.visibility = View.GONE
            }
        }

        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    loadData(loc.latitude, loc.longitude)
                } else {
                    loadData(43.0731, -89.4012) // Fallback
                }
            }
            .addOnFailureListener {
                loadData(43.0731, -89.4012) // Fallback
            }
    }

    private fun loadData(lat: Double, lng: Double) {
        lifecycleScope.launch {
            try {
                val locationString = getLocationString(lat, lng)

                val emotionData = RecommendationEngine.createEmotionData(
                    emotion = "neutral",
                    emotionScore = 0.5f,
                    sentiment = "neutral",
                    sentimentScore = 0.5f
                )

                // Load events and amenities in parallel
                val eventsDeferred = async {
                    recommendationEngine.getEventRecommendations(
                        location = locationString,
                        emotionData = emotionData
                    )
                }

                val amenitiesDeferred = async {
                    recommendationEngine.getNearbyHelp(
                        journalText = "Looking for nearby mental health resources and wellness amenities",
                        emotionData = emotionData,
                        location = locationString
                    )
                }

                val eventRecs = eventsDeferred.await()
                val nearbyHelp = amenitiesDeferred.await()

                // Update events
                if (eventRecs != null && eventRecs.events.isNotEmpty()) {
                    cachedEvents = eventRecs.events
                    displayEvents(eventRecs.events)
                } else {
                    binding.layoutEventsLoading.visibility = View.GONE
                    if (cachedEvents == null) {
                        binding.txtEventsEmpty.visibility = View.VISIBLE
                    }
                }

                // Update amenities
                if (nearbyHelp != null && nearbyHelp.resources.isNotEmpty()) {
                    cachedAmenities = nearbyHelp.resources
                    displayAmenities(nearbyHelp.resources)
                } else {
                    binding.layoutAmenitiesLoading.visibility = View.GONE
                    if (cachedAmenities == null) {
                        binding.layoutAmenitiesEmpty.visibility = View.VISIBLE
                    }
                }

                // Save to cache
                saveCache(cachedEvents, cachedAmenities, locationString)

                binding.swipeRefresh.isRefreshing = false

            } catch (e: Exception) {
                hideAllLoading()

                if (cachedEvents != null || cachedAmenities != null) {
                    Toast.makeText(requireContext(), "Could not refresh. Showing cached data.", Toast.LENGTH_SHORT).show()
                } else {
                    binding.txtEventsEmpty.visibility = View.VISIBLE
                    binding.layoutAmenitiesEmpty.visibility = View.VISIBLE
                }

                android.util.Log.e("ActivitiesFragment", "Error loading data", e)
            }
        }
    }

    private fun getLocationString(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: ""
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""

                when {
                    city.isNotEmpty() && state.isNotEmpty() -> "$city, $state"
                    city.isNotEmpty() -> city
                    state.isNotEmpty() -> state
                    country.isNotEmpty() -> country
                    else -> "latitude: $lat, longitude: $lng"
                }
            } else {
                "latitude: $lat, longitude: $lng"
            }
        } catch (e: Exception) {
            "latitude: $lat, longitude: $lng"
        }
    }

    // -------------------------------------------------
    // STEP COUNTER
    // -------------------------------------------------

    private fun checkActivityPermissionAndSetupSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupStepSensor()
                }
                else -> {
                    activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            setupStepSensor()
        }
    }

    private fun loadStepGoal() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        stepGoal = prefs.getInt(KEY_STEP_GOAL, DEFAULT_STEP_GOAL)

        val lastSaveDate = prefs.getString(KEY_LAST_SAVE_DATE, "") ?: ""
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(java.util.Date())

        if (lastSaveDate != today) {
            initialSteps = -1f
            prefs.edit()
                .putFloat(KEY_INITIAL_STEPS, -1f)
                .putString(KEY_LAST_SAVE_DATE, today)
                .apply()
        } else {
            initialSteps = prefs.getFloat(KEY_INITIAL_STEPS, -1f)
        }
    }

    private fun saveInitialSteps(steps: Float) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(java.util.Date())
        prefs.edit()
            .putFloat(KEY_INITIAL_STEPS, steps)
            .putString(KEY_LAST_SAVE_DATE, today)
            .apply()
    }

    private fun setupStepSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor == null) {
            binding.txtStepCount.text = "—"
            binding.txtProgressPercent.text = "Step counter not available"
        }
    }

    private fun setupEditGoalButton() {
        binding.btnEditGoal.setOnClickListener {
            showEditGoalDialog()
        }
    }

    private fun showEditGoalDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.enter_step_goal)
            setText(stepGoal.toString())
            selectAll()
        }

        val padding = (24 * resources.displayMetrics.density).toInt()
        editText.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.set_step_goal)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val newGoal = editText.text.toString().toIntOrNull()
                if (newGoal != null && newGoal > 0) {
                    stepGoal = newGoal
                    saveStepGoal(newGoal)
                    updateStepUI()
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveStepGoal(goal: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_STEP_GOAL, goal).apply()
    }

    private fun updateStepUI() {
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

        binding.txtStepCount.text = numberFormat.format(currentSteps)
        binding.txtStepGoal.text = "/ ${numberFormat.format(stepGoal)} steps"

        val progressPercent = if (stepGoal > 0) {
            ((currentSteps.toFloat() / stepGoal) * 100).coerceAtMost(100f).toInt()
        } else {
            0
        }

        binding.progressSteps.progress = progressPercent
        binding.txtProgressPercent.text = "$progressPercent% of daily goal"
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0]

            if (initialSteps < 0) {
                initialSteps = totalSteps
                saveInitialSteps(totalSteps)
            }

            currentSteps = (totalSteps - initialSteps).toInt().coerceAtLeast(0)
            updateStepUI()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for step counter
    }
}
