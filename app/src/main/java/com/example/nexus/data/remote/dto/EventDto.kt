package com.example.nexus.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * EventDto
 * Data Transfer Object for college events from REST API
 * Represents the API response structure for events/intents
 */
data class EventDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("type")
    val type: String,  // "LOOKING_FOR", "HAPPENING", "ASKING", "OFFERING"

    @SerializedName("author_id")
    val authorId: String,

    @SerializedName("author_name")
    val authorName: String,

    @SerializedName("author_avatar")
    val authorAvatar: String?,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("location")
    val location: LocationDto?,

    @SerializedName("location_label")
    val locationLabel: String?,

    @SerializedName("created_at")
    val createdAt: Long,  // Unix timestamp in milliseconds

    @SerializedName("expires_at")
    val expiresAt: Long,  // Unix timestamp in milliseconds

    @SerializedName("poll_options")
    val pollOptions: List<PollOptionDto>? = null,

    @SerializedName("image_url")
    val imageUrl: String?,  // Remote URL to image (if any)

    @SerializedName("react_count")
    val reactCount: Int = 0,

    @SerializedName("comment_count")
    val commentCount: Int = 0,

    @SerializedName("interested_count")
    val interestedCount: Int = 0
)

/**
 * LocationDto for API location data
 */
data class LocationDto(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)

/**
 * PollOptionDto for poll options in events
 */
data class PollOptionDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("text")
    val text: String,

    @SerializedName("vote_count")
    val voteCount: Int = 0
)
