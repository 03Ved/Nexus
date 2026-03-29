package com.example.nexus.ui.auth

import com.example.nexus.domain.model.User

/**
 * AuthUiState
 * Sealed class representing all possible UI states during authentication flow
 */
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * AuthScreenState
 * Tracks which screen in the auth flow is currently displayed
 */
enum class AuthScreenState {
    SPLASH,
    LOGIN,
    REGISTER
}
