package com.example.pockettherapist

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load home ONLY on first creation
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNav.selectedItemId = R.id.nav_home
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val current = supportFragmentManager.findFragmentById(R.id.fragment_container)

            when (item.itemId) {

                R.id.nav_home -> {
                    // Avoid reloading if already on Home
                    if (current !is HomeFragment) {
                        replaceFragment(HomeFragment())
                    }
                    true
                }

                R.id.nav_profile -> {
                    // Avoid reloading if already on Profile
                    if (current !is ProfileFragment) {
                        replaceFragment(ProfileFragment())
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
