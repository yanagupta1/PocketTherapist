package com.example.pockettherapist

import android.os.Bundle
import android.view.*
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
}
