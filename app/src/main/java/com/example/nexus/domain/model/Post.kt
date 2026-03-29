package com.example.nexus.domain.model

import java.io.Serializable

data class Post(
    val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val type: IntentType = IntentType.LOOKING_FOR,
    val title: String = "",
    val description: String = "",
    val localImagePath: String? = null,
    val pollOptions: List<PollOption>? = null,
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val expiresAt: Long = 0L,
    val createdAt: Long = 0L,
    val reactCount: Int = 0,
    val commentCount: Int = 0,
    val interestedUserIds: List<String> = emptyList(),
    val likedUserIds: List<String> = emptyList()
) : Serializable

data class Comment(
    val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Long = 0L
) : Serializable