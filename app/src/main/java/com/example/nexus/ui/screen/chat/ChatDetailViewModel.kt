package com.example.nexus.ui.screen.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.ChatRepository
import com.example.nexus.domain.model.Chat
import com.example.nexus.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _chatId = MutableStateFlow<String?>(null)
    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    private val _selectedImagePath = MutableStateFlow<String?>(null)
    val selectedImagePath: StateFlow<String?> = _selectedImagePath.asStateFlow()

    private val _scrollToLatestMessage = MutableStateFlow(false)
    val scrollToLatestMessage: StateFlow<Boolean> = _scrollToLatestMessage.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun initializeChat(chatId: String) {
        _chatId.value = chatId
        loadChat(chatId)
        subscribeToMessages(chatId)
    }

    private fun loadChat(chatId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = chatRepository.getChatById(chatId)
            if (result.isSuccess) {
                _chat.value = result.getOrNull()
                _isLoading.value = false
            } else {
                _isLoading.value = false
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load chat"
            }
        }
    }

    private fun subscribeToMessages(chatId: String) {
        viewModelScope.launch {
            chatRepository.getMessagesForChat(chatId).collect { result ->
                if (result.isSuccess) {
                    _messages.value = result.getOrNull() ?: emptyList()
                    if (_messages.value.isNotEmpty()) {
                        _scrollToLatestMessage.value = true
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                }
            }
        }
    }

    fun sendMessage() {
        val chatId = _chatId.value ?: return
        val text = _messageInput.value
        val imagePath = _selectedImagePath.value

        if (text.isBlank() && imagePath == null) return

        viewModelScope.launch {
            _isSending.value = true
            val result = chatRepository.sendMessage(
                chatId = chatId,
                text = text,
                localImagePath = imagePath
            )
            if (result.isSuccess) {
                _messageInput.value = ""
                _selectedImagePath.value = null
                _scrollToLatestMessage.value = true
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to send"
            }
            _isSending.value = false
        }
    }

    fun updateMessageInput(text: String) {
        _messageInput.value = text
    }

    fun selectImage(uri: Uri, localPath: String) {
        _selectedImagePath.value = localPath
    }

    fun clearImageSelection() {
        _selectedImagePath.value = null
    }

    fun resetScrollToLatest() {
        _scrollToLatestMessage.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun isAdmin(): Boolean = _chat.value?.adminId == currentUserId

    fun leaveChat(onSuccess: () -> Unit) {
        val chatId = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.leaveChat(chatId)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message ?: "Failed to leave" }
        }
    }

    fun deleteChat(onSuccess: () -> Unit) {
        val chatId = _chatId.value ?: return
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message ?: "Failed to delete" }
        }
    }

    fun getFormattedMessageTime(message: Message): String {
        val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        return sdf.format(message.sentAt)
    }

    fun getMessageDate(message: Message): String {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
        return sdf.format(message.sentAt)
    }
}