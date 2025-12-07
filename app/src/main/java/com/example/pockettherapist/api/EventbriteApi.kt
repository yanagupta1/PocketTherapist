package com.example.pockettherapist.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface EventbriteApi {

    @GET("events/search/")
    fun getEvents(
        @Header("Authorization") token: String,
        @Query("q") query: String = "wellness",
        @Query("location.latitude") lat: Double,
        @Query("location.longitude") lng: Double,
        @Query("location.within") within: String = "10km"
    ): Call<EventbriteResponse>

    @GET("venues/")
    fun getVenue(
        @Header("Authorization") token: String,
        @Query("venue_id") venueId: String
    ): Call<EventbriteVenue>
}