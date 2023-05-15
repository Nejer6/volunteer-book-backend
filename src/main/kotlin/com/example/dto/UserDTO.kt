package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: Int,
    val name: String,
    val surname: String
)
