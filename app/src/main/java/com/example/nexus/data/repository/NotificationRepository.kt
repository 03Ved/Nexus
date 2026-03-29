package com.example.nexus.data.repository

import com.example.nexus.data.local.dao.NotificationDao
import com.example.nexus.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NotificationRepository
 * Repository for managing notification data persistence
 * Handles all Room database operations for notifications
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {

    /**
     * Get all notifications as Flow
     * Ordered by timestamp (most recent first)
     */
    fun getAllNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications()

    /**
     * Get notifications by type
     */
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsByType(type)

    /**
     * Save a new notification to local database
     */
    suspend fun saveNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    /**
     * Save multiple notifications
     */
    suspend fun saveNotifications(notifications: List<NotificationEntity>) {
        notificationDao.insertNotifications(notifications)
    }

    /**
     * Delete notification by ID
     */
    suspend fun deleteNotification(notificationId: String) {
        notificationDao.deleteNotificationById(notificationId)
    }

    /**
     * Delete all notifications of a specific type
     */
    suspend fun deleteNotificationsByType(type: String) {
        notificationDao.deleteNotificationsByType(type)
    }

    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications() {
        notificationDao.deleteAllNotifications()
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String) {
        notificationDao.markAsRead(notificationId)
    }

    /**
     * Get total notification count
     */
    suspend fun getNotificationCount(): Int =
        notificationDao.getNotificationCount()

    /**
     * Get count of notifications by type
     */
    suspend fun getNotificationCountByType(type: String): Int =
        notificationDao.getNotificationCountByType(type)

    /**
     * Get unread notification count
     */
    suspend fun getUnreadCount(): Int =
        notificationDao.getUnreadCount()

    /**
     * Get notifications from last N days
     */
    fun getNotificationsAfterTime(timeMs: Long): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsAfterTime(timeMs)
}
