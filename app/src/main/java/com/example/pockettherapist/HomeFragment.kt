package com.example.pockettherapist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val user = UserStore.loggedInUser ?: "User"
        binding.txtWelcome.text = "Welcome $user,\nHow are we feeling today?"

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // JOURNAL
        binding.cardJournal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, JournalOptionsFragment())
                .addToBackStack(null)
                .commit()
        }

        // ACTIVITIES NEAR ME
        binding.cardActivities.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NearbyFragment())
                .addToBackStack(null)
                .commit()
        }
    }

}
