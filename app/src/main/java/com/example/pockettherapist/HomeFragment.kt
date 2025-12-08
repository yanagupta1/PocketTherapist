package com.example.pockettherapist

import android.Manifest
import android.animation.AnimatorSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pockettherapist.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: JournalAdapter
    private var audioHelper: AudioTextHelper? = null

    private val micPermission = Manifest.permission.RECORD_AUDIO
    private val micRequestCode = 101

    private var journalEntries = mutableListOf<JournalEntry>()
    private var isOverlayVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = UserStore.getCurrentUsername()  // IMPORTANT

        // SHOW ONBOARDING ONE TIME FOR THIS USER ONLY
        if (FirstTimeStore.isFirstTime(requireContext(), username)) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, OnboardingFragment.newInstance(username))
                .commit()
            return
        }


        checkMicPermission()
        setupAudioHelper()
        setupRecyclerView()
        addSwipeToDelete()
        setupFab()
        setupOverlay()
        loadJournalEntries()
    }

    private fun checkMicPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), micPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(micPermission),
                micRequestCode
            )
        }
    }

    private fun setupAudioHelper() {
        audioHelper = AudioTextHelper(
            requireContext(),
            onResult = { text ->
                val current = binding.etNewEntry.text.toString()
                val appended = if (current.isEmpty()) text else "$current $text"
                binding.etNewEntry.setText(appended)
                binding.etNewEntry.setSelection(appended.length)
            },
            onError = { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupRecyclerView() {
        adapter = JournalAdapter()
        binding.journalRecycler.adapter = adapter
        binding.journalRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupFab() {
        binding.fabContainer.setOnClickListener {
            if (!isOverlayVisible) {
                showOverlay()
            }
        }

        // Hold-to-speak when overlay is visible (FAB becomes mic)
        binding.fabContainer.setOnTouchListener { _, event ->
            if (isOverlayVisible) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Gray background, blue icon when pressed
                        setFabColor(R.color.dark_gray)
                        binding.fabIcon.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.accent)
                        )
                        audioHelper?.startListening()
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Return to accent background, white icon
                        setFabColor(R.color.accent)
                        binding.fabIcon.setColorFilter(
                            ContextCompat.getColor(requireContext(), R.color.white)
                        )
                        audioHelper?.stopListening()
                        true
                        }
                    else -> false
                }
            } else {
                false // Let onClick handle it
            }
        }
    }

    private fun setFabColor(colorRes: Int) {
        val drawable = binding.fabContainer.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(requireContext(), colorRes))
    }

    private fun setupOverlay() {
        // Clicking dim overlay closes and saves
        binding.dimOverlay.setOnClickListener {
            if (isOverlayVisible) {
                hideOverlayAndSave()
            }
        }
    }

    private fun showOverlay() {
        isOverlayVisible = true
        binding.dimOverlay.visibility = View.VISIBLE

        // Set height to 70% of screen
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val params = binding.entryOverlay.layoutParams
        params.height = (screenHeight * 0.70).toInt()
        binding.entryOverlay.layoutParams = params

        binding.entryOverlay.visibility = View.VISIBLE

        // Animate plus to mic with full rotation (360 so icon stays upright)
        animateFabIcon(R.drawable.ic_mic)

        binding.etNewEntry.requestFocus()
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etNewEntry, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun animateFabIcon(newIconRes: Int) {
        // Scale down, change icon, scale back up
        val scaleDownX = ObjectAnimator.ofFloat(binding.fabIcon, "scaleX", 1f, 0f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.fabIcon, "scaleY", 1f, 0f)
        val rotate = ObjectAnimator.ofFloat(binding.fabIcon, "rotation", 0f, 90f)

        val scaleDown = AnimatorSet()
        scaleDown.playTogether(scaleDownX, scaleDownY, rotate)
        scaleDown.duration = 150
        scaleDown.interpolator = AccelerateDecelerateInterpolator()

        val scaleUpX = ObjectAnimator.ofFloat(binding.fabIcon, "scaleX", 0f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.fabIcon, "scaleY", 0f, 1f)
        val rotateBack = ObjectAnimator.ofFloat(binding.fabIcon, "rotation", 90f, 0f)

        val scaleUp = AnimatorSet()
        scaleUp.playTogether(scaleUpX, scaleUpY, rotateBack)
        scaleUp.duration = 150
        scaleUp.interpolator = AccelerateDecelerateInterpolator()

        scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.fabIcon.setImageResource(newIconRes)
            }
        })

        val animatorSet = AnimatorSet()
        animatorSet.playSequentially(scaleDown, scaleUp)
        animatorSet.start()
    }

    private fun hideOverlayAndSave() {
        val text = binding.etNewEntry.text.toString().trim()

        // Hide keyboard
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etNewEntry.windowToken, 0)

        // Hide overlay
        isOverlayVisible = false
        binding.dimOverlay.visibility = View.GONE
        binding.entryOverlay.visibility = View.GONE
        binding.etNewEntry.setText("")

        // Animate mic back to plus
        animateFabIcon(R.drawable.ic_plus)

        // Save if there's text
        if (text.isNotEmpty()) {
            saveJournalEntry(text)
        }
    }

    private fun loadJournalEntries() {
        UserStore.loadJournalEntries(
            onSuccess = { entries ->
                journalEntries = entries.toMutableList()
                updateList()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Failed to load entries: $error", Toast.LENGTH_SHORT).show()
                updateList()
            }
        )
    }

    private fun saveJournalEntry(text: String) {
        UserStore.saveJournalEntry(
            text = text,
            onSuccess = { entry ->
                journalEntries.add(0, entry)
                updateList()
                Toast.makeText(requireContext(), "Entry saved", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(requireContext(), "Failed to save: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateList() {
        val items = buildListItems(journalEntries)
        adapter.submitList(items)
    }

    private fun buildListItems(entries: List<JournalEntry>): List<JournalListItem> {
        val items = mutableListOf<JournalListItem>()

        if (entries.isEmpty()) {
            return items
        }

        // Group entries by date
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        var currentDateString = ""

        for (entry in entries) {
            val entryDate = Date(entry.timestamp)
            val entryCalendar = Calendar.getInstance().apply { time = entryDate }

            val dateString = when {
                isSameDay(entryCalendar, today) -> "Today"
                isSameDay(entryCalendar, yesterday) -> "Yesterday"
                else -> dateFormat.format(entryDate)
            }

            if (dateString != currentDateString) {
                currentDateString = dateString
                items.add(JournalListItem.DateHeader(dateString))
            }

            items.add(JournalListItem.Entry(entry))
        }

        return items
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onPause() {
        super.onPause()
        // Auto-save when leaving the fragment
        if (isOverlayVisible) {
            hideOverlayAndSave()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioHelper?.destroy()
        audioHelper = null
    }

    private fun addSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]

                if (item is JournalListItem.Entry) {

                    // 1. Remove from local list
                    journalEntries.remove(item.entry)
                    updateList()

                    // 2. Delete from UserStore (Firebase)
                    UserStore.deleteJournalEntry(
                        item.entry.id,
                        onSuccess = {},
                        onFailure = {
                            Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.journalRecycler)
    }

}
