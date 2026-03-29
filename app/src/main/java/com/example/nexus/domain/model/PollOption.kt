package com.example.nexus.domain.model

import java.io.Serializable

/**
 * Represents a polling option in an ASKING type post
 * All fields have default values to support Firestore deserialization
 */
data class PollOption(
    val id: String = "",
    val text: String = "",
    val votes: Int = 0
) : Serializable
