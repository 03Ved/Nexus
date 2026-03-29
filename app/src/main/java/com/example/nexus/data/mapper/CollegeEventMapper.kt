package com.example.nexus.data.mapper

import com.example.nexus.data.remote.dto.EventDto
import com.example.nexus.data.remote.dto.PollOptionDto
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.PollOption
import com.example.nexus.domain.model.Post
import javax.inject.Inject
import java.util.UUID

/**
 * CollegeEventMapper
 * Maps EventDto (from REST API) to Post (domain model)
 * Handles type conversions, nested object mapping, and null safety
 */
class CollegeEventMapper @Inject constructor() {

    /**
     * Map EventDto to Post domain model
     * Converts API response to internal representation for UI consumption
     */
    fun mapEventToPost(eventDto: EventDto): Post {
        return Post(
            id = eventDto.id,
            authorId = eventDto.authorId,
            authorName = eventDto.authorName,
            authorAvatar = eventDto.authorAvatar,
            type = mapStringToIntentType(eventDto.type),
            title = eventDto.title,
            description = eventDto.description,
            localImagePath = null,  // API doesn't provide local paths; use imageUrl instead
            pollOptions = eventDto.pollOptions?.map { mapPollOptionDtoToPoll(it) },
            tags = eventDto.tags,
            latitude = null,
            longitude = null,
            locationLabel = eventDto.locationLabel,
            expiresAt = eventDto.expiresAt,
            createdAt = eventDto.createdAt,
            reactCount = eventDto.reactCount,
            commentCount = eventDto.commentCount,
            interestedUserIds = emptyList()  // API response doesn't include user-specific data
        )
    }

    /**
     * Map list of EventDtos to list of Posts
     */
    fun mapEventsToPostList(eventDtos: List<EventDto>): List<Post> {
        return eventDtos.map { mapEventToPost(it) }
    }

    /**
     * Convert String intent type to IntentType enum
     * Handles case-insensitive matching and defaults to LOOKING_FOR
     */
    private fun mapStringToIntentType(typeString: String): IntentType {
        return try {
            IntentType.valueOf(typeString.uppercase())
        } catch (e: IllegalArgumentException) {
            // Default fallback if type doesn't match any enum value
            IntentType.LOOKING_FOR
        }
    }

    /**
     * Map PollOptionDto to Poll domain model
     */
    private fun mapPollOptionDtoToPoll(pollOptionDto: PollOptionDto): PollOption {
        return PollOption(
            id = pollOptionDto.id ?: UUID.randomUUID().toString(),
            text = pollOptionDto.text ?: "",
            votes = pollOptionDto.voteCount ?: 0
        )
    }

    /**
     * Get image URL from EventDto
     * Returns remote URL from API or null if not available
     */
    fun getImageUrl(eventDto: EventDto): String? {
        return eventDto.imageUrl
    }

    /**
     * Create a Post from EventDto with additional context
     * Useful for creating posts with User-specific data
     */
    fun mapEventToPostWithContext(
        eventDto: EventDto,
        isInterested: Boolean = false,
        interestedUserIds: List<String> = emptyList()
    ): Post {
        return mapEventToPost(eventDto).copy(
            interestedUserIds = if (isInterested) {
                interestedUserIds.ifEmpty { listOf() }
            } else {
                emptyList()
            }
        )
    }

    /**
     * Validate EventDto has all required fields
     */
    fun isEventDtoValid(eventDto: EventDto): Boolean {
        return eventDto.id.isNotEmpty() &&
                eventDto.title.isNotEmpty() &&
                eventDto.description.isNotEmpty() &&
                eventDto.authorId.isNotEmpty() &&
                eventDto.authorName.isNotEmpty() &&
                eventDto.createdAt > 0 &&
                eventDto.expiresAt > 0
    }

    /**
     * Check if an event has expired
     */
    fun isEventExpired(eventDto: EventDto): Boolean {
        return System.currentTimeMillis() > eventDto.expiresAt
    }

    /**
     * Get time remaining until expiration (in milliseconds)
     * Returns negative value if already expired
     */
    fun getTimeUntilExpiration(eventDto: EventDto): Long {
        return eventDto.expiresAt - System.currentTimeMillis()
    }

    /**
     * Check if event is expiring soon (within 1 hour)
     */
    fun isExpiringWithinHour(eventDto: EventDto): Boolean {
        val timeRemaining = getTimeUntilExpiration(eventDto)
        return timeRemaining in 0..<(3600 * 1000)  // 1 hour in milliseconds
    }
}
