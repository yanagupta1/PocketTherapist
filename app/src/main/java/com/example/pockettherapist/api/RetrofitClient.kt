package com.example.pockettherapist.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val EVENTBRITE_BASE_URL = "https://www.eventbriteapi.com/v3/"
    private const val OSM_BASE_URL = "https://overpass-api.de/api/"

    val eventbrite: EventbriteApi by lazy {
        Retrofit.Builder()
            .baseUrl(EVENTBRITE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventbriteApi::class.java)
    }

    val osm: OSMApi by lazy {
        Retrofit.Builder()
            .baseUrl(OSM_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OSMApi::class.java)
    }
}