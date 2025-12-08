package com.example.pockettherapist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private lateinit var binding: FragmentOnboardingBinding
    private var username: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the username passed from HomeFragment
        username = arguments?.getString("username") ?: ""

        // Continue button → finish onboarding
        binding.btnContinue.setOnClickListener {
            if (username.isNotEmpty()) {
                // Mark this user as no longer first-time
                FirstTimeStore.setNotFirstTime(requireContext(), username)
            }

            // Navigate back to HomeFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }

    companion object {
        fun newInstance(username: String): OnboardingFragment {
            val f = OnboardingFragment()
            val args = Bundle()
            args.putString("username", username)
            f.arguments = args
            return f
        }
    }
}
