package com.example.nexus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.PollOption

/**
 * Room Entity for Post
 * Represents a post/intent stored locally on device
 * Includes sync metadata for offline-first pattern
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String?,
    val type: IntentType,
    val title: String,
    val description: String,
    val localImagePath: String?,
    val pollOptions: List<PollOption>?,
    val tags: List<String>,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String?,
    val expiresAt: Long,
    val createdAt: Long,
    val reactCount: Int,
    val commentCount: Int,
    val interestedUserIds: List<String>,
    val likedUserIds: List<String> = emptyList(),

    // Sync metadata for offline-first pattern
    val isSyncedWithFirestore: Boolean = false,
    val lastSyncedAt: Long? = null,
    val syncError: String? = null
)
