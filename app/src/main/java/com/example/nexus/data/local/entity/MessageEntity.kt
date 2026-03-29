package com.example.nexus.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity for Message
 * Represents a single message in a chat stored locally on device
 * Includes sync metadata for offline-first pattern
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("chatId"),  // Query messages by chat
        Index("senderId"), // Query messages by sender
        Index("sentAt")   // Query recent messages
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val localImagePath: String?,
    val sentAt: Long,
    
    // Sync metadata for offline-first pattern
    val isSyncedWithFirestore: Boolean = false,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
