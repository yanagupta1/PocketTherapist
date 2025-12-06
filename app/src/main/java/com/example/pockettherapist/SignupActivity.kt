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

        // Setup Gender Dropdown
        binding.inputGender.setOnClickListener {
            binding.inputGender.showDropDown()
        }

        val genderOptions = listOf("Male", "Female", "Other")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            genderOptions
        )
        binding.inputGender.setAdapter(adapter)

        binding.inputGender.setOnClickListener {
            binding.inputGender.showDropDown()
        }

        // Sign Up Button Logic
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()
            val gender = binding.inputGender.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and Password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ageInt = ageText.toIntOrNull()
            if (ageInt == null || ageInt < 0) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSignup.isEnabled = false

            UserStore.signUp(
                username = username,
                password = password,
                age = ageText,
                gender = gender,
                onSuccess = {
                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                onFailure = { error ->
                    binding.btnSignup.isEnabled = true
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Redirect to Sign In
        binding.txtLoginRedirect.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}
