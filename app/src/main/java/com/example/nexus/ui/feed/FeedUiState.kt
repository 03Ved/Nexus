package com.example.nexus.ui.feed

import com.example.nexus.domain.model.Post

/**
 * FeedUiState
 * Sealed class representing all possible UI states in the feed
 */
sealed class FeedUiState {
    data object Idle : FeedUiState()
    data object Loading : FeedUiState()
    data class Success(val posts: List<Post>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}
