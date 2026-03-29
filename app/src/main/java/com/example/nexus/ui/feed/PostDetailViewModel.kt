package com.example.nexus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.PostRepository
import com.example.nexus.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.nexus.domain.model.Comment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.nexus.data.repository.ChatRepository

/**
 * PostDetailViewModel
 * Manages fetching and state for a single post by ID
 * Handles comments and "I'm In" interactions
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _hasJoinedChat = MutableStateFlow(false)
    val hasJoinedChat: StateFlow<Boolean> = _hasJoinedChat.asStateFlow()

    private val _joinedChatId = MutableStateFlow<String?>(null)
    val joinedChatId: StateFlow<String?> = _joinedChatId.asStateFlow()

    /**
     * Fetch a post by ID from Firestore
     */
    fun loadPost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = firebaseAuth.currentUser?.uid ?: ""
            _currentUserId.value = uid
            _isLoading.value = true
            _error.value = null

            val result = postRepository.getPostById(postId)
            if (result.isSuccess) {
                val post = result.getOrNull()!!
                _post.value = post
                _isLiked.value = uid in post.likedUserIds
                // Check if user already joined the chat for this post
                val hasJoined = chatRepository.hasUserJoinedChat(post.id)
                _hasJoinedChat.value = hasJoined.getOrDefault(false)
                _isLoading.value = false
                loadComments(postId)
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load post"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load comments for the current post (stub - returns empty for now)
     * In a full implementation, this would query the Firestore subcollection:
     * posts/{postId}/comments/{commentId}
     */
    private fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId)
                .collect { result ->
                    result.onSuccess { comments ->
                        _comments.value = comments
                    }
                }
        }
    }

    /**
     * Join chat when user clicks "I'm In"
     * Once joined, the button disappears permanently for this user
     */
    fun joinChat(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Mark as interested in Firestore
            postRepository.markInterested(postId)
                .onSuccess {
                    val uid = firebaseAuth.currentUser?.uid ?: return@launch
                    _post.value = _post.value?.copy(
                        interestedUserIds = (
                                (_post.value?.interestedUserIds ?: emptyList()) + uid
                                ).distinct()
                    )
                }

            // Join the group chat
            chatRepository.joinChatForPost(postId)
                .onSuccess { chat ->
                    _hasJoinedChat.value = true
                    _joinedChatId.value = chat.id
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to join chat"
                }
        }
    }

    /**
     * Add a comment to the post (stub - will be implemented with Firestore)
     */
    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentUser = firebaseAuth.currentUser ?: return@launch

            postRepository.addComment(postId, text)
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to add comment"
                }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentUserId = firebaseAuth.currentUser?.uid ?: return@launch
            if (_isLiked.value) {
                firebaseFirestore.collection("posts").document(postId)
                    .update(
                        mapOf(
                            "reactCount" to com.google.firebase.firestore.FieldValue.increment(-1),
                            "likedUserIds" to com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId)
                        )
                    ).await()
                _isLiked.value = false
            } else {
                firebaseFirestore.collection("posts").document(postId)
                    .update(
                        mapOf(
                            "reactCount" to com.google.firebase.firestore.FieldValue.increment(1),
                            "likedUserIds" to com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId)
                        )
                    ).await()
                _isLiked.value = true
            }
            loadPost(postId)
        }
    }
}
