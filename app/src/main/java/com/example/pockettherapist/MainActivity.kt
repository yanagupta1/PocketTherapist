package com.example.pockettherapist

import android.content.res.ColorStateList
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show HOME first (user already logged in)
        replaceFragment(HomeFragment())
        updateNavIconColor(R.id.nav_home)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    updateNavIconColor(R.id.nav_home)
                    true
                }
                R.id.nav_activities -> {
                    replaceFragment(ActivitiesFragment())
                    updateNavIconColor(R.id.nav_activities)
                    true
                }
                R.id.nav_recommendations -> {
                    replaceFragment(RecommendationsFragment())
                    updateNavIconColor(R.id.nav_recommendations)
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    updateNavIconColor(R.id.nav_profile)
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun updateNavIconColor(selectedItemId: Int) {
        val accentColor = when (selectedItemId) {
            R.id.nav_home -> R.color.accent_blue
            R.id.nav_activities -> R.color.accent_purple
            R.id.nav_recommendations -> R.color.accent_orange
            R.id.nav_profile -> R.color.accent_red
            else -> R.color.accent_blue
        }

        val unselectedColor = ContextCompat.getColor(this, R.color.hint_color)
        val selectedColor = ContextCompat.getColor(this, accentColor)

        val colorStateList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(selectedColor, unselectedColor)
        )

        binding.bottomNav.itemIconTintList = colorStateList
        binding.bottomNav.itemTextColor = colorStateList
    }
}
