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
You are a music therapist AI. Recommend 6-7 REAL Spotify songs based on the user's emotional state.

User Journal: "$journalText"
Emotion: ${emotionData.emotion} (${emotionData.emotionScore})
Sentiment: ${emotionData.sentiment} (${emotionData.sentimentScore})

Return ONLY valid JSON in this EXACT format (no markdown, no code blocks):
{
  "songs": [
    {"title": "Breathe Me", "artist": "Sia"},
    {"title": "Weightless", "artist": "Marconi Union"},
    {"title": "Clair de Lune", "artist": "Claude Debussy"}
  ],
  "mood": "Calming and soothing",
  "reasoning": "Why these songs help"
}

Guidelines by emotion:
- sadness: Fix You-Coldplay, Lean on Me-Bill Withers, The Scientist-Coldplay
- fear/anxiety: Weightless-Marconi Union, Clair de Lune-Debussy, Breathe Me-Sia
- anger: Smells Like Teen Spirit-Nirvana, Break Stuff-Limp Bizkit
- joy: Happy-Pharrell Williams, Walking on Sunshine-Katrina and the Waves
- love: Can't Help Falling in Love-Elvis Presley, At Last-Etta James
- surprise: Eye of the Tiger-Survivor, Don't Stop Believin'-Journey

