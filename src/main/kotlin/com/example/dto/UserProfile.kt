package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDTO(
    val id: Int,
    val role: String,
    val avatarUrl: String,
    val name: String,
    val surname: String,
    val city: String,
    val birthday: String,
    val phone: String,
    val email: String,
    val organization: String,
    val points: Int,
    val currentEvents: List<EventDTO>,
    val previousEvents: List<EventDTO>
)
