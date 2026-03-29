package com.example.nexus.domain.model

import java.io.Serializable

/**
 * Domain model for a single message in a chat
 * Supports text and local image attachments
 * All fields have default values to support Firestore deserialization
 */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val localImagePath: String? = null,
    val sentAt: Long = 0L
) : Serializable
