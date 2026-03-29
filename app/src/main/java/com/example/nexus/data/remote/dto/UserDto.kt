package com.example.nexus.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * UserDto
 * Data Transfer Object for user profile from REST API
 * Represents the API response structure for user data
 */
data class UserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("college_name")
    val collegeName: String,

    @SerializedName("avatar_url")
    val avatarUrl: String?,

    @SerializedName("bio")
    val bio: String = "",

    @SerializedName("skills")
    val skills: List<String> = emptyList(),

    @SerializedName("joined_at")
    val joinedAt: Long,  // Unix timestamp in milliseconds

    @SerializedName("post_count")
    val postCount: Int = 0,

    @SerializedName("follower_count")
    val followerCount: Int = 0,

    @SerializedName("following_count")
    val followingCount: Int = 0,

    @SerializedName("is_verified")
    val isVerified: Boolean = false,

    @SerializedName("verification_badge")
    val verificationBadge: String? = null,  // e.g., "official", "student", etc.

    @SerializedName("last_active")
    val lastActive: Long? = null  // Unix timestamp of last activity
)

/**
 * UserProfileDto - Extended user profile with additional details
 */
data class UserProfileDto(
    @SerializedName("user")
    val user: UserDto,

    @SerializedName("recent_posts")
    val recentPosts: List<EventDto> = emptyList(),

    @SerializedName("followers")
    val followers: List<UserDto> = emptyList(),

    @SerializedName("is_following")
    val isFollowing: Boolean = false,

    @SerializedName("is_follower")
    val isFollower: Boolean = false
)
