package com.example.nexus.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.AuthRepository
import com.example.nexus.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileViewModel
 * Manages profile screen state:
 * - Fetches current user information
 * - Handles logout
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Load current user from repository
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val result = authRepository.getCurrentUser()
                result.onSuccess { user ->
                    _user.value = user
                }.onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load user"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
            } catch (e: Exception) {
                _error.value = "Logout failed: ${e.message}"
            }
        }
    }
}
