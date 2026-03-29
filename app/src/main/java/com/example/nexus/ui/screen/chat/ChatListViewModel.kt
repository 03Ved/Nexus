package com.example.nexus.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.ChatRepository
import com.example.nexus.domain.model.Chat
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatFilter { ALL, OWNED, PUBLIC }

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _allChats = MutableStateFlow<List<Chat>>(emptyList())

    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _filteredChats = MutableStateFlow<List<Chat>>(emptyList())
    val filteredChats: StateFlow<List<Chat>> = _filteredChats.asStateFlow()

    val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    init {
        loadChats()
    }

    fun loadChats() {
        viewModelScope.launch {
            _isLoading.value = true
            chatRepository.getAllChatsForUser().collect { result ->
                result.onSuccess { chats ->
                    _allChats.value = chats
                    applyFilter()
                    _isLoading.value = false
                }.onFailure { exception ->
                    _isLoading.value = false
                    _error.value = exception.message ?: "Failed to load chats"
                }
            }
        }
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    private fun applyFilter() {
        val uid = currentUserId
        _filteredChats.value = when (_selectedFilter.value) {
            ChatFilter.ALL -> _allChats.value
            ChatFilter.OWNED -> _allChats.value.filter { it.adminId == uid }
            ChatFilter.PUBLIC -> _allChats.value.filter { it.adminId != uid }
        }.sortedByDescending { it.lastMessageAt }
    }

    fun leaveChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.leaveChat(chatId)
                .onSuccess {
                    _allChats.value = _allChats.value.filter { it.id != chatId }
                    applyFilter()
                }
                .onFailure { _error.value = it.message ?: "Failed to leave chat" }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteChat(chatId)
                .onSuccess {
                    _allChats.value = _allChats.value.filter { it.id != chatId }
                    applyFilter()
                }
                .onFailure { _error.value = it.message ?: "Failed to delete chat" }
        }
    }

    fun getLastMessagePreview(chat: Chat): String {
        return when {
            chat.lastMessage.isEmpty() -> "No messages yet"
            chat.lastMessage.length > 50 -> chat.lastMessage.take(50) + "..."
            else -> chat.lastMessage
        }
    }

    fun getFormattedLastMessageTime(chat: Chat): String {
        if (chat.lastMessageAt <= 0) return ""
        val now = System.currentTimeMillis()
        val diffMs = now - chat.lastMessageAt
        val diffMin = diffMs / 60000
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24
        return when {
            diffMin < 1 -> "now"
            diffMin < 60 -> "${diffMin}m"
            diffHour < 24 -> "${diffHour}h"
            diffDay < 7 -> "${diffDay}d"
            else -> java.text.SimpleDateFormat(
                "MMM d", java.util.Locale.getDefault()
            ).format(chat.lastMessageAt)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun isAdmin(chat: Chat): Boolean = chat.adminId == currentUserId
}