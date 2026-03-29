package com.example.nexus.domain.model

import java.io.Serializable

data class Chat(
    val id: String = "",
    val postId: String = "",
    val name: String = "",
    val adminId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val isGroupChat: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = 0L
) : Serializable
