package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventEditDTO(
    val id: Int,
    val title: String,
    val date: String,
    val direction: String,
    val address: String,
    val organizer: String,
    val description: String,
    val requests: List<UserDTO>,
    val participants: List<ParticipantDTO>,
    val maxParticipant: Int?
)
