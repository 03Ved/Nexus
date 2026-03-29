package com.example.nexus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.PollOption

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val draftId: String = "",
    val authorId: String = "",
    val type: IntentType? = null,
    val title: String = "",
    val description: String = "",
    val localImagePath: String? = null,
    val pollOptions: List<PollOption>? = null,
    val tags: List<String> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val createdAt: Long = 0L,
    val lastModifiedAt: Long = 0L,
    val isDraft: Boolean = true,
    val autoSaveEnabled: Boolean = true
)