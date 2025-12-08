package com.example.pockettherapist

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class JournalListItem {
    data class DateHeader(val date: String) : JournalListItem()
    data class Entry(val entry: JournalEntry) : JournalListItem()
}

class JournalAdapter : ListAdapter<JournalListItem, RecyclerView.ViewHolder>(JournalDiffCallback()) {

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_ENTRY = 1
        private const val PAYLOAD_EXPAND_CHANGE = "expand_change"
    }

    private var expandedEntryId: String? = null

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is JournalListItem.DateHeader -> TYPE_DATE_HEADER
            is JournalListItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE_HEADER -> DateHeaderViewHolder(
                inflater.inflate(R.layout.item_date_header, parent, false)
            )
            TYPE_ENTRY -> EntryViewHolder(
                inflater.inflate(R.layout.item_journal_entry, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is JournalListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is JournalListItem.Entry -> (holder as EntryViewHolder).bind(item.entry)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == PAYLOAD_EXPAND_CHANGE) {
            // Only update expansion state without full rebind
            val item = getItem(position)
            if (item is JournalListItem.Entry && holder is EntryViewHolder) {
                holder.updateExpansion(item.entry.id)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtDate: TextView = itemView.findViewById(R.id.txtDateHeader)

        fun bind(date: String) {
            txtDate.text = date
        }
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPreview: TextView = itemView.findViewById(R.id.txtEntryPreview)
        private val txtTime: TextView = itemView.findViewById(R.id.txtEntryTime)
        private var currentEntryId: String? = null

        fun bind(entry: JournalEntry) {
            currentEntryId = entry.id
            val isExpanded = expandedEntryId == entry.id

            txtPreview.text = entry.text
            txtPreview.maxLines = if (isExpanded) Integer.MAX_VALUE else 3

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            txtTime.text = timeFormat.format(Date(entry.timestamp))

            itemView.setOnClickListener {
                val wasExpanded = expandedEntryId == entry.id
                val previousExpandedId = expandedEntryId

                // Toggle expansion
                expandedEntryId = if (wasExpanded) null else entry.id

                // Notify changes with payload to avoid default animation
                if (previousExpandedId != null && previousExpandedId != entry.id) {
                    val prevPos = findPositionById(previousExpandedId)
                    if (prevPos != -1) {
                        notifyItemChanged(prevPos, PAYLOAD_EXPAND_CHANGE)
                    }
                }
                notifyItemChanged(bindingAdapterPosition, PAYLOAD_EXPAND_CHANGE)
            }
        }

        fun updateExpansion(entryId: String) {
            val isExpanded = expandedEntryId == entryId
            txtPreview.maxLines = if (isExpanded) Integer.MAX_VALUE else 3
        }
    }

    private fun findPositionById(id: String): Int {
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if (item is JournalListItem.Entry && item.entry.id == id) {
                return i
            }
        }
        return -1
    }
}

class JournalDiffCallback : DiffUtil.ItemCallback<JournalListItem>() {
    override fun areItemsTheSame(oldItem: JournalListItem, newItem: JournalListItem): Boolean {
        return when {
            oldItem is JournalListItem.DateHeader && newItem is JournalListItem.DateHeader ->
                oldItem.date == newItem.date
            oldItem is JournalListItem.Entry && newItem is JournalListItem.Entry ->
                oldItem.entry.id == newItem.entry.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: JournalListItem, newItem: JournalListItem): Boolean {
        return oldItem == newItem
    }
}
