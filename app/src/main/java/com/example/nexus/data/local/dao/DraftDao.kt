package com.example.nexus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.local.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

/**
 * DraftDao
 * Data Access Object for Draft entity
 * Provides database operations for managing post drafts
 */
@Dao
interface DraftDao {

    /**
     * Insert a new draft
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftEntity): Long

    /**
     * Insert multiple drafts
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrafts(drafts: List<DraftEntity>)

    /**
     * Update an existing draft (e.g., auto-save)
     */
    @Update
    suspend fun updateDraft(draft: DraftEntity)

    /**
     * Delete a draft by id
     */
    @Query("DELETE FROM drafts WHERE id = :draftRowId")
    suspend fun deleteDraft(draftRowId: Int)

    /**
     * Delete a draft by draftId (UUID)
     */
    @Query("DELETE FROM drafts WHERE draftId = :draftId")
    suspend fun deleteDraftByUUID(draftId: String)

    /**
     * Delete all drafts for a user
     */
    @Query("DELETE FROM drafts WHERE authorId = :authorId")
    suspend fun deleteDraftsByAuthor(authorId: String)

    /**
     * Delete all drafts
     */
    @Query("DELETE FROM drafts")
    suspend fun deleteAllDrafts()

    /**
     * Get a single draft by row id
     */
    @Query("SELECT * FROM drafts WHERE id = :draftRowId")
    suspend fun getDraftById(draftRowId: Int): DraftEntity?

    /**
     * Get a draft by UUID
     */
    @Query("SELECT * FROM drafts WHERE draftId = :draftId")
    suspend fun getDraftByUUID(draftId: String): DraftEntity?

    /**
     * Get all drafts for the current user as Flow
     */
    @Query("SELECT * FROM drafts WHERE authorId = :authorId ORDER BY lastModifiedAt DESC")
    fun getDraftsByAuthor(authorId: String): Flow<List<DraftEntity>>

    /**
     * Get all drafts as Flow
     */
    @Query("SELECT * FROM drafts ORDER BY lastModifiedAt DESC")
    fun getAllDrafts(): Flow<List<DraftEntity>>

    /**
     * Get draft count for a user
     */
    @Query("SELECT COUNT(*) FROM drafts WHERE authorId = :authorId")
    suspend fun getDraftCount(authorId: String): Int

    /**
     * Search drafts by title or description
     */
    @Query("""
        SELECT * FROM drafts 
        WHERE authorId = :authorId 
          AND (title LIKE '%' || :query || '%' 
               OR description LIKE '%' || :query || '%')
        ORDER BY lastModifiedAt DESC
    """)
    fun searchDrafts(authorId: String, query: String): Flow<List<DraftEntity>>

    /**
     * Get recently modified drafts (last 7 days)
     */
    @Query("""
        SELECT * FROM drafts 
        WHERE authorId = :authorId 
          AND lastModifiedAt > :timestamp7DaysAgo
        ORDER BY lastModifiedAt DESC
    """)
    fun getRecentDrafts(authorId: String, timestamp7DaysAgo: Long): Flow<List<DraftEntity>>

    /**
     * Get drafts with auto-save enabled
     */
    @Query("""
        SELECT * FROM drafts 
        WHERE authorId = :authorId AND autoSaveEnabled = 1
        ORDER BY lastModifiedAt DESC
    """)
    fun getAutoSaveDrafts(authorId: String): Flow<List<DraftEntity>>

    /**
     * Delete drafts older than specified timestamp
     */
    @Query("DELETE FROM drafts WHERE lastModifiedAt < :timestamp")
    suspend fun deleteDraftsOlderThan(timestamp: Long): Int

    /**
     * Get draft with most recent lastModifiedAt
     */
    @Query("""
        SELECT * FROM drafts 
        WHERE authorId = :authorId 
        ORDER BY lastModifiedAt DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentDraft(authorId: String): DraftEntity?

    /**
     * Check if a draft with given UUID exists
     */
    @Query("SELECT COUNT(*) > 0 FROM drafts WHERE draftId = :draftId")
    suspend fun draftExists(draftId: String): Boolean

    /**
     * Get all incomplete drafts (missing required fields)
     */
    @Query("""
        SELECT * FROM drafts 
        WHERE authorId = :authorId 
          AND (type IS NULL OR title = '' OR description = '')
        ORDER BY lastModifiedAt DESC
    """)
    fun getIncompleteDrafts(authorId: String): Flow<List<DraftEntity>>

    /**
     * Clear old drafts for storage optimization (older than 30 days)
     */
    @Query("""
        DELETE FROM drafts 
        WHERE lastModifiedAt < :thirtyDaysAgo
    """)
    suspend fun clearOldDrafts(thirtyDaysAgo: Long): Int
}
