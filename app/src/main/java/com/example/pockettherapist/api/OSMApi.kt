package com.example.pockettherapist.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OSMApi {

    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("interpreter")
    fun getAmenities(
        @Body query: String
    ): Call<OSMResponse>
}