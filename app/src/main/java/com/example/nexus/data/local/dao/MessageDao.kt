package com.example.nexus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * MessageDao
 * Data Access Object for Message entity
 * Provides database operations for managing messages
 */
@Dao
interface MessageDao {

    /**
     * Insert a new message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    /**
     * Insert multiple messages (e.g., loading chat history)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    /**
     * Update an existing message
     */
    @Update
    suspend fun updateMessage(message: MessageEntity)

    /**
     * Delete a message by id
     */
    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    /**
     * Delete all messages in a chat
     */
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChat(chatId: String)

    /**
     * Delete all messages
     */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /**
     * Get a single message by id
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    /**
     * Get all messages in a chat ordered by send time (newest first)
     */
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY sentAt DESC")
    fun getMessagesByChat(chatId: String): Flow<List<MessageEntity>>

    /**
     * Get recent messages in a chat (paginated)
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId 
        ORDER BY sentAt DESC 
        LIMIT :pageSize OFFSET :offset
    """)
    suspend fun getMessagesByChatPaginated(
        chatId: String,
        pageSize: Int,
        offset: Int
    ): List<MessageEntity>

    /**
     * Get messages sent by a specific user
     */
    @Query("SELECT * FROM messages WHERE senderId = :senderId ORDER BY sentAt DESC")
    fun getMessagesBySender(senderId: String): Flow<List<MessageEntity>>

    /**
     * Get messages sent by user in specific chat
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND senderId = :senderId 
        ORDER BY sentAt DESC
    """)
    fun getMessagesBySenderInChat(chatId: String, senderId: String): Flow<List<MessageEntity>>

    /**
     * Get unsynced messages (for syncing with Firestore)
     */
    @Query("SELECT * FROM messages WHERE isSyncedWithFirestore = 0 ORDER BY sentAt ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    /**
     * Get unsynced messages for a specific chat
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND isSyncedWithFirestore = 0 
        ORDER BY sentAt ASC
    """)
    suspend fun getUnsyncedMessagesByChat(chatId: String): List<MessageEntity>

    /**
     * Mark messages as synced with Firestore
     */
    @Query("UPDATE messages SET isSyncedWithFirestore = 1, lastSyncedAt = :syncTime WHERE id IN (:messageIds)")
    suspend fun markMessagesAsSynced(messageIds: List<String>, syncTime: Long)

    /**
     * Mark message sync failure
     */
    @Query("UPDATE messages SET syncError = :error WHERE id = :messageId")
    suspend fun markMessageSyncError(messageId: String, error: String)

    /**
     * Get count of messages in a chat
     */
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessageCount(chatId: String): Int

    /**
     * Search messages by text content
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND text LIKE '%' || :query || '%'
        ORDER BY sentAt DESC
    """)
    fun searchMessagesInChat(chatId: String, query: String): Flow<List<MessageEntity>>

    /**
     * Get recent messages (last 24 hours) in a chat
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND sentAt > :timestamp24HoursAgo
        ORDER BY sentAt DESC
    """)
    fun getRecentMessagesInChat(chatId: String, timestamp24HoursAgo: Long): Flow<List<MessageEntity>>

    /**
     * Get messages since a specific timestamp
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND sentAt > :sinceTimestamp
        ORDER BY sentAt ASC
    """)
    suspend fun getMessagesSince(chatId: String, sinceTimestamp: Long): List<MessageEntity>

    /**
     * Delete messages older than specified timestamp
     */
    @Query("DELETE FROM messages WHERE sentAt < :timestamp")
    suspend fun deleteMessagesOlderThan(timestamp: Long): Int

    /**
     * Clear old messages for a specific chat
     */
    @Query("""
        DELETE FROM messages 
        WHERE chatId = :chatId AND sentAt < :timestamp
    """)
    suspend fun deleteOldMessagesInChat(chatId: String, timestamp: Long): Int

    /**
     * Get most recent message in a chat
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId 
        ORDER BY sentAt DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentMessage(chatId: String): MessageEntity?

    /**
     * Clear all messages (for logout or cache clear)
     */
    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    /**
     * Get messages with local image attachments
     */
    @Query("""
        SELECT * FROM messages 
        WHERE chatId = :chatId AND localImagePath IS NOT NULL
        ORDER BY sentAt DESC
    """)
    fun getMessagesWithImages(chatId: String): Flow<List<MessageEntity>>

    /**
     * Count unsynced messages
     */
    @Query("SELECT COUNT(*) FROM messages WHERE isSyncedWithFirestore = 0")
    suspend fun getUnsyncedMessageCount(): Int
}
