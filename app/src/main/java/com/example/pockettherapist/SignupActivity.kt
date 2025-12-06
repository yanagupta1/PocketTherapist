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

        // -----------------------------
        // Setup Gender Dropdown (AutoCompleteTextView)
        // -----------------------------
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

        // Show dropdown when tapping the field
        binding.inputGender.setOnClickListener {
            binding.inputGender.showDropDown()
        }

        // -----------------------------
        // Sign Up Button Logic
        // -----------------------------
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val ageText = binding.etAge.text.toString().trim()
            val gender = binding.inputGender.text.toString().trim()

            // Validate username/password
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username and Password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate age
            val ageInt = ageText.toIntOrNull()
            if (ageInt == null || ageInt < 0) {
                Toast.makeText(this, "Invalid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate gender
            if (gender.isEmpty()) {
                Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Try creating account
            if (!UserStore.signUp(username, password)) {
                Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Store profile info
            UserStore.loggedInUser = username
            UserStore.age = ageText
            UserStore.gender = gender

            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()

            // Redirect to Home Screen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // -----------------------------
        // Redirect to Sign In
        // -----------------------------
        binding.txtLoginRedirect.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }
    }
}
