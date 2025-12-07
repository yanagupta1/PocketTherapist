package com.example.pockettherapist

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pockettherapist.adapters.AmenityAdapter
import com.example.pockettherapist.adapters.TMEventAdapter
import com.example.pockettherapist.api.OSMResponse
import com.example.pockettherapist.api.RetrofitClient
import com.example.pockettherapist.api.TicketmasterResponse
import com.example.pockettherapist.databinding.FragmentNearbyBinding
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NearbyFragment : Fragment() {

    private lateinit var binding: FragmentNearbyBinding

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadUserLocation()
            else Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNearbyBinding.inflate(inflater, container, false)

        binding.eventsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.amenitiesRecycler.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermission()
    }

    // -------------------------------------------------
    // PERMISSION HANDLING
    // -------------------------------------------------

    private fun requestPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = ContextCompat.checkSelfPermission(requireContext(), permission)

        if (granted == PackageManager.PERMISSION_GRANTED) {
            loadUserLocation()
        } else {
            permissionLauncher.launch(permission)
        }
    }



    @SuppressLint("MissingPermission")
    private fun loadUserLocation() {
        binding.loadingSpinner.visibility = View.VISIBLE

        val fused = LocationServices.getFusedLocationProviderClient(requireActivity())

        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    loadTicketmasterEvents(loc.latitude, loc.longitude)
                    loadOSM(loc.latitude, loc.longitude)
                } else {
                    // fallback: Madison
                    loadTicketmasterEvents(43.0731, -89.4012)
                    loadOSM(43.0731, -89.4012)
                }
            }
            .addOnFailureListener {
                loadTicketmasterEvents(43.0731, -89.4012)
                loadOSM(43.0731, -89.4012)
            }
    }

    // -------------------------------------------------
    // TICKETMASTER EVENTS
    // -------------------------------------------------

    private fun loadTicketmasterEvents(lat: Double, lng: Double) {

        val apiKey = "0ooZTY3A5ENbdIT4hAFlbVvbpBAOx5MM"  // ← replace!
        val latlong = "$lat,$lng"

        RetrofitClient.ticketmaster.getEvents(
            apiKey = apiKey,
            latlong = latlong,
            radius = 10
        ).enqueue(object : Callback<TicketmasterResponse> {

            override fun onResponse(
                call: Call<TicketmasterResponse>,
                response: Response<TicketmasterResponse>
            ) {
                if (!response.isSuccessful) {
                    println("TM error: ${response.code()}")
                    binding.eventsRecycler.adapter = TMEventAdapter(emptyList())
                    return
                }

                val events = response.body()?._embedded?.events ?: emptyList()

                println("Loaded Ticketmaster events: ${events.size}")

                binding.eventsRecycler.adapter = TMEventAdapter(events)
            }

            override fun onFailure(call: Call<TicketmasterResponse>, t: Throwable) {
                println("Ticketmaster failure: ${t.message}")
            }
        })
    }

    // -------------------------------------------------
    // OSM AMENITIES
    // -------------------------------------------------

    private fun loadOSM(lat: Double, lng: Double) {
        val radius = 3000

        val query = """
            [out:json];
            (
              node["amenity"="hospital"](around:$radius,$lat,$lng);
              node["amenity"="clinic"](around:$radius,$lat,$lng);
              node["amenity"="gym"](around:$radius,$lat,$lng);
              node["amenity"="spa"](around:$radius,$lat,$lng);
              node["leisure"="fitness_centre"](around:$radius,$lat,$lng);
            );
            out;
        """.trimIndent()

        RetrofitClient.osm.getAmenities(query)
            .enqueue(object : Callback<OSMResponse> {

                override fun onResponse(
                    call: Call<OSMResponse>,
                    response: Response<OSMResponse>
                ) {
                    val amenities = response.body()?.elements ?: emptyList()

                    binding.amenitiesRecycler.adapter = AmenityAdapter(amenities)
                    binding.loadingSpinner.visibility = View.GONE
                }

                override fun onFailure(call: Call<OSMResponse>, t: Throwable) {
                    binding.loadingSpinner.visibility = View.GONE
                    println("OSM failure: ${t.message}")
                }
            })
    }
}
