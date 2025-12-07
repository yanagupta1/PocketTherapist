package com.example.pockettherapist.api

data class EventbriteResponse(
    val events: List<EventbriteEvent>
)

data class EventbriteEvent(
    val name: TextField,
    val description: TextField?,
    val url: String,
    val start: TimeField,
    val venue_id: String?
)

data class TextField(
    val text: String?
)

data class TimeField(
    val local: String
)

data class EventbriteVenue(
    val address: EventbriteAddress?
)

data class EventbriteAddress(
    val localized_address_display: String?,
    val latitude: String?,
    val longitude: String?
)
