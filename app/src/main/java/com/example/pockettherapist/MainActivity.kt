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

        // Load Home by default
        replaceFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.nav_home -> replaceFragment(HomeFragment())
                R.id.nav_journal -> replaceFragment(JournalFragment())
                R.id.nav_insights -> replaceFragment(InsightsFragment())
                R.id.nav_suggestions -> replaceFragment(SuggestionFragment())
                R.id.nav_profile -> replaceFragment(ProfileFragment())
            }

            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
