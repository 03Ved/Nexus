package com.example.nexus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nexus.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * NotificationDao
 * Data Access Object for notification database operations
 */
@Dao
interface NotificationDao {

    /**
     * Insert a notification
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    /**
     * Insert multiple notifications
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    /**
     * Get all notifications ordered by timestamp (most recent first)
     */
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    /**
     * Get notifications by type
     */
    @Query("SELECT * FROM notification_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getNotificationsByType(type: String): Flow<List<NotificationEntity>>

    /**
     * Get notification by ID
     */
    @Query("SELECT * FROM notification_logs WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?

    /**
     * Delete a notification by ID
     */
    @Query("DELETE FROM notification_logs WHERE id = :id")
    suspend fun deleteNotificationById(id: String)

    /**
     * Delete notifications by type
     */
    @Query("DELETE FROM notification_logs WHERE type = :type")
    suspend fun deleteNotificationsByType(type: String)

    /**
     * Delete all notifications (clear history)
     */
    @Query("DELETE FROM notification_logs")
    suspend fun deleteAllNotifications()

    /**
     * Get count of all notifications
     */
    @Query("SELECT COUNT(*) FROM notification_logs")
    suspend fun getNotificationCount(): Int

    /**
     * Get count of notifications by type
     */
    @Query("SELECT COUNT(*) FROM notification_logs WHERE type = :type")
    suspend fun getNotificationCountByType(type: String): Int

    /**
     * Mark notification as read
     */
    @Query("UPDATE notification_logs SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    /**
     * Delete a notification entity
     */
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    /**
     * Get unread notifications count
     */
    @Query("SELECT COUNT(*) FROM notification_logs WHERE isRead = 0")
    suspend fun getUnreadCount(): Int

    /**
     * Get notifications from last N days
     */
    @Query("SELECT * FROM notification_logs WHERE timestamp > :timeMs ORDER BY timestamp DESC")
    fun getNotificationsAfterTime(timeMs: Long): Flow<List<NotificationEntity>>
}
