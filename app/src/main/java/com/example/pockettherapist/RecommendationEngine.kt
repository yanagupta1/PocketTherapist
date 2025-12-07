package com.example.pockettherapist

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * RecommendationEngine class that provides three types of AI-powered recommendations:
 * 1. Song Recommendations - based on user's emotional state
 * 2. Nearby Help - mental health resources and therapists
 * 3. General Suggestions - wellness tips and activities
 *
 * Uses Google Gemini API for generating context-aware recommendations
 */
class RecommendationEngine(private val context: Context) {

    private val TAG = "RecommendationEngine"
    private val GEMINI_API_KEY = "AIzaSyCC5SCVEfG62IX5jK00t3hGTP4Z0QIynQg"
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

    /**
     * Data class to hold emotion and sentiment analysis results
     */
    data class EmotionData(
        val emotion: String,          // Primary emotion: sadness, joy, love, anger, fear, surprise
        val emotionScore: Float,      // Confidence score for emotion (0-1)
        val sentiment: String,        // Sentiment: negative, neutral, positive
        val sentimentScore: Float     // Confidence score for sentiment (0-1)
    )

    /**
     * Data class for song recommendations
     */
    data class SongRecommendation(
        val songs: List<SongDetail>,
        val mood: String,
        val reasoning: String,
        val spotifyPlaylistUrl: String,
        val spotifyWebUrl: String
    )

    /**
     * Data class for individual song details
     */
    data class SongDetail(
        val title: String,
        val artist: String,
        val spotifySearchUrl: String,
        val spotifyUri: String
    )

    /**
     * Data class for nearby help resources
     */
    data class NearbyHelpResource(
        val resources: List<ResourceDetail>,
        val emergencyContacts: List<EmergencyContact>,
        val reasoning: String
    )

    /**
     * Data class for individual resource details
     */
    data class ResourceDetail(
        val name: String,
        val type: String,
        val description: String,
        val phone: String?,
        val website: String?,
        val address: String?,
        val googleMapsUrl: String?,
        val latitude: Double?,
        val longitude: Double?
    )

    /**
     * Data class for emergency contacts
     */
    data class EmergencyContact(
        val name: String,
        val number: String,
        val description: String,
        val type: String  // "call", "text", or "both"
    )

    /**
     * Data class for general suggestions
     */
    data class WellnessSuggestion(
        val techniques: List<WellnessTechnique>,
        val reasoning: String
    )

    /**
     * Data class for individual wellness technique
     */
    data class WellnessTechnique(
        val name: String,
        val category: String,  // "breathing", "mindfulness", "movement", "cognitive"
        val duration: String,  // "2-5 min", "5-10 min"
        val difficulty: String,  // "easy", "moderate"
        val description: String,
        val instructions: List<String>,  // Step-by-step instructions
        val benefits: List<String>,  // Why this helps
        val tips: List<String>?  // Optional tips for success
    )

    // ==================== 1. SONG RECOMMENDATIONS ====================

