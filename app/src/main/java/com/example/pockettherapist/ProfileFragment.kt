package com.example.pockettherapist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up gender dropdown
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.gender_options,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter

        // Load saved data
        binding.editUsername.setText(UserStore.loggedInUser ?: "")
        binding.editAge.setText(UserStore.age ?: "")

        // Set saved gender in spinner
        val savedGender = UserStore.gender
        if (savedGender != null) {
            val index = adapter.getPosition(savedGender)
            if (index >= 0) binding.spinnerGender.setSelection(index)
        }

        // Save profile
        binding.btnSaveProfile.setOnClickListener {
            val ageText = binding.editAge.text.toString()

            val ageInt = ageText.toIntOrNull()
            if (ageInt == null || ageInt < 0) {
                Toast.makeText(requireContext(), "Invalid age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = binding.spinnerGender.selectedItem.toString()

            binding.btnSaveProfile.isEnabled = false

            UserStore.updateProfile(
                age = ageText,
                gender = gender,
                onSuccess = {
                    binding.btnSaveProfile.isEnabled = true
                    Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    binding.btnSaveProfile.isEnabled = true
                    Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Logout button
        // Logout button
        binding.btnLogout.setOnClickListener {

            // Stop both background services
            requireContext().stopService(Intent(requireContext(), StepSensorService::class.java))
            requireContext().stopService(Intent(requireContext(), StepCheckService::class.java))

            // Clear user session
            UserStore.signOut()

            // Navigate back to SignIn
            startActivity(Intent(requireContext(), SignInActivity::class.java))
            requireActivity().finish()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
