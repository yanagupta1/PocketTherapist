package com.example.pockettherapist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.example.pockettherapist.databinding.FragmentActivitiesBinding
import java.text.NumberFormat
import java.util.Locale

class ActivitiesFragment : Fragment(), SensorEventListener {

    private lateinit var binding: FragmentActivitiesBinding
    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null

    private var initialSteps = -1f
    private var currentSteps = 0
    private var stepGoal = DEFAULT_STEP_GOAL

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setupStepSensor()
        } else {
            binding.txtStepCount.text = "—"
            binding.txtProgressPercent.text = "Permission required for step counting"
        }
    }

    companion object {
        private const val PREFS_NAME = "step_counter_prefs"
        private const val KEY_STEP_GOAL = "step_goal"
        private const val KEY_INITIAL_STEPS = "initial_steps"
        private const val KEY_LAST_SAVE_DATE = "last_save_date"
        private const val DEFAULT_STEP_GOAL = 7000
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadStepGoal()
        setupEditGoalButton()
        updateStepUI()
        checkPermissionAndSetupSensor()
    }

    private fun checkPermissionAndSetupSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED -> {
                    setupStepSensor()
                }
                else -> {
                    permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }
        } else {
            // No runtime permission needed before Android Q
            setupStepSensor()
        }
    }

    private fun loadStepGoal() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        stepGoal = prefs.getInt(KEY_STEP_GOAL, DEFAULT_STEP_GOAL)

        // Check if it's a new day - reset initial steps
        val lastSaveDate = prefs.getString(KEY_LAST_SAVE_DATE, "") ?: ""
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(java.util.Date())

        if (lastSaveDate != today) {
            // New day, reset the counter
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
            // Device doesn't have a step counter sensor
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
                // First reading of the day, store this as our baseline
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