    /**
     * Generate song/music recommendations based on user's emotional state and journal text
     *
     * @param journalText The user's journal entry
     * @param emotionData Emotion and sentiment analysis results
     * @return SongRecommendation with curated songs and mood context
     */
    suspend fun getSongRecommendations(
        journalText: String,
        emotionData: EmotionData
    ): SongRecommendation? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildSongPrompt(journalText, emotionData)
                val response = callGeminiAPI(prompt)
                parseSongRecommendation(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting song recommendations", e)
                null
            }
        }
    }

    private fun buildSongPrompt(journalText: String, emotionData: EmotionData): String {
        return """
            You are a music therapist AI. Based on the user's journal entry and emotional state,
            recommend 6-7 REAL, EXISTING songs that are available on Spotify. Use actual song titles and artists.

            User's Journal Entry:
            "$journalText"

            Detected Emotion: ${emotionData.emotion} (confidence: ${emotionData.emotionScore})
            Sentiment: ${emotionData.sentiment} (confidence: ${emotionData.sentimentScore})

            IMPORTANT: Recommend ONLY real songs that exist on Spotify. Format each as "Song Title - Artist Name".

            Provide recommendations in the following JSON format:
            {
                "songs": ["Breathe Me - Sia", "Weightless - Marconi Union", ...],
                "mood": "Brief mood description",
                "reasoning": "Why these specific songs were recommended"
            }

            Guidelines based on emotion:
            - If sad: suggest empathetic yet uplifting songs (e.g., "Fix You - Coldplay", "Lean on Me - Bill Withers")
            - If anxious: suggest calming, ambient music (e.g., "Weightless - Marconi Union", "Clair de Lune - Debussy")
            - If angry: suggest cathartic rock/alternative (e.g., "Smells Like Teen Spirit - Nirvana", "Break Stuff - Limp Bizkit")
            - If joyful: suggest upbeat pop/dance (e.g., "Happy - Pharrell Williams", "Walking on Sunshine - Katrina and the Waves")
            - If fearful: suggest comforting, reassuring music (e.g., "Here Comes the Sun - The Beatles", "Don't Worry Be Happy - Bobby McFerrin")
            - If surprised: suggest energizing, dynamic music (e.g., "Eye of the Tiger - Survivor", "Don't Stop Believin' - Journey")

            Return ONLY valid JSON with REAL song titles that exist on Spotify, no additional text.
        """.trimIndent()
    }

    private fun parseSongRecommendation(response: String): SongRecommendation {
        val jsonResponse = JSONObject(response)
        return SongRecommendation(
            songs = jsonArrayToList(jsonResponse.getJSONArray("songs")),
            mood = jsonResponse.getString("mood"),
            reasoning = jsonResponse.getString("reasoning")
        )
    }

    // ==================== 2. NEARBY HELP ====================

    /**
     * Get nearby mental health resources and help based on user's emotional state
     *
     * @param journalText The user's journal entry
     * @param emotionData Emotion and sentiment analysis results
     * @param location Optional location information for localized resources
     * @return NearbyHelpResource with mental health resources and emergency contacts
     */
    suspend fun getNearbyHelp(
        journalText: String,
        emotionData: EmotionData,
        location: String = "general"
    ): NearbyHelpResource? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildHelpPrompt(journalText, emotionData, location)
                val response = callGeminiAPI(prompt)
                parseNearbyHelp(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting nearby help", e)
                null
            }
        }
    }

    private fun buildHelpPrompt(
        journalText: String,
        emotionData: EmotionData,
        location: String
    ): String {
        return """
            You are a mental health resource assistant. Based on the user's journal entry and emotional state,
            recommend appropriate mental health resources, support services, and emergency contacts.

            User's Journal Entry:
            "$journalText"

            Detected Emotion: ${emotionData.emotion} (confidence: ${emotionData.emotionScore})
            Sentiment: ${emotionData.sentiment} (confidence: ${emotionData.sentimentScore})
            Location Context: $location

            Provide recommendations in the following JSON format:
            {
                "resources": [
                    "Resource type or service 1",
                    "Resource type or service 2",
                    ...
                ],
                "emergencyContacts": [
                    "National Suicide Prevention Lifeline: 988",
                    "Crisis Text Line: Text HOME to 741741",
                    ...
                ],
                "reasoning": "Why these resources were recommended"
            }

            Consider:
            - If severe distress is detected, prioritize crisis hotlines
            - Suggest therapy types (CBT, DBT, EMDR) based on emotion
            - Include support groups for specific emotions (grief, anxiety, etc.)
            - Recommend self-help resources and apps
            - Include general mental health helplines

            Return ONLY valid JSON, no additional text.
        """.trimIndent()
    }

    private fun parseNearbyHelp(response: String): NearbyHelpResource {
        val jsonResponse = JSONObject(response)
        return NearbyHelpResource(
            resources = jsonArrayToList(jsonResponse.getJSONArray("resources")),
            emergencyContacts = jsonArrayToList(jsonResponse.getJSONArray("emergencyContacts")),
            reasoning = jsonResponse.getString("reasoning")
        )
    }

    // ==================== 3. GENERAL SUGGESTIONS ====================

    /**
     * Get general wellness suggestions and activities based on emotional state
     *
     * @param journalText The user's journal entry
     * @param emotionData Emotion and sentiment analysis results
     * @return WellnessSuggestion with personalized wellness activities and tips
     */
    suspend fun getWellnessSuggestions(
        journalText: String,
        emotionData: EmotionData
    ): WellnessSuggestion? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildSuggestionsPrompt(journalText, emotionData)
                val response = callGeminiAPI(prompt)
                parseWellnessSuggestions(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting wellness suggestions", e)
                null
            }
        }
    }

    private fun buildSuggestionsPrompt(journalText: String, emotionData: EmotionData): String {
        return """
            You are a wellness coach AI. Based on the user's journal entry and emotional state,
            suggest practical wellness activities, coping strategies, and micro-interventions.

            User's Journal Entry:
            "$journalText"

            Detected Emotion: ${emotionData.emotion} (confidence: ${emotionData.emotionScore})
            Sentiment: ${emotionData.sentiment} (confidence: ${emotionData.sentimentScore})

            Provide recommendations in the following JSON format:
            {
                "suggestions": [
                    "Practical tip or coping strategy 1",
                    "Practical tip or coping strategy 2",
                    ...
                ],
                "activities": [
                    "Specific activity 1 (2-5 min)",
                    "Specific activity 2 (5-10 min)",
                    ...
                ],
                "reasoning": "Why these suggestions were recommended"
            }

            Focus on:
            - Micro-interventions (2-10 minutes)
            - Evidence-based coping strategies
            - Breathing exercises for anxiety/stress
            - Grounding techniques for fear/panic
            - Physical activities for anger/frustration
            - Gratitude exercises for sadness
            - Energy-boosting activities for low mood
            - Creative outlets for emotional expression

            Make suggestions specific, actionable, and brief.
            Return ONLY valid JSON, no additional text.
        """.trimIndent()
    }

    private fun parseWellnessSuggestions(response: String): WellnessSuggestion {
        val jsonResponse = JSONObject(response)
        return WellnessSuggestion(
            suggestions = jsonArrayToList(jsonResponse.getJSONArray("suggestions")),
            activities = jsonArrayToList(jsonResponse.getJSONArray("activities")),
            reasoning = jsonResponse.getString("reasoning")
        )
    }

    // ==================== HELPER FUNCTIONS ====================

    /**
     * Call Google Gemini API with the given prompt
     */
    private suspend fun callGeminiAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL(GEMINI_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Build request body for Gemini API
                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                // Send request
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                // Read response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    extractTextFromGeminiResponse(response)
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    throw Exception("API Error: $responseCode - $errorStream")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * Extract text content from Gemini API response
     */
    private fun extractTextFromGeminiResponse(apiResponse: String): String {
        val jsonResponse = JSONObject(apiResponse)
        val candidates = jsonResponse.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                val text = parts.getJSONObject(0).getString("text")
                // Extract JSON from markdown code blocks if present
                return extractJSON(text)
            }
        }
        throw Exception("No valid response from Gemini API")
    }

    /**
     * Extract JSON from text that might contain markdown code blocks
     */
    private fun extractJSON(text: String): String {
        val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```|```\\s*([\\s\\S]*?)\\s*```|\\{[\\s\\S]*\\}")
        val match = jsonPattern.find(text)
        return match?.groupValues?.firstOrNull { it.trim().startsWith("{") } ?: text.trim()
    }

    /**
     * Convert JSONArray to List<String>
     */
    private fun jsonArrayToList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    companion object {
        /**
         * Convenience method to create EmotionData from model outputs
         */
        fun createEmotionData(
            emotion: String,
            emotionScore: Float,
            sentiment: String,
            sentimentScore: Float
        ): EmotionData {
            return EmotionData(emotion, emotionScore, sentiment, sentimentScore)
        }
    }
}
