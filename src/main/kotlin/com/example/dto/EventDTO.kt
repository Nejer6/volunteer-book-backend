package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class EventDTO(
    val id: Int,
    val title: String,
    val date: String,
    val direction: String,
    val points: Int?
)
