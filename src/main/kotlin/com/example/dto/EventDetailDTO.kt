package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventDetailDTO(
    val id: Int,
    val title: String,
    val date: String,
    val direction: String,
    val address: String,
    val organizer: String,
    val description: String,
    val state: String,
    val maxParticipant: Int?
)
