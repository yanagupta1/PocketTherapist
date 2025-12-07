package com.example.pockettherapist

import com.example.pockettherapist.api.RetrofitClient
import com.example.pockettherapist.api.EventbriteResponse
import com.example.pockettherapist.api.OSMResponse

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices

class NearbyFragment : Fragment() {

    private lateinit var eventsRecycler: RecyclerView
    private lateinit var amenitiesRecycler: RecyclerView
    private lateinit var loadingSpinner: ProgressBar

    // Ask for location permission using new AndroidX API
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                loadUserLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_nearby, container, false)

        eventsRecycler = root.findViewById(R.id.eventsRecycler)
        amenitiesRecycler = root.findViewById(R.id.amenitiesRecycler)
        loadingSpinner = root.findViewById(R.id.loadingSpinner)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), fine) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), coarse) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            loadUserLocation()
        } else {
            locationPermissionLauncher.launch(fine)
        }
    }

    private fun loadUserLocation() {

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission missing → request again or return
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        loadingSpinner.visibility = View.VISIBLE

        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude

                    loadEventbriteEvents(lat, lng)
                    loadOSMAmenities(lat, lng)
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
            }
    }


    // -------------------------
    // API CALL PLACEHOLDERS
    // -------------------------

    private fun loadEventbriteEvents(lat: Double, lng: Double) {

        val token = "Bearer YOUR_EVENTBRITE_API_KEY"

        RetrofitClient.eventbrite.getEvents(
            token = token,
            lat = lat,
            lng = lng
        ).enqueue(object : Callback<EventbriteResponse> {

            override fun onResponse(call: Call<EventbriteResponse>, response: Response<EventbriteResponse>) {
                if (response.isSuccessful) {
                    val events = response.body()?.events ?: emptyList()
                    println("Loaded EventBrite events: ${events.size}")

                    // TODO display in RecyclerView
                }
            }

            override fun onFailure(call: Call<EventbriteResponse>, t: Throwable) {
                println("EventBrite Error: ${t.localizedMessage}")
            }
        })
    }


    private fun loadOSMAmenities(lat: Double, lng: Double) {

        val radius = 3000 // meters

        val query = """
        [out:json];
        (
          node["amenity"="hospital"](around:$radius,$lat,$lng);
          node["amenity"="clinic"](around:$radius,$lat,$lng);
          node["amenity"="social_facility"](around:$radius,$lat,$lng);
          node["leisure"="fitness_centre"](around:$radius,$lat,$lng);
          node["amenity"="spa"](around:$radius,$lat,$lng);
          node["amenity"="gym"](around:$radius,$lat,$lng);
        );
        out;
    """.trimIndent()

        RetrofitClient.osm.getAmenities(query)
            .enqueue(object : Callback<OSMResponse> {

                override fun onResponse(call: Call<OSMResponse>, response: Response<OSMResponse>) {
                    if (response.isSuccessful) {
                        val amenities = response.body()?.elements ?: emptyList()
                        println("Loaded OSM amenities: ${amenities.size}")

                        // TODO update RecyclerView
                    }
                }

                override fun onFailure(call: Call<OSMResponse>, t: Throwable) {
                    println("OSM Error: ${t.localizedMessage}")
                }
            })
    }

}
