package com.example.nexus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.local.entity.PostEntity
import com.example.nexus.domain.model.IntentType
import kotlinx.coroutines.flow.Flow

/**
 * PostDao
 * Data Access Object for Post entity
 * Provides database operations: insert, update, delete, query
 */
@Dao
interface PostDao {

    /**
     * Insert a new post into the database
     * Will replace if post with same id already exists
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    /**
     * Insert multiple posts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    /**
     * Update an existing post
     */
    @Update
    suspend fun updatePost(post: PostEntity)

    /**
     * Delete a post by id
     */
    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    /**
     * Delete all posts
     */
    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    /**
     * Get a single post by id
     */
    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    /**
     * Get all posts as Flow for reactive updates
     */
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    /**
     * Get posts by intent type
     */
    @Query("SELECT * FROM posts WHERE type = :intentType ORDER BY createdAt DESC")
    fun getPostsByType(intentType: IntentType): Flow<List<PostEntity>>

    /**
     * Get unsynced posts (for syncing with Firestore)
     */
    @Query("SELECT * FROM posts WHERE isSyncedWithFirestore = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedPosts(): List<PostEntity>

    /**
     * Get posts near expiration time
     */
    @Query("SELECT * FROM posts WHERE expiresAt > :currentTime ORDER BY expiresAt ASC")
    fun getActivePostsStream(currentTime: Long): Flow<List<PostEntity>>

    /**
     * Mark posts as synced with Firestore
     */
    @Query("UPDATE posts SET isSyncedWithFirestore = 1, lastSyncedAt = :syncTime WHERE id IN (:postIds)")
    suspend fun markPostsAsSynced(postIds: List<String>, syncTime: Long)

    /**
     * Mark post sync failure
     */
    @Query("UPDATE posts SET syncError = :error WHERE id = :postId")
    suspend fun markPostSyncError(postId: String, error: String)

    /**
     * Get count of all posts
     */
    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    /**
     * Search posts by title or description
     */
    @Query("""
        SELECT * FROM posts 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchPosts(query: String): Flow<List<PostEntity>>

    /**
     * Get posts by a specific author
     */
    @Query("SELECT * FROM posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getPostsByAuthor(authorId: String): Flow<List<PostEntity>>

    /**
     * Get posts the user has marked as interested in
     */
    @Query("""
        SELECT * FROM posts 
        WHERE id IN (SELECT id FROM posts WHERE :userId IN (interestedUserIds))
        ORDER BY createdAt DESC
    """)
    fun getUserInterestedPosts(userId: String): Flow<List<PostEntity>>

    /**
     * Delete expired posts
     */
    @Query("DELETE FROM posts WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredPosts(currentTime: Long): Int

    /**
     * Clear all posts (for logout or cache clear)
     */
    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    /**
     * Get recently created posts as a stream (last 24 hours)
     */
    @Query("""
        SELECT * FROM posts 
        WHERE createdAt > :timestamp24HoursAgo
        ORDER BY createdAt DESC
    """)
    fun getRecentPostsStream(timestamp24HoursAgo: Long): Flow<List<PostEntity>>
}
