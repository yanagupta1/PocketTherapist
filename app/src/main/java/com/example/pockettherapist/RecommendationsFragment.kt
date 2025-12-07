package com.example.pockettherapist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.pockettherapist.databinding.FragmentRecommendationsBinding
import kotlinx.coroutines.launch

class RecommendationsFragment : Fragment() {

    private lateinit var binding: FragmentRecommendationsBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var recommendationEngine: RecommendationEngineV2
    private lateinit var spotifyIntegration: SpotifyIntegration
    private lateinit var emotionPredictor: EmotionModelPredictor
    private lateinit var sentimentPredictor: SentimentModelPredictor

    private val TAG = "RecommendationsFragment"
    private var currentJournalId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecommendationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize cache
        RecommendationsCache.init(requireContext())

        // Initialize services
        recommendationEngine = RecommendationEngineV2(requireContext())
        spotifyIntegration = SpotifyIntegration(requireContext())
        emotionPredictor = EmotionModelPredictor(requireContext())
        sentimentPredictor = SentimentModelPredictor(requireContext())

        // Setup RecyclerView with 2-column grid
        songAdapter = SongAdapter()
        binding.recyclerSongs.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = songAdapter
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setColorSchemeResources(
            R.color.accent_orange,
            R.color.accent_red,
            R.color.accent_purple
        )
        binding.swipeRefresh.setOnRefreshListener {
            // Force refresh - bypass cache
            loadRecommendations(forceRefresh = true)
        }

