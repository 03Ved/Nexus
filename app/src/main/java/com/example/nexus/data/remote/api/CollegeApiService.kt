package com.example.nexus.data.remote.api

import com.example.nexus.data.remote.dto.EventDto
import com.example.nexus.data.remote.dto.UserDto
import com.example.nexus.data.remote.dto.UserProfileDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header

/**
 * CollegeApiService
 * Retrofit service for REST API calls
 * Provides endpoints for events, users, and campus data
 */
interface CollegeApiService {

    // ========== EVENT ENDPOINTS ==========

    /**
     * Get all events/intents
     * @param page pagination page number (0-indexed)
     * @param pageSize number of results per page
     * @param sortBy sort field: "created_at", "expires_at", "interested_count"
     * @param sortOrder "asc" or "desc"
     */
    @GET("events")
    suspend fun getAllEvents(
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort_by") sortBy: String = "created_at",
        @Query("sort_order") sortOrder: String = "desc"
    ): List<EventDto>

    /**
     * Get events filtered by type (intent type)
     * @param type IntentType: "LOOKING_FOR", "HAPPENING", "ASKING", "OFFERING"
     */
    @GET("events/type/{type}")
    suspend fun getEventsByType(
        @Path("type") type: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    /**
     * Get events near a specific campus/location
     * @param campus campus name or identifier
     * @param radiusMiles search radius in miles (default 10)
     */
    @GET("events/campus/{campus}")
    suspend fun getEventsByLocation(
        @Path("campus") campus: String,
        @Query("radius_miles") radiusMiles: Float = 10f,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    /**
     * Get a single event by ID
     */
    @GET("events/{id}")
    suspend fun getEventById(
        @Path("id") eventId: String
    ): EventDto

    /**
     * Search events by query string
     * Searches title, description, and tags
     */
    @GET("events/search")
    suspend fun searchEvents(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    /**
     * Get trending events (by interested count, recent activity)
     */
    @GET("events/trending")
    suspend fun getTrendingEvents(
        @Query("limit") limit: Int = 10
    ): List<EventDto>

    /**
     * Get events from users the current user follows
     */
    @GET("events/following")
    suspend fun getFollowingEvents(
        @Header("Authorization") authToken: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    /**
     * Get current user's events
     */
    @GET("events/my-events")
    suspend fun getMyEvents(
        @Header("Authorization") authToken: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    /**
     * Get saved/bookmarked events
     */
    @GET("events/saved")
    suspend fun getSavedEvents(
        @Header("Authorization") authToken: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<EventDto>

    // ========== USER ENDPOINTS ==========

    /**
     * Get current user profile
     */
    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") authToken: String
    ): UserDto

    /**
     * Get user profile by ID
     */
    @GET("users/{id}")
    suspend fun getUserById(
        @Path("id") userId: String
    ): UserProfileDto

    /**
     * Get user profile by email
     */
    @GET("users/email/{email}")
    suspend fun getUserByEmail(
        @Path("email") email: String
    ): UserProfileDto

    /**
     * Search users by name or skills
     */
    @GET("users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<UserDto>

    /**
     * Get list of users at a specific campus
     */
    @GET("users/campus/{campus}")
    suspend fun getUsersByCollege(
        @Path("campus") campus: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<UserDto>

    /**
     * Get user followers
     */
    @GET("users/{id}/followers")
    suspend fun getUserFollowers(
        @Path("id") userId: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<UserDto>

    /**
     * Get users that a user is following
     */
    @GET("users/{id}/following")
    suspend fun getUserFollowing(
        @Path("id") userId: String,
        @Query("page") page: Int = 0,
        @Query("page_size") pageSize: Int = 20
    ): List<UserDto>

    // ========== CAMPUS/METADATA ENDPOINTS ==========

    /**
     * Get list of supported colleges/campuses
     */
    @GET("campuses")
    suspend fun getCampuses(): List<CampusDto>

    /**
     * Get campus details
     */
    @GET("campuses/{id}")
    suspend fun getCampusById(
        @Path("id") campusId: String
    ): CampusDto
}

/**
 * CampusDto - Campus/college metadata
 */
data class CampusDto(
    val id: String,
    val name: String,
    val location: LocationDto,
    val studentCount: Int = 0,
    val founded: Long? = null
)

/**
 * LocationDto for coordinates
 */
data class LocationDto(
    val latitude: Double,
    val longitude: Double
)
