package com.example.nexus.ui.feed

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
import javax.inject.Inject

/**
 * FeedViewModel
 * Manages feed state with real-time Firestore listener
 * Handles post filtering, swipe actions, and error states
 */
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _feedState = MutableStateFlow<FeedUiState>(FeedUiState.Idle)
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<IntentType?>(null)
    val selectedFilter: StateFlow<IntentType?> = _selectedFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _allPosts = mutableListOf<Post>()

    init {
        _currentUserId.value = firebaseAuth.currentUser?.uid ?: ""
        loadPosts()
    }

    /**
     * Load all posts with real-time Firestore listener
     */
    private fun loadPosts() {
        _feedState.value = FeedUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            postRepository.getAllPosts()
                .collect { result ->
                    result.onSuccess { posts ->
                        _allPosts.clear()
                        _allPosts.addAll(posts)
                        updateFilteredPosts()
                    }
                    result.onFailure { error ->
                        _feedState.value = FeedUiState.Error(
                            error.message ?: "Failed to load posts"
                        )
                    }
                }
        }
    }

    /**
     * Refresh posts by reloading from Firestore
     */
    fun refreshPosts() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadPosts()
            _isRefreshing.value = false
        }
    }

    /**
     * Update feed based on selected filter
     */
    fun setFilter(intentType: IntentType?) {
        _selectedFilter.value = intentType
        updateFilteredPosts()
    }

    /**
     * Update displayed posts based on current filter
     */
    private fun updateFilteredPosts() {
        val filter = _selectedFilter.value
        val filteredPosts = if (filter != null) {
            _allPosts.filter { it.type == filter }
        } else {
            _allPosts
        }

        _feedState.value = FeedUiState.Success(filteredPosts)
    }

    /**
     * Mark post as interested (swipe right action)
     */
    fun markPostInterested(postId: String) {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Update local state immediately for responsive UI
            val index = _allPosts.indexOfFirst { it.id == postId }
            if (index != -1) {
                val post = _allPosts[index]
                val isInterested = currentUserId in post.interestedUserIds
                val updatedPost = if (isInterested) {
                    post.copy(interestedUserIds = post.interestedUserIds - currentUserId)
                } else {
                    post.copy(interestedUserIds = post.interestedUserIds + currentUserId)
                }
                _allPosts[index] = updatedPost
                updateFilteredPosts()
            }
            // Also update in Firestore
            val post = _allPosts.find { it.id == postId }
            val isInterested = post?.let { currentUserId in it.interestedUserIds } ?: false
            if (isInterested) {
                postRepository.unmarkInterested(postId)
            } else {
                postRepository.markInterested(postId)
            }
        }
    }

    /**
     * Unmark post as interested
     */
    fun unmarkPostInterested(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            postRepository.unmarkInterested(postId)
                .onFailure { error ->
                    _feedState.value = FeedUiState.Error(
                        error.message ?: "Failed to unmark as interested"
                    )
                }
        }
    }
}
