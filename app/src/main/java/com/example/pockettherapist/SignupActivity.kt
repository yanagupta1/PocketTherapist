package com.example.pockettherapist

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.pockettherapist.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        UserStore.init(this)

        // -------------------------------
        // GENDER DROPDOWN SETUP (FIXED)
        // -------------------------------
        val genderOptions = listOf("Male", "Female", "Other")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )

        binding.inputGender.setAdapter(adapter)

        // *** IMPORTANT FIX ***
        binding.inputGender.setOnClickListener {
            binding.inputGender.showDropDown()
        }

        // -------------------------------
        // SIGNUP BUTTON
        // -------------------------------
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val age = binding.etAge.text.toString().trim()
            val gender = binding.inputGender.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserStore.signUp(
                username,
                password,
                age,
                gender,
                onSuccess = {
                    startBackgroundServices()
                    Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onFailure = {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // -------------------------------
        // REDIRECT TO LOGIN
        // -------------------------------
        binding.txtLoginRedirect.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }

    private fun startBackgroundServices() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, StepSensorService::class.java))
        } else {
            startService(Intent(this, StepSensorService::class.java))
        }

        startService(Intent(this, StepCheckService::class.java))
    }
}