        // Load recommendations (use cache if valid)
        loadRecommendations(forceRefresh = false)
    }

    private fun loadRecommendations(forceRefresh: Boolean) {
        // Show loading states
        binding.progressWellness.visibility = View.VISIBLE
        binding.txtWellnessTitle.text = getString(R.string.loading)
        binding.txtWellnessDescription.visibility = View.GONE
        binding.layoutSongsLoading.visibility = View.VISIBLE
        binding.recyclerSongs.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE

        // Load most recent journal entry
        UserStore.loadJournalEntries(
            onSuccess = { entries ->
                if (entries.isEmpty()) {
                    showEmptyState()
                    binding.swipeRefresh.isRefreshing = false
                    return@loadJournalEntries
                }

                // Get most recent entry
                val mostRecent = entries.first()
                currentJournalId = mostRecent.id

                // Check cache validity
                if (!forceRefresh && RecommendationsCache.isCacheValid(mostRecent.id)) {
                    // Use cached data
                    Log.d(TAG, "Using cached recommendations")
                    displayCachedData()
                    binding.swipeRefresh.isRefreshing = false
                } else {
                    // Fetch fresh recommendations
                    Log.d(TAG, "Fetching fresh recommendations")
                    processJournalEntry(mostRecent.id, mostRecent.text)
                }
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to load journal entries: $error")
                showEmptyState()
                binding.swipeRefresh.isRefreshing = false
            }
        )
    }

    private fun displayCachedData() {
        activity?.runOnUiThread {
            // Display cached wellness
            val wellnessTitle = RecommendationsCache.getWellnessTitle()
            val wellnessDescription = RecommendationsCache.getWellnessDescription()

            binding.progressWellness.visibility = View.GONE
            if (!wellnessTitle.isNullOrEmpty()) {
                binding.txtWellnessTitle.text = wellnessTitle
                if (!wellnessDescription.isNullOrEmpty()) {
                    binding.txtWellnessDescription.text = wellnessDescription
                    binding.txtWellnessDescription.visibility = View.VISIBLE
                }
            } else {
                binding.txtWellnessTitle.text = getString(R.string.no_recommendations)
            }

            // Display cached songs
            val cachedSongs = RecommendationsCache.getSongs()
            binding.layoutSongsLoading.visibility = View.GONE

            if (cachedSongs != null && cachedSongs.isNotEmpty()) {
                binding.recyclerSongs.visibility = View.VISIBLE
                binding.txtMusicHeader.visibility = View.VISIBLE
                songAdapter.updateSongs(cachedSongs)
            } else {
                binding.txtMusicHeader.visibility = View.GONE
            }
        }
    }

    private fun processJournalEntry(journalId: String, journalText: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Analyze emotion and sentiment
                val emotionResult = emotionPredictor.predictEmotion(journalText)
                val sentimentResult = sentimentPredictor.predictSentiment(journalText)

                if (emotionResult == null || sentimentResult == null) {
                    showError()
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }

                // Create emotion data for recommendation engine
                val emotionData = RecommendationEngineV2.createEmotionData(
                    emotion = emotionResult.emotion,
                    emotionScore = emotionResult.confidence,
                    sentiment = sentimentResult.sentiment,
                    sentimentScore = sentimentResult.confidence
                )

                // Fetch wellness suggestions and song recommendations in parallel
                launch { loadWellnessSuggestion(journalId, journalText, emotionData) }
                launch { loadSongRecommendations(journalText, emotionData) }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing journal entry", e)
                showError()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun loadWellnessSuggestion(
        journalId: String,
        journalText: String,
        emotionData: RecommendationEngineV2.EmotionData
    ) {
        try {
            val wellness = recommendationEngine.getWellnessSuggestions(journalText, emotionData)

            activity?.runOnUiThread {
                binding.progressWellness.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (wellness != null && wellness.techniques.isNotEmpty()) {
                    val technique = wellness.techniques.first()
                    binding.txtWellnessTitle.text = technique.name
                    binding.txtWellnessDescription.text = technique.description
                    binding.txtWellnessDescription.visibility = View.VISIBLE

                    // Cache the wellness recommendation
                    RecommendationsCache.saveWellness(journalId, technique.name, technique.description)
                } else {
                    binding.txtWellnessTitle.text = getString(R.string.no_recommendations)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wellness suggestion", e)
            activity?.runOnUiThread {
                binding.progressWellness.visibility = View.GONE
                binding.txtWellnessTitle.text = getString(R.string.no_recommendations)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private suspend fun loadSongRecommendations(
        journalText: String,
        emotionData: RecommendationEngineV2.EmotionData
    ) {
        try {
            // Get song recommendations from Gemini
            val songRecommendation = recommendationEngine.getSongRecommendations(journalText, emotionData)

            if (songRecommendation == null || songRecommendation.songs.isEmpty()) {
                activity?.runOnUiThread {
                    binding.layoutSongsLoading.visibility = View.GONE
                    binding.txtMusicHeader.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
                return
            }

            // Search for each song on Spotify to get album art
            val spotifyTracks = mutableListOf<SpotifyIntegration.SpotifyTrack>()
            for (song in songRecommendation.songs) {
                val track = spotifyIntegration.searchTrack(song.title, song.artist)
                if (track != null) {
                    spotifyTracks.add(track)
                }
            }

            activity?.runOnUiThread {
                binding.layoutSongsLoading.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (spotifyTracks.isNotEmpty()) {
                    binding.recyclerSongs.visibility = View.VISIBLE
                    binding.txtMusicHeader.visibility = View.VISIBLE
                    songAdapter.updateSongs(spotifyTracks)

                    // Cache the songs
                    RecommendationsCache.saveSongs(spotifyTracks)
                } else {
                    binding.txtMusicHeader.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading song recommendations", e)
            activity?.runOnUiThread {
                binding.layoutSongsLoading.visibility = View.GONE
                binding.txtMusicHeader.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showEmptyState() {
        activity?.runOnUiThread {
            binding.progressWellness.visibility = View.GONE
            binding.txtWellnessTitle.text = getString(R.string.no_journal_for_recommendations)
            binding.layoutSongsLoading.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.txtMusicHeader.visibility = View.GONE
        }
    }

    private fun showError() {
        activity?.runOnUiThread {
            binding.progressWellness.visibility = View.GONE
            binding.txtWellnessTitle.text = getString(R.string.no_recommendations)
            binding.layoutSongsLoading.visibility = View.GONE
        }
    }
}
