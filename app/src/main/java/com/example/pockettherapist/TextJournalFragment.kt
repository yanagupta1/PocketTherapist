package com.example.pockettherapist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.pockettherapist.databinding.FragmentTextJournalBinding

class TextJournalFragment : Fragment() {

    private lateinit var binding: FragmentTextJournalBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Analyse My Mood
        binding.btnAnalyseMood.setOnClickListener {
            val text = binding.etJournalText.text.toString().trim()

            if (text.isNotEmpty()) {
                // TODO: add mood analysis logic
            }
        }
    }
}
