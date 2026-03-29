package com.example.nexus.ui.screen.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.LocationRepository
import com.example.nexus.data.repository.PostRepository
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MapViewModel
 * Manages Campus Map state including:
 * - Posts with location data
 * - Current device location
 * - Selected marker on map
 * - IntentType filters
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    // ===== State Management =====
    
    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _selectedIntentTypeFilter = MutableStateFlow<IntentType?>(null)
    val selectedIntentTypeFilter: StateFlow<IntentType?> = _selectedIntentTypeFilter

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ===== Posts with Location =====

    val postsWithLocation: Flow<List<Post>> = flow {
        postRepository.getAllPosts().collect { result ->
            result.onSuccess { posts ->
                // Filter posts that have a non-null location
                val postsWithLoc = posts.filter { it.latitude != null && it.longitude != null }
                emit(postsWithLoc)
            }.onFailure { exception ->
                emit(emptyList())
                _error.value = exception.message ?: "Failed to load posts"
            }
        }
    }

    // ===== Combined Flow: Posts filtered by selected IntentType =====

    val filteredPosts: Flow<List<Post>> = combine(
        postsWithLocation,
        _selectedIntentTypeFilter
    ) { posts, intentTypeFilter ->
        if (intentTypeFilter == null) {
            posts
        } else {
            posts.filter { it.type == intentTypeFilter }
        }
    }

    // ===== Continuous Location Updates =====

    init {
        // Start listening to location updates
        viewModelScope.launch {
            locationRepository.currentLocation().collect { location ->
                _currentLocation.value = location
                _error.value = null  // Clear error on successful location fetch
            }
        }

        // Load initial posts
        loadPostsWithLocation()
    }

    // ===== Public Functions =====

    /**
     * Load posts with location data from repository
     */
    fun loadPostsWithLocation() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                postRepository.getAllPosts().collect { result ->
                    result.onSuccess {
                        _isLoading.value = false
                    }.onFailure { exception ->
                        _isLoading.value = false
                        _error.value = exception.message ?: "Failed to load posts"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    /**
     * Select a marker/post on the map
     * Updates the UI to show bottom sheet with post details
     */
    fun selectPost(post: Post) {
        _selectedPost.value = post
        _error.value = null
    }

    /**
     * Deselect the current selected post
     * Closes bottom sheet on map
     */
    fun deselectPost() {
        _selectedPost.value = null
    }

    /**
     * Filter posts by IntentType
     * Pass null to show all posts
     */
    fun filterByIntentType(intentType: IntentType?) {
        _selectedIntentTypeFilter.value = intentType
    }

    /**
     * Get all available IntentTypes for filter options
     */
    fun getAvailableIntentTypes(): List<IntentType> {
        return IntentType.values().toList()
    }

    /**
     * Clear all filters and show all posts
     */
    fun clearFilters() {
        _selectedIntentTypeFilter.value = null
    }

    /**
     * Get marker color based on IntentType
     * Returns RGB color value for the marker
     */
    fun getMarkerColorForIntentType(intentType: IntentType): Float {
        return when (intentType) {
            IntentType.LOOKING_FOR -> 0f          // Red
            IntentType.HAPPENING -> 120f          // Green
            IntentType.ASKING -> 210f             // Blue
            IntentType.OFFERING -> 30f            // Orange
        }
    }

    /**
     * Get marker color for a specific post
     */
    fun getMarkerColorForPost(post: Post): Float {
        return getMarkerColorForIntentType(post.type)
    }

    /**
     * Calculate distance from current location to a post location
     * Returns distance in meters or null if either location is unavailable
     */
    fun getDistanceToPost(post: Post): Float? {
        val current = _currentLocation.value ?: return null
        val lat = post.latitude ?: return null
        val lng = post.longitude ?: return null
        val postLocation = Location("").apply {
            latitude = lat
            longitude = lng
        }

        return locationRepository.calculateDistance(current, postLocation)
    }

    /**
     * Get formatted distance string for display
     */
    fun getFormattedDistance(post: Post): String? {
        val distanceInMeters = getDistanceToPost(post) ?: return null

        return when {
            distanceInMeters < 1000 -> {
                String.format("%.0f m", distanceInMeters)
            }
            else -> {
                String.format("%.1f km", distanceInMeters / 1000)
            }
        }
    }

    /**
     * Get count of posts near current location (within 5km)
     */
    fun getPostsNearby(): Flow<Int> = filteredPosts.mapToNearbyCount()

    /**
     * Helper function to count posts within 5km radius
     */
    private fun Flow<List<Post>>.mapToNearbyCount(): Flow<Int> = flow {
        this@mapToNearbyCount.collect { posts ->
            val current = _currentLocation.value
            if (current != null) {
                val nearbyCount = posts.count { post ->
                    val distance = getDistanceToPost(post)
                    distance != null && distance <= 5000  // 5km
                }
                emit(nearbyCount)
            } else {
                emit(0)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Request current location (one-time)
     * Useful for centering map on user location
     */
    fun requestCurrentLocation() {
        viewModelScope.launch {
            locationRepository.getCurrentLocation().collect { location ->
                _currentLocation.value = location
            }
        }
    }

    /**
     * Get last known location (cached)
     */
    fun getLastKnownLocation() {
        viewModelScope.launch {
            locationRepository.getLastKnownLocation().collect { location ->
                if (location != null) {
                    _currentLocation.value = location
                }
            }
        }
    }

    /**
     * Check if a post is currently selected
     */
    fun isPostSelected(postId: String): Boolean {
        return _selectedPost.value?.id == postId
    }

    /**
     * Get total count of posts with location data
     */
    fun getTotalPostsWithLocation(): Flow<Int> = postsWithLocation.mapToCount()

    /**
     * Helper extension to map posts list to count
     */
    private fun Flow<List<Post>>.mapToCount(): Flow<Int> = flow {
        this@mapToCount.collect { posts ->
            emit(posts.size)
        }
    }

    companion object {
        private const val TAG = "MapViewModel"
        private const val NEARBY_RADIUS_METERS = 5000f  // 5km
    }
}
