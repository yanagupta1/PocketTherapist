package com.example.pockettherapist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pockettherapist.databinding.ActivitySigninBinding

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySigninBinding
    private val reqFgsPermissions = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UserStore.init(this)

        // Inflate UI ALWAYS (important when auto-login happens)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // AUTO LOGIN → still need UI loaded before permission request
        if (UserStore.isLoggedIn()) {
            requestFgsPermissions()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {

        binding.btnSignIn.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Both fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserStore.signIn(
                username,
                password,
                onSuccess = { requestFgsPermissions() },
                onFailure = { err ->
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.txtSignupRedirect.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    // ---------------------------------------------------------
    // Request Foreground Service permissions (Android 14+ only)
    // ---------------------------------------------------------
    private fun requestFgsPermissions() {
        if (Build.VERSION.SDK_INT >= 34) {

            val needed = ArrayList<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE_HEALTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.FOREGROUND_SERVICE_HEALTH)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
            }

            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    needed.toTypedArray(),
                    reqFgsPermissions
                )
                return
            }
        }

        startServicesAndContinue()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)

        // optional but better UX
        if (results.any { it != PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions required for step tracking", Toast.LENGTH_LONG)
                .show()
        }

        startServicesAndContinue()
    }

    // ---------------------------------------------------------
    // Start background services safely
    // ---------------------------------------------------------
    private fun startServicesAndContinue() {

        // Foreground StepSensorService
        val sensorIntent = Intent(this, StepSensorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(sensorIntent)
        } else {
            startService(sensorIntent)
        }

        // Background StepCheckService
        startService(Intent(this, StepCheckService::class.java))

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
