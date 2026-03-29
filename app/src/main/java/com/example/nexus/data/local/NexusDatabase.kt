package com.example.nexus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nexus.data.local.dao.DraftDao
import com.example.nexus.data.local.dao.MessageDao
import com.example.nexus.data.local.dao.NotificationDao
import com.example.nexus.data.local.dao.PostDao
import com.example.nexus.data.local.entity.DraftEntity
import com.example.nexus.data.local.entity.MessageEntity
import com.example.nexus.data.local.entity.NotificationEntity
import com.example.nexus.data.local.entity.PostEntity

/**
 * NexusDatabase
 * Main Room database for Nexus app
 * Contains all entities and provides DAOs
 */
@Database(
    entities = [
        PostEntity::class,
        DraftEntity::class,
        MessageEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NexusDatabase : RoomDatabase() {

    /**
     * Post DAO for post-related database operations
     */
    abstract fun postDao(): PostDao

    /**
     * Draft DAO for draft-related database operations
     */
    abstract fun draftDao(): DraftDao

    /**
     * Message DAO for message-related database operations
     */
    abstract fun messageDao(): MessageDao

    /**
     * Notification DAO for notification-related database operations
     */
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "nexus_database"
    }
}
