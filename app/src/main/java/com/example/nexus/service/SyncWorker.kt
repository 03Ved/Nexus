package com.example.nexus.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.nexus.data.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val postRepository: PostRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Sync task started")

            // Step 1: Sync unsynced posts from Room to Firestore
            syncPostsFromRoom()

            // Step 2: Delete expired posts from local Room cache
            deleteExpiredPosts()

            // Step 3: Update FCM token in Firestore
            updateFCMToken()

            Log.d(TAG, "Sync task completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync task failed: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncPostsFromRoom() {
        try {
            val result = postRepository.syncUnsyncedPosts()
            result.onSuccess { count ->
                Log.d(TAG, "Synced $count posts to Firestore")
            }.onFailure { error ->
                Log.e(TAG, "Failed to sync posts: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing posts: ${e.message}")
        }
    }

    private suspend fun deleteExpiredPosts() {
        try {
            val result = postRepository.deleteExpiredPosts()
            result.onSuccess { count ->
                Log.d(TAG, "Deleted $count expired posts from Room")
            }.onFailure { error ->
                Log.e(TAG, "Failed to delete expired posts: ${error.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expired posts: ${e.message}")
        }
    }

    private fun updateFCMToken() {
        try {
            val currentUser = firebaseAuth.currentUser ?: return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                firebaseFirestore.collection("users")
                    .document(currentUser.uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update FCM token: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val SYNC_WORK_NAME = "nexus_sync_work"
        private const val SYNC_INTERVAL_MINUTES = 15L

        fun schedulePeriodicSync(context: Context) {
            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        }

        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
        }
    }
}