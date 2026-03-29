package com.example.nexus.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.repository.AuthRepository
import com.example.nexus.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuthViewModel
 * Manages authentication state and handles all auth operations
 * Exposes StateFlows for UI consumption with collectAsStateWithLifecycle()
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    init {
        checkAuthenticationState()
    }

    /**
     * Check if user is already authenticated (on app launch)
     * Updates _isUserLoggedIn StateFlow which MainActivity observes to decide navigation
     */
    private fun checkAuthenticationState() {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if user is already logged in
            val isLoggedIn = authRepository.isUserLoggedIn()
            _isUserLoggedIn.value = isLoggedIn

            if (isLoggedIn) {
                // Fetch and cache current user data
                authRepository.getCurrentUser()
                    .onSuccess { user: User? ->
                        _currentUser.value = user
                        _authState.value = if (user != null) {
                            AuthUiState.Success(user)
                        } else {
                            AuthUiState.Error("User data not found")
                        }
                    }
                    .onFailure { error: Throwable ->
                        _authState.value = AuthUiState.Error(error.message ?: "Unknown error")
                    }
            }
        }
    }

    /**
     * Register new user with email and password
     */
    fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        collegeName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = AuthUiState.Loading

            authRepository.registerWithEmail(name, email, password, collegeName)
                .onSuccess { user: User ->
                    _currentUser.value = user
                    _isUserLoggedIn.value = true
                    _authState.value = AuthUiState.Success(user)
                }
                .onFailure { error: Throwable ->
                    _authState.value = AuthUiState.Error(
                        error.message ?: "Registration failed. Please try again."
                    )
                }
        }
    }

    /**
     * Login with email and password
     */
    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = AuthUiState.Loading

            authRepository.loginWithEmail(email, password)
                .onSuccess { user: User ->
                    _currentUser.value = user
                    _isUserLoggedIn.value = true
                    _authState.value = AuthUiState.Success(user)
                }
                .onFailure { error: Throwable ->
                    _authState.value = AuthUiState.Error(
                        when {
                            error.message?.contains("password", ignoreCase = true) == true ->
                                "Invalid email or password"
                            error.message?.contains("user", ignoreCase = true) == true ->
                                "User not found. Please register first."
                            else -> error.message ?: "Login failed. Please try again."
                        }
                    )
                }
        }
    }

    /**
     * Sign in with Google OAuth
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _authState.value = AuthUiState.Loading

            authRepository.signInWithGoogle(idToken)
                .onSuccess { user: User ->
                    _currentUser.value = user
                    _isUserLoggedIn.value = true
                    _authState.value = AuthUiState.Success(user)
                }
                .onFailure { error: Throwable ->
                    _authState.value = AuthUiState.Error(
                        error.message ?: "Google sign-in failed. Please try again."
                    )
                }
        }
    }

    /**
     * Clear auth state (for navigation)
     */
    fun clearAuthState() {
        _authState.value = AuthUiState.Idle
    }

    /**
     * Logout current user
     */
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authRepository.logout()
                .onSuccess { _: Unit ->
                    _currentUser.value = null
                    _isUserLoggedIn.value = false
                    _authState.value = AuthUiState.Idle
                }
                .onFailure { error: Throwable ->
                    _authState.value = AuthUiState.Error(
                        error.message ?: "Logout failed"
                    )
                }
        }
    }
}
