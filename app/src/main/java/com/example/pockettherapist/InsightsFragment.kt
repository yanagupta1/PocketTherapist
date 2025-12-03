package com.example.pockettherapist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.*

class InsightsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_insights, container, false)
    }
}