Return ONLY the JSON object, nothing else.
        """.trimIndent()
    }

    private fun parseSongRecommendation(response: String): SongRecommendation {
        val jsonResponse = JSONObject(response)
        val songsArray = jsonResponse.getJSONArray("songs")

        val songs = mutableListOf<SongDetail>()
        for (i in 0 until songsArray.length()) {
            val songObj = songsArray.getJSONObject(i)
            val title = songObj.getString("title")
            val artist = songObj.getString("artist")

            songs.add(SongDetail(
                title = title,
                artist = artist,
                spotifySearchUrl = "https://open.spotify.com/search/${java.net.URLEncoder.encode("$title $artist", "UTF-8")}",
                spotifyUri = "spotify:search:${java.net.URLEncoder.encode("$title $artist", "UTF-8")}"
            ))
        }

        return SongRecommendation(
            songs = songs,
            mood = jsonResponse.getString("mood"),
            reasoning = jsonResponse.getString("reasoning"),
            spotifyPlaylistUrl = "",
            spotifyWebUrl = ""
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
You are a mental health resource assistant. Recommend resources for the user's location.

User Journal: "$journalText"
Emotion: ${emotionData.emotion} (${emotionData.emotionScore})
Sentiment: ${emotionData.sentiment} (${emotionData.sentimentScore})
Location: $location

Return ONLY valid JSON in this EXACT format:
{
  "resources": [
    {
      "name": "Crisis Connections",
      "type": "Crisis Line",
      "description": "24/7 crisis support",
      "phone": "866-427-4747",
      "website": "https://www.crisisconnections.org",
      "address": "123 Main St, City, State 12345",
      "latitude": 47.6062,
      "longitude": -122.3321
    }
  ],
  "emergencyContacts": [
    {
      "name": "988 Suicide & Crisis Lifeline",
      "number": "988",
      "description": "24/7 crisis support",
      "type": "both"
    }
  ],
  "reasoning": "Why these resources were recommended"
}

Include:
- Local crisis lines, therapy centers, support groups
- Google Maps coordinates (latitude/longitude)
- Emergency contacts: 988, Crisis Text Line
- Type: "call", "text", or "both"

Return ONLY the JSON object, nothing else.
        """.trimIndent()
    }

    private fun parseNearbyHelp(response: String): NearbyHelpResource {
        val jsonResponse = JSONObject(response)

        val resourcesArray = jsonResponse.getJSONArray("resources")
        val resources = mutableListOf<ResourceDetail>()
        for (i in 0 until resourcesArray.length()) {
            val resObj = resourcesArray.getJSONObject(i)
            val lat = if (resObj.has("latitude")) resObj.optDouble("latitude", Double.NaN) else null
            val lon = if (resObj.has("longitude")) resObj.optDouble("longitude", Double.NaN) else null
            val validLat = if (lat != null && !lat.isNaN()) lat else null
            val validLon = if (lon != null && !lon.isNaN()) lon else null

            resources.add(ResourceDetail(
                name = resObj.getString("name"),
                type = resObj.getString("type"),
                description = resObj.getString("description"),
                phone = resObj.optString("phone").takeIf { it.isNotEmpty() },
                website = resObj.optString("website").takeIf { it.isNotEmpty() },
                address = resObj.optString("address").takeIf { it.isNotEmpty() },
                googleMapsUrl = if (validLat != null && validLon != null) {
                    "https://www.google.com/maps/search/?api=1&query=$validLat,$validLon"
                } else null,
                latitude = validLat,
                longitude = validLon
            ))
        }

        val emergencyArray = jsonResponse.getJSONArray("emergencyContacts")
        val emergencyContacts = mutableListOf<EmergencyContact>()
        for (i in 0 until emergencyArray.length()) {
            val emObj = emergencyArray.getJSONObject(i)
            emergencyContacts.add(EmergencyContact(
                name = emObj.getString("name"),
                number = emObj.getString("number"),
                description = emObj.getString("description"),
                type = emObj.getString("type")
            ))
        }

        return NearbyHelpResource(
            resources = resources,
            emergencyContacts = emergencyContacts,
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
You are a wellness coach AI. Recommend detailed wellness techniques.

User Journal: "$journalText"
Emotion: ${emotionData.emotion} (${emotionData.emotionScore})
Sentiment: ${emotionData.sentiment} (${emotionData.sentimentScore})

Return ONLY valid JSON in this EXACT format:
{
  "techniques": [
    {
      "name": "Box Breathing",
      "category": "breathing",
      "duration": "2-5 min",
      "difficulty": "easy",
      "description": "A simple breathing technique to calm anxiety",
      "instructions": [
        "Sit comfortably with your back straight",
        "Inhale slowly through your nose for 4 counts",
        "Hold your breath for 4 counts",
        "Exhale slowly through your mouth for 4 counts",
        "Hold empty lungs for 4 counts",
        "Repeat 5-10 times"
      ],
      "benefits": [
        "Activates parasympathetic nervous system",
        "Reduces heart rate and blood pressure",
        "Immediate anxiety relief"
      ],
      "tips": [
        "Count slowly and steadily",
        "Focus on the sensation of breathing"
      ]
    }
  ],
  "reasoning": "Why these techniques were selected"
}

Recommend 5-7 techniques from categories:
- breathing: Box Breathing, 4-7-8 Breathing, Diaphragmatic Breathing
- mindfulness: 5-4-3-2-1 Grounding, Body Scan, Mindful Walking
- movement: Gentle Stretching, Progressive Muscle Relaxation, Yoga Poses
- cognitive: Gratitude Journaling, Positive Affirmations, Thought Challenging

Each technique MUST include:
- name, category, duration, difficulty
- description (1 sentence)
- instructions (step-by-step list)
- benefits (3-5 points)
- tips (optional, 2-4 practical tips)

Return ONLY the JSON object, nothing else.
        """.trimIndent()
    }

    private fun parseWellnessSuggestions(response: String): WellnessSuggestion {
        val jsonResponse = JSONObject(response)
        val techniquesArray = jsonResponse.getJSONArray("techniques")

        val techniques = mutableListOf<WellnessTechnique>()
        for (i in 0 until techniquesArray.length()) {
            val techObj = techniquesArray.getJSONObject(i)

            techniques.add(WellnessTechnique(
                name = techObj.getString("name"),
                category = techObj.getString("category"),
                duration = techObj.getString("duration"),
                difficulty = techObj.getString("difficulty"),
                description = techObj.getString("description"),
                instructions = jsonArrayToList(techObj.getJSONArray("instructions")),
                benefits = jsonArrayToList(techObj.getJSONArray("benefits")),
                tips = if (techObj.has("tips")) {
                    jsonArrayToList(techObj.getJSONArray("tips"))
                } else null
            ))
        }

        return WellnessSuggestion(
            techniques = techniques,
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
