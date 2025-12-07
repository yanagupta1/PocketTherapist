package com.example.pockettherapist

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Example usage of the Recommendation Engine with Emotion and Sentiment models
 *
 * This file demonstrates how to:
 * 1. Analyze text using Emotion and Sentiment models
 * 2. Get song recommendations based on emotional state
 * 3. Find nearby mental health resources
 * 4. Receive personalized wellness suggestions
 */
class RecommendationEngineExample(private val context: Context) {

    private val TAG = "RecommendationExample"

    // Initialize the models and recommendation engine
    private val emotionPredictor = EmotionModelPredictor(context)
    private val sentimentPredictor = SentimentModelPredictor(context)
    private val recommendationEngine = RecommendationEngine(context)

    /**
     * Complete example: Analyze journal entry and get all recommendations
     *
     * @param journalText The user's journal entry (from voice or text input)
     */
    fun analyzeAndRecommend(journalText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Starting analysis for: ${journalText.take(50)}...")

            // Step 1: Predict emotion from journal text
            val emotionResult = emotionPredictor.predictEmotion(journalText)
            if (emotionResult == null) {
                Log.e(TAG, "Failed to predict emotion")
                return@launch
            }

            Log.d(TAG, "Emotion: ${emotionResult.emotion} (${emotionResult.confidence})")
            Log.d(TAG, "Emotion emoji: ${emotionPredictor.getEmotionEmoji(emotionResult.emotion)}")
            Log.d(TAG, "All emotion scores: ${emotionResult.allScores}")

            // Step 2: Predict sentiment from journal text
            val sentimentResult = sentimentPredictor.predictSentiment(journalText)
            if (sentimentResult == null) {
                Log.e(TAG, "Failed to predict sentiment")
                return@launch
            }

            Log.d(TAG, "Sentiment: ${sentimentResult.sentiment} (${sentimentResult.confidence})")
            Log.d(TAG, "Sentiment emoji: ${sentimentPredictor.getSentimentEmoji(sentimentResult.sentiment)}")
            Log.d(TAG, "All sentiment scores: ${sentimentResult.allScores}")

            // Step 3: Create EmotionData for recommendation engine
            val emotionData = RecommendationEngine.createEmotionData(
                emotion = emotionResult.emotion,
                emotionScore = emotionResult.confidence,
                sentiment = sentimentResult.sentiment,
                sentimentScore = sentimentResult.confidence
            )

            // Step 4: Get Song Recommendations
            Log.d(TAG, "\n=== Getting Song Recommendations ===")
            val songRecs = recommendationEngine.getSongRecommendations(journalText, emotionData)
            if (songRecs != null) {
                Log.d(TAG, "Mood: ${songRecs.mood}")
                Log.d(TAG, "Reasoning: ${songRecs.reasoning}")
                Log.d(TAG, "Recommended Songs:")
                songRecs.songs.forEachIndexed { index, song ->
                    Log.d(TAG, "  ${index + 1}. ${song.title} - ${song.artist}")
                }
            } else {
                Log.e(TAG, "Failed to get song recommendations")
            }

            // Step 5: Get Nearby Help Resources
            Log.d(TAG, "\n=== Getting Nearby Help Resources ===")
            val helpResources = recommendationEngine.getNearbyHelp(
                journalText,
                emotionData,
                location = "USA" // Can be dynamically obtained from GPS
            )
            if (helpResources != null) {
                Log.d(TAG, "Reasoning: ${helpResources.reasoning}")
                Log.d(TAG, "Recommended Resources:")
                helpResources.resources.forEachIndexed { index, resource ->
                    Log.d(TAG, "  ${index + 1}. ${resource.name} (${resource.type})")
                    Log.d(TAG, "     ${resource.description}")
                }
                Log.d(TAG, "Emergency Contacts:")
                helpResources.emergencyContacts.forEach { contact ->
                    Log.d(TAG, "  - ${contact.name}: ${contact.number}")
                }
            } else {
                Log.e(TAG, "Failed to get help resources")
            }

            // Step 6: Get Wellness Suggestions
            Log.d(TAG, "\n=== Getting Wellness Suggestions ===")
            val wellnessSuggestions = recommendationEngine.getWellnessSuggestions(journalText, emotionData)
            if (wellnessSuggestions != null) {
                Log.d(TAG, "Reasoning: ${wellnessSuggestions.reasoning}")
                Log.d(TAG, "Wellness Techniques:")
                wellnessSuggestions.techniques.forEachIndexed { index, technique ->
                    Log.d(TAG, "  ${index + 1}. ${technique.name} (${technique.category}, ${technique.duration})")
                    Log.d(TAG, "     Description: ${technique.description}")
                    Log.d(TAG, "     Benefits: ${technique.benefits.joinToString(", ")}")
                }
            } else {
                Log.e(TAG, "Failed to get wellness suggestions")
            }
        }
    }

    /**
     * Example 1: User feeling anxious
     */
    fun exampleAnxiety() {
        val journalText = """
            I'm feeling really anxious today. Work has been overwhelming and I can't seem to
            focus on anything. My mind keeps racing with worries about deadlines and whether
            I'm doing a good job. I feel tense and on edge all the time.
        """.trimIndent()

        analyzeAndRecommend(journalText)
    }

    /**
     * Example 2: User feeling happy
     */
    fun exampleHappiness() {
        val journalText = """
            Had an amazing day today! Got promoted at work and celebrated with friends.
            Everything feels so positive right now. I'm grateful for all the wonderful
            people in my life and excited about the future.
        """.trimIndent()

        analyzeAndRecommend(journalText)
    }

    /**
     * Example 3: User feeling sad
     */
    fun exampleSadness() {
        val journalText = """
            Feeling really down today. I miss my family and feel lonely. Nothing seems
            to make me happy anymore. I've been crying a lot and just want to stay in bed.
            Everything feels hopeless.
        """.trimIndent()

        analyzeAndRecommend(journalText)
    }

    /**
     * Example 4: Simple integration with existing VoiceJournalFragment
     */
    fun integrateWithVoiceJournal(transcribedText: String) {
        // This shows how to integrate with the existing audio_text.kt functionality
        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "Analyzing voice journal entry...")

            // Get emotion and sentiment
            val emotion = emotionPredictor.predictEmotion(transcribedText)
            val sentiment = sentimentPredictor.predictSentiment(transcribedText)

            if (emotion != null && sentiment != null) {
                // Create emotion data
                val emotionData = RecommendationEngine.createEmotionData(
                    emotion.emotion,
                    emotion.confidence,
                    sentiment.sentiment,
                    sentiment.confidence
                )

                // Get only song recommendations (example)
                val songs = recommendationEngine.getSongRecommendations(transcribedText, emotionData)

                // Display results in UI
                displayResults(emotion, sentiment, songs)
            }
        }
    }

    /**
     * Display results in UI (placeholder)
     */
    private fun displayResults(
        emotion: EmotionModelPredictor.EmotionResult,
        sentiment: SentimentModelPredictor.SentimentResult,
        songs: RecommendationEngine.SongRecommendation?
    ) {
        // TODO: Update UI with results
        Log.d(TAG, "Display in UI:")
        Log.d(TAG, "  Emotion: ${emotion.emotion} ${emotionPredictor.getEmotionEmoji(emotion.emotion)}")
        Log.d(TAG, "  Sentiment: ${sentiment.sentiment} ${sentimentPredictor.getSentimentEmoji(sentiment.sentiment)}")
        if (songs != null) {
            Log.d(TAG, "  Recommended: ${songs.songs.take(3).joinToString(", ") { "${it.title} - ${it.artist}" }}")
        }
    }

    companion object {
        /**
         * Quick test function to verify all components work
         */
        fun runQuickTest(context: Context) {
            val example = RecommendationEngineExample(context)

            Log.d("QuickTest", "Running test examples...")

            // Test anxiety scenario
            example.exampleAnxiety()

            // You can also test other scenarios:
            // example.exampleHappiness()
            // example.exampleSadness()
        }
    }
}

/**
 * INTEGRATION INSTRUCTIONS:
 *
 * 1. Add Kotlin Coroutines to build.gradle:
 *    dependencies {
 *        implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
 *    }
 *
 * 2. Add Internet permission to AndroidManifest.xml:
 *    <uses-permission android:name="android.permission.INTERNET" />
 *
 * 3. To use in your Fragment/Activity:
 *
 *    val recommender = RecommendationEngineExample(requireContext())
 *    recommender.integrateWithVoiceJournal(transcribedText)
 *
 * 4. For the VoiceJournalFragment, you can add:
 *
 *    private val recommender by lazy {
 *        RecommendationEngineExample(requireContext())
 *    }
 *
 *    // Then in your onResult callback from AudioTextHelper:
 *    onResult = { transcribedText ->
 *        // Existing code...
 *        recommender.integrateWithVoiceJournal(transcribedText)
 *    }
 */
