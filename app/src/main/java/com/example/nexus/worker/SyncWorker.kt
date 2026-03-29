package com.example.nexus.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nexus.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * SyncWorker
 * Background synchronization worker using WorkManager
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "nexus_background_sync"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Background sync started")
            syncPostsFromFirestore()
            deleteExpiredPosts()
            refreshFCMToken()
            Log.d(TAG, "Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncPostsFromFirestore() {
        try {
            postRepository.syncUnsyncedPosts()
            Log.d(TAG, "Post sync completed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sync posts: ${e.message}")
        }
    }

    private suspend fun deleteExpiredPosts() {
        try {
            postRepository.deleteExpiredPosts()
            Log.d(TAG, "Expired post cleanup completed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete expired posts: ${e.message}")
        }
    }

    private suspend fun refreshFCMToken() {
        try {
            val currentToken = FirebaseMessaging.getInstance().token.await()
            val userId = firebaseAuth.currentUser?.uid ?: return
            if (currentToken.isNotEmpty()) {
                firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", currentToken)
                    .await()
                Log.d(TAG, "FCM token updated")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh FCM token: ${e.message}")
        }
    }
}
