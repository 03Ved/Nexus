package com.example.nexus.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.PostRepository
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.nexus.data.repository.ChatRepository

/**
 * ComposePostViewModel
 * Manages post composition state including title, description, intent type,
 * image selection, and location. Handles draft saving and post creation.
 */
@HiltViewModel
class ComposePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    // Form state
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedIntentType = MutableStateFlow<IntentType>(IntentType.LOOKING_FOR)
    val selectedIntentType: StateFlow<IntentType> = _selectedIntentType.asStateFlow()

    private val _selectedImagePath = MutableStateFlow<String?>(null)
    val selectedImagePath: StateFlow<String?> = _selectedImagePath.asStateFlow()

    private val _selectedLocation = MutableStateFlow<String?>(null)
    private val _selectedLatitude = MutableStateFlow<Double?>(null)
    private val _selectedLongitude = MutableStateFlow<Double?>(null)
    val selectedLocation: StateFlow<String?> = _selectedLocation.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _expiresInHours = MutableStateFlow(24)
    val expiresInHours: StateFlow<Int> = _expiresInHours.asStateFlow()

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isPostCreated = MutableStateFlow(false)
    val isPostCreated: StateFlow<Boolean> = _isPostCreated.asStateFlow()

    // Validation
    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid.asStateFlow()

    init {
        // Observe form fields to update validation
        viewModelScope.launch {
            kotlinx.coroutines.supervisorScope {
                launch { _title.collect { updateFormValidity() } }
                launch { _description.collect { updateFormValidity() } }
                launch { _selectedIntentType.collect { updateFormValidity() } }
            }
        }
        loadDraft()
    }

    /**
     * Update post title
     */
    fun setTitle(newTitle: String) {
        _title.value = newTitle.take(150)  // Max 150 chars
        updateFormValidity()
    }

    /**
     * Update post description
     */
    fun setDescription(newDescription: String) {
        _description.value = newDescription.take(2000)  // Max 2000 chars
        updateFormValidity()
    }

    /**
     * Update selected intent type
     */
    fun setIntentType(intentType: IntentType) {
        _selectedIntentType.value = intentType
        updateFormValidity()
    }

    /**
     * Set selected image path from image picker
     */
    fun setImagePath(imagePath: String?) {
        _selectedImagePath.value = imagePath
    }

    /**
     * Set selected location (from map picker)
     */
    fun setLocation(label: String?, latitude: Double?, longitude: Double?) {
        _selectedLocation.value = label
        _selectedLatitude.value = latitude
        _selectedLongitude.value = longitude
    }

    /**
     * Add tag to post
     */
    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_tags.value.contains(tag)) {
            _tags.value = (_tags.value + tag).take(10)  // Max 10 tags
        }
    }

    /**
     * Remove tag from post
     */
    fun removeTag(tag: String) {
        _tags.value = _tags.value.filter { it != tag }
    }

    /**
     * Set expiration time in hours
     */
    fun setExpiresInHours(hours: Int) {
        _expiresInHours.value = hours.coerceIn(1, 720)  // 1 hour to 30 days
    }

    /**
     * Update form validity based on required fields
     */
    private fun updateFormValidity() {
        val titleValid = _title.value.isNotBlank() && _title.value.length >= 5
        val descriptionValid = _description.value.isNotBlank() && _description.value.length >= 10
        _isFormValid.value = titleValid && descriptionValid
    }

    /**
     * Create and publish post to Firestore
     */
    fun createPost() {
        if (!_isFormValid.value) {
            _error.value = "Title (5+ chars) and description (10+ chars) required"
            return
        }

        val currentUserId = firebaseAuth.currentUser?.uid
        if (currentUserId == null) {
            _error.value = "Not authenticated"
            return
        }

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = firebaseAuth.currentUser
                val post = Post(
                    id = UUID.randomUUID().toString(),
                    authorId = currentUserId,
                    authorName = currentUser?.displayName ?: "Anonymous",
                    authorAvatar = currentUser?.photoUrl?.toString(),
                    type = _selectedIntentType.value,
                    title = _title.value,
                    description = _description.value,
                    localImagePath = _selectedImagePath.value,
                    pollOptions = null,  // Poll creation handled in separate feature
                    tags = _tags.value,
                    latitude = _selectedLatitude.value,
                    longitude = _selectedLongitude.value,
                    locationLabel = _selectedLocation.value,
                    expiresAt = System.currentTimeMillis() + (_expiresInHours.value * 3600 * 1000),
                    createdAt = System.currentTimeMillis(),
                    reactCount = 0,
                    commentCount = 0,
                    interestedUserIds = emptyList()
                )

                postRepository.createPost(post).onSuccess {
                    // Auto-create group chat for this post
                    chatRepository.createGroupChatForPost(
                        postId = post.id,
                        postTitle = post.title
                    )
                    _isLoading.value = false
                    _isPostCreated.value = true
                    clearDraft()
                }.onFailure { error ->
                    _isLoading.value = false
                    _error.value = error.message ?: "Failed to create post"
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = e.message ?: "Unexpected error"
            }
        }
    }

    /**
     * Save current form state as draft to preferences (Room integration pending)
     */
    fun saveDraft() {
        // TODO: Implement draft saving to Room database
        // This will be integrated in database feature
    }

    /**
     * Load draft from local storage if available
     */
    private fun loadDraft() {
        // TODO: Implement draft loading from Room database
        // This will be integrated in database feature
    }

    /**
     * Clear draft from local storage
     */
    private fun clearDraft() {
        // TODO: Implement draft clearing
        // This will be integrated in database feature
    }

    /**
     * Discard composition and reset form
     */
    fun discardPost() {
        _title.value = ""
        _description.value = ""
        _selectedIntentType.value = IntentType.LOOKING_FOR
        _selectedImagePath.value = null
        _selectedLocation.value = null
        _selectedLatitude.value = null
        _selectedLongitude.value = null
        _tags.value = emptyList()
        _expiresInHours.value = 24
        _error.value = null
        updateFormValidity()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Reset post creation success flag
     */
    fun resetPostCreated() {
        _isPostCreated.value = false
    }
}
