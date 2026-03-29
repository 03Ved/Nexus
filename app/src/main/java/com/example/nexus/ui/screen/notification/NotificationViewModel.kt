package com.example.nexus.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.data.local.dao.NotificationDao
import com.example.nexus.data.local.entity.NotificationEntity
import com.example.nexus.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NotificationViewModel
 * Manages notification list state and operations:
 * - Fetches notifications from Room database
 * - Groups notifications by type
 * - Handles deletion and clearing operations
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isEmpty = MutableStateFlow(true)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    private val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _groupedNotifications = MutableStateFlow<Map<String, List<NotificationData>>>(emptyMap())
    val groupedNotifications: StateFlow<Map<String, List<NotificationData>>> = _groupedNotifications.asStateFlow()

    init {
        loadNotifications()
    }

    /**
     * Load notifications from repository
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                notificationRepository.getAllNotifications().collect { entities: List<NotificationEntity> ->
                    _notifications.value = entities
                    _isEmpty.value = entities.isEmpty()
                    
                    // Group notifications by type
                    val grouped = entities
                        .sortedByDescending { it.timestamp }
                        .groupBy { it.type }
                        .mapValues { (_, notifs) ->
                            notifs.map { entity ->
                                NotificationData(
                                    id = entity.id,
                                    type = entity.type,
                                    title = entity.title,
                                    body = entity.body,
                                    timestamp = entity.timestamp,
                                    deepLinkDestination = entity.deepLinkDestination,
                                    deepLinkValue = entity.deepLinkValue
                                )
                            }
                        }
                        .toSortedMap(compareBy { typeOrder(it) })
                    
                    _groupedNotifications.value = grouped
                }
            } catch (e: Exception) {
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a notification by ID
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotification(notificationId)
            } catch (e: Exception) {
                _error.value = "Failed to delete notification: ${e.message}"
            }
        }
    }

    /**
     * Delete all notifications of a specific type
     */
    fun deleteNotificationsOfType(type: String) {
        viewModelScope.launch {
            try {
                notificationRepository.deleteNotificationsByType(type)
            } catch (e: Exception) {
                _error.value = "Failed to delete notifications: ${e.message}"
            }
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.clearAllNotifications()
            } catch (e: Exception) {
                _error.value = "Failed to clear notifications: ${e.message}"
            }
        }
    }

    /**
     * Determine sort order for notification types
     */
    private fun typeOrder(type: String): Int = when (type) {
        "new_message" -> 0      // Messages first (highest priority)
        "new_comment" -> 1      // Comments second
        "new_interest" -> 2     // Interests third
        "poll_closing" -> 3     // Polls last
        else -> 4
    }
}
