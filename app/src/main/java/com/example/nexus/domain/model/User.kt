package com.example.nexus.domain.model

import java.io.Serializable

/**
 * Domain model for a user on Nexus
 * Represents a campus community member with authentication via college email
 * 
 * All fields have default values to support Firestore deserialization,
 * which requires a no-argument constructor
 */
data class User(
    val uid: String = "",                    // Firebase UID
    val name: String = "",
    val email: String = "",                  // College email (validated domain)
    val collegeName: String = "",            // Name of the college/university
    val avatarUrl: String? = null,           // Local file path or URL
    val bio: String = "",
    val skills: List<String> = emptyList(), // User's skills/expertise
    val joinedAt: Long = 0L                  // Unix timestamp in milliseconds
) : Serializable
