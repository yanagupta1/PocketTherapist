package com.example.pockettherapist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved session
        UserStore.init(this)

        // If not logged in → go to login screen
        if (!UserStore.isLoggedIn()) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        // Create notifications FIRST (required before starting services)
        NotificationHelper.createChannels(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start background services
        startServices()

        // Default fragment
        replaceFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment()); true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment()); true
                }
                else -> false
            }
        }
    }

    private fun startServices() {
        // Foreground step counter
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, StepSensorService::class.java))
        } else {
            startService(Intent(this, StepSensorService::class.java))
        }

        // Background hourly checker
        startService(Intent(this, StepCheckService::class.java))
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
