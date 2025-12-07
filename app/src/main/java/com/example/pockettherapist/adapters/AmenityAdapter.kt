package com.example.pockettherapist.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pockettherapist.databinding.ItemAmenityBinding
import com.example.pockettherapist.api.OSMElement

class AmenityAdapter(private val items: List<OSMElement>) :
    RecyclerView.Adapter<AmenityAdapter.AmenityViewHolder>() {

    inner class AmenityViewHolder(val binding: ItemAmenityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmenityViewHolder {
        val binding = ItemAmenityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AmenityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmenityViewHolder, position: Int) {
        val item = items[position]
        val name = item.tags?.get("name")
            ?: item.tags?.values?.firstOrNull()
            ?: "Unknown Place"

        holder.binding.amenityName.text = name
    }

    override fun getItemCount() = items.size
}
