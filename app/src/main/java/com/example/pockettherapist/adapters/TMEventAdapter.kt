package com.example.pockettherapist.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pockettherapist.api.TMEvent
import com.example.pockettherapist.databinding.ItemEventBinding

class TMEventAdapter(private val items: List<TMEvent>) :
    RecyclerView.Adapter<TMEventAdapter.TMEventViewHolder>() {

    inner class TMEventViewHolder(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TMEventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TMEventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TMEventViewHolder, position: Int) {
        val item = items[position]

        holder.binding.eventTitle.text = item.name ?: "Unnamed Event"

        val date = item.dates?.start?.localDate ?: "No date"
        val time = item.dates?.start?.localTime ?: ""
        holder.binding.eventDate.text = "$date $time"

        val venue = item._embedded?.venues?.firstOrNull()?.name ?: "Unknown venue"
        holder.binding.eventVenue.text = venue
    }

    override fun getItemCount() = items.size
}
