package com.example.pockettherapist

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * RecommendationEngineV2 - Enhanced version with:
 * 1. Detailed wellness technique descriptions
 * 2. Spotify playlist integration + deep links
 * 3. Google Maps integration for resources
 * 4. Fully structured JSON output
 */
class RecommendationEngineV2(private val context: Context) {

    private val TAG = "RecommendationEngineV2"
    private val GEMINI_API_KEY = "AIzaSyCC5SCVEfG62IX5jK00t3hGTP4Z0QIynQg"
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

    // ==================== DATA CLASSES ====================

    data class EmotionData(
        val emotion: String,
        val emotionScore: Float,
        val sentiment: String,
        val sentimentScore: Float
    )

    data class SongRecommendation(
        val songs: List<SongDetail>,
        val mood: String,
        val reasoning: String,
        val spotifyPlaylistUrl: String,  // Opens in Spotify app
        val spotifyWebUrl: String         // Opens in web browser
    )

    data class SongDetail(
        val title: String,
        val artist: String,
        val spotifySearchUrl: String,
        val spotifyUri: String
    )

    data class NearbyHelpResource(
        val resources: List<ResourceDetail>,
        val emergencyContacts: List<EmergencyContact>,
        val reasoning: String
    )

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

    data class EmergencyContact(
        val name: String,
        val number: String,
        val description: String,
        val type: String  // "call", "text", or "both"
    )

    data class WellnessSuggestion(
        val techniques: List<WellnessTechnique>,
        val reasoning: String
    )

    data class WellnessTechnique(
        val name: String,
        val category: String,
        val duration: String,
        val difficulty: String,
        val description: String,
        val instructions: List<String>,
        val benefits: List<String>,
        val tips: List<String>?
    )

    // ==================== 1. SONG RECOMMENDATIONS ====================

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
                spotifySearchUrl = createSpotifySearchUrl(title, artist),
                spotifyUri = createSpotifyUri(title, artist)
            ))
        }

        val mood = jsonResponse.getString("mood")
        val reasoning = jsonResponse.getString("reasoning")

        return SongRecommendation(
            songs = songs,
            mood = mood,
            reasoning = reasoning,
            spotifyPlaylistUrl = createSpotifyPlaylistUrl(songs),
            spotifyWebUrl = createSpotifyWebPlaylistUrl(songs)
        )
    }

    private fun createSpotifySearchUrl(title: String, artist: String): String {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        return "https://open.spotify.com/search/$query"
    }

    private fun createSpotifyUri(title: String, artist: String): String {
        // Spotify URI format: spotify:search:query
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        return "spotify:search:$query"
    }

    private fun createSpotifyPlaylistUrl(songs: List<SongDetail>): String {
        // Creates a search URL for all songs combined
        val allSongs = songs.joinToString(" ") { "${it.title} ${it.artist}" }
        val query = URLEncoder.encode(allSongs, "UTF-8")
        return "spotify:search:$query"
    }

    private fun createSpotifyWebPlaylistUrl(songs: List<SongDetail>): String {
        // Web URL to search for songs
        val query = songs.joinToString(", ") { "${it.title} - ${it.artist}" }
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://open.spotify.com/search/$encoded"
    }

    // ==================== 2. NEARBY HELP ====================

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

    private fun buildHelpPrompt(journalText: String, emotionData: EmotionData, location: String): String {
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
            val lat = if (resObj.has("latitude")) resObj.getDouble("latitude") else null
            val lon = if (resObj.has("longitude")) resObj.getDouble("longitude") else null

            resources.add(ResourceDetail(
                name = resObj.getString("name"),
                type = resObj.getString("type"),
                description = resObj.getString("description"),
                phone = resObj.optString("phone").takeIf { it.isNotEmpty() },
                website = resObj.optString("website").takeIf { it.isNotEmpty() },
                address = resObj.optString("address").takeIf { it.isNotEmpty() },
                googleMapsUrl = if (lat != null && lon != null) {
                    createGoogleMapsUrl(lat, lon, resObj.getString("name"))
                } else null,
                latitude = lat,
                longitude = lon
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

    private fun createGoogleMapsUrl(lat: Double, lon: Double, placeName: String): String {
        val query = URLEncoder.encode(placeName, "UTF-8")
        return "https://www.google.com/maps/search/?api=1&query=$lat,$lon&query=$query"
    }

    // ==================== 3. WELLNESS SUGGESTIONS ====================

    suspend fun getWellnessSuggestions(
        journalText: String,
        emotionData: EmotionData
    ): WellnessSuggestion? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = buildWellnessPrompt(journalText, emotionData)
                val response = callGeminiAPI(prompt)
                parseWellnessSuggestions(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting wellness suggestions", e)
                null
            }
        }
    }

    private fun buildWellnessPrompt(journalText: String, emotionData: EmotionData): String {
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
        "Improves focus and mental clarity",
        "Immediate anxiety relief"
      ],
      "tips": [
        "Count slowly and steadily",
        "Focus on the sensation of breathing",
        "Don't force the breath",
        "Practice regularly for best results"
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
- benefits (3-5 points why it helps)
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

    private suspend fun callGeminiAPI(prompt: String): String {
        return withContext(Dispatchers.IO) {
            val url = URL(GEMINI_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

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

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

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

    private fun extractTextFromGeminiResponse(apiResponse: String): String {
        val jsonResponse = JSONObject(apiResponse)
        val candidates = jsonResponse.getJSONArray("candidates")
        if (candidates.length() > 0) {
            val candidate = candidates.getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            if (parts.length() > 0) {
                val text = parts.getJSONObject(0).getString("text")
                return extractJSON(text)
            }
        }
        throw Exception("No valid response from Gemini API")
    }

    private fun extractJSON(text: String): String {
        // Remove markdown code blocks if present
        val cleaned = text.replace("```json", "").replace("```", "").trim()

        // Try to extract JSON object
        val jsonPattern = Regex("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}")
        val match = jsonPattern.find(cleaned)

        return match?.value ?: cleaned
    }

    private fun jsonArrayToList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }

    companion object {
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
