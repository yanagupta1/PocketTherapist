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
 * RecommendationEngine - Enhanced version with:
 * 1. Detailed wellness technique descriptions
 * 2. Spotify playlist integration + deep links
 * 3. Google Maps integration for resources
 * 4. Fully structured JSON output
 */
class RecommendationEngine(private val context: Context) {

    private val TAG = "RecommendationEngine"
    private val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    private val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$GEMINI_API_KEY"

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

    data class EventRecommendation(
        val events: List<EventDetail>,
        val reasoning: String
    )

    data class EventDetail(
        val name: String,
        val date: String,
        val time: String?,
        val venue: String,
        val address: String?,
        val description: String,
        val category: String,
        val url: String?,
        val latitude: Double?,
        val longitude: Double?,
        val googleMapsUrl: String?
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
                Log.d(TAG, "Getting nearby help for location: $location")
                val prompt = buildHelpPrompt(journalText, emotionData, location)
                val response = callGeminiAPI(prompt)
                Log.d(TAG, "Gemini API response received for amenities")
                parseNearbyHelp(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting nearby help for location: $location", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun buildHelpPrompt(journalText: String, emotionData: EmotionData, location: String): String {
        return """
You are a mental health resource assistant. Recommend 3-5 resources for the user's location.

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
            Log.d(TAG, "Calling Gemini API...")
            val url = URL(GEMINI_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30000  // 30 seconds
                connection.readTimeout = 30000     // 30 seconds

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

                Log.d(TAG, "Sending request to Gemini API...")
                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Gemini API response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Gemini API response: ${response.take(200)}...")
                    extractTextFromGeminiResponse(response)
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e(TAG, "Gemini API error response: $errorStream")
                    throw Exception("API Error: $responseCode - $errorStream")
                }
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "No internet connection - UnknownHostException", e)
                throw Exception("No internet connection. Please check your network settings.")
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Request timeout - slow or no internet", e)
                throw Exception("Request timeout. Please check your internet connection.")
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Network error - IOException", e)
                throw Exception("Network error: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception in callGeminiAPI", e)
                throw e
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

    // ==================== 4. EVENT RECOMMENDATIONS ====================

    suspend fun getEventRecommendations(
        location: String,
        emotionData: EmotionData? = null
    ): EventRecommendation? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting event recommendations for location: $location")
                val prompt = buildEventPrompt(location, emotionData)
                val response = callGeminiAPI(prompt)
                Log.d(TAG, "Gemini API response received for events")
                parseEventRecommendations(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting event recommendations for location: $location", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun buildEventPrompt(location: String, emotionData: EmotionData?): String {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

        return """
You are an event recommendation AI. Recommend 3-5 REAL upcoming wellness, mental health, or relaxation events in the user's location.

Location: $location
Current Date: $today
${emotionData?.let { "User Emotion: ${it.emotion} (${it.emotionScore})" } ?: ""}

Return ONLY valid JSON in this EXACT format (no markdown, no code blocks):
{
  "events": [
    {
      "name": "Mindfulness Meditation Workshop",
      "date": "2025-12-15",
      "time": "18:00",
      "venue": "Community Wellness Center",
      "address": "123 Main St, Portland, OR 97201",
      "description": "Learn mindfulness meditation techniques for stress relief",
      "category": "Wellness",
      "url": "https://example.com/event",
      "latitude": 45.5152,
      "longitude": -122.6784
    }
  ],
  "reasoning": "Why these events were selected"
}

Event Guidelines:
- Focus on: wellness workshops, meditation classes, yoga sessions, mental health seminars, support groups, nature walks, art therapy, music therapy, fitness classes
- Include ONLY events that are upcoming (after $today)
- Use REAL dates in YYYY-MM-DD format (not past dates!)
- Include specific venue names and addresses
- Add GPS coordinates (latitude/longitude) for each venue
- Provide event URLs if available
- Categories: Wellness, Mental Health, Fitness, Mindfulness, Social, Creative, Nature
- Make events appropriate for mental wellness and stress relief

IMPORTANT:
- All dates MUST be future dates (after $today)
- Use proper date format: YYYY-MM-DD
- Time in HH:MM format (24-hour)
- Include real addresses and coordinates for $location

Return ONLY the JSON object, nothing else.
        """.trimIndent()
    }

    private fun parseEventRecommendations(response: String): EventRecommendation {
        val jsonResponse = JSONObject(response)
        val eventsArray = jsonResponse.getJSONArray("events")

        val events = mutableListOf<EventDetail>()

        for (i in 0 until eventsArray.length()) {
            val eventObj = eventsArray.getJSONObject(i)
            val lat = if (eventObj.has("latitude")) eventObj.getDouble("latitude") else null
            val lon = if (eventObj.has("longitude")) eventObj.getDouble("longitude") else null

            events.add(EventDetail(
                name = eventObj.getString("name"),
                date = eventObj.getString("date"),
                time = eventObj.optString("time").takeIf { it.isNotEmpty() },
                venue = eventObj.getString("venue"),
                address = eventObj.optString("address").takeIf { it.isNotEmpty() },
                description = eventObj.getString("description"),
                category = eventObj.getString("category"),
                url = eventObj.optString("url").takeIf { it.isNotEmpty() },
                latitude = lat,
                longitude = lon,
                googleMapsUrl = if (lat != null && lon != null) {
                    createGoogleMapsUrl(lat, lon, eventObj.getString("venue"))
                } else null
            ))
        }

        return EventRecommendation(
            events = events,
            reasoning = jsonResponse.getString("reasoning")
        )
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
