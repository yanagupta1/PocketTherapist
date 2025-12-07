package com.example.pockettherapist.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pockettherapist.databinding.ItemEventBinding
import com.example.pockettherapist.api.EventbriteEvent

class EventAdapter(private val items: List<EventbriteEvent>) :
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val item = items[position]

        holder.binding.eventTitle.text = item.name.text ?: "Unnamed Event"

        // Optional: show date/time
        // holder.binding.eventDate.text = item.start.local
    }

    override fun getItemCount() = items.size
}
