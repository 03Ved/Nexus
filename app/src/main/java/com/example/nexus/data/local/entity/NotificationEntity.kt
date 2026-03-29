package com.example.nexus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * NotificationEntity
 * Room database entity for storing local notification logs
 * Stores all received push notification metadata for historical display
 */
@Entity(tableName = "notification_logs")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    val type: String,              // new_interest, new_message, poll_closing, new_comment
    val title: String,             // Display title
    val body: String,              // Notification message/body
    val timestamp: Long,           // When notification was received
    val deepLinkDestination: String? = null,  // Screen to open (post_detail, chat_detail, etc.)
    val deepLinkValue: String? = null,        // Parameter for deep link (postId, chatId, etc.)
    val isRead: Boolean = false,   // Whether user has viewed this notification
    val rawData: String? = null    // Store raw FCM data if needed for troubleshooting
)
