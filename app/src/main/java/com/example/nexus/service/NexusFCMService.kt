package com.example.nexus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.nexus.MainActivity
import com.example.nexus.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * NexusFCMService
 * Firebase Cloud Messaging Service for handling push notifications
 * 
 * Supported notification types:
 * - new_interest: User showed interest in a post
 * - new_message: New message in a chat
 * - poll_closing: Poll is closing soon
 * - new_comment: New comment on a post
 */
class NexusFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Save token to Firestore
        saveFCMTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Handle notification payload (if sent alongside data)
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification title: ${it.title}")
            Log.d(TAG, "Message notification body: ${it.body}")
        }
    }

    /**
     * Handle data messages and dispatch to appropriate handler
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val notificationType = data["type"] ?: return
        
        Log.d(TAG, "Handling notification type: $notificationType")
        
        when (notificationType) {
            "new_interest" -> handleNewInterestNotification(data)
            "new_message" -> handleNewMessageNotification(data)
            "poll_closing" -> handlePollClosingNotification(data)
            "new_comment" -> handleNewCommentNotification(data)
            else -> Log.w(TAG, "Unknown notification type: $notificationType")
        }
    }

    /**
     * Handle new_interest notifications
     * Sent when a user shows interest in one of your posts
     * Data: postId, userId, userName
     */
    private fun handleNewInterestNotification(data: Map<String, String>) {
        val postId = data["postId"] ?: return
        val userName = data["userName"] ?: "Someone"
        
        val title = "New Interest"
        val body = "$userName is interested in your post"
        val channelId = CHANNEL_NEW_INTEREST
        
        showNotification(
            title = title,
            body = body,
            channelId = channelId,
            notificationType = "new_interest",
            deepLinkData = mapOf(
                "destination" to "post_detail",
                "postId" to postId
            )
        )
    }

    /**
     * Handle new_message notifications
     * Sent when a new message arrives in a chat
     * Data: chatId, senderId, senderName, messagePreview
     */
    private fun handleNewMessageNotification(data: Map<String, String>) {
        val chatId = data["chatId"] ?: return
        val senderName = data["senderName"] ?: "Unknown"
        val messagePreview = data["messagePreview"] ?: ""
        
        val title = "New Message from $senderName"
        val body = messagePreview.take(100)
        val channelId = CHANNEL_NEW_MESSAGE
        
        showNotification(
            title = title,
            body = body,
            channelId = channelId,
            notificationType = "new_message",
            deepLinkData = mapOf(
                "destination" to "chat_detail",
                "chatId" to chatId
            )
        )
    }

    /**
     * Handle poll_closing notifications
     * Sent when a poll is closing soon
     * Data: postId, pollQuestion
     */
    private fun handlePollClosingNotification(data: Map<String, String>) {
        val postId = data["postId"] ?: return
        val pollQuestion = data["pollQuestion"] ?: "A poll"
        
        val title = "Poll Closing Soon"
        val body = "$pollQuestion is closing in 1 hour"
        val channelId = CHANNEL_POLL_CLOSING
        
        showNotification(
            title = title,
            body = body,
            channelId = channelId,
            notificationType = "poll_closing",
            deepLinkData = mapOf(
                "destination" to "post_detail",
                "postId" to postId
            )
        )
    }

    /**
     * Handle new_comment notifications
     * Sent when there's a new comment on your post
     * Data: postId, commenterId, commenterName, commentPreview
     */
    private fun handleNewCommentNotification(data: Map<String, String>) {
        val postId = data["postId"] ?: return
        val commenterName = data["commenterName"] ?: "Someone"
        val commentPreview = data["commentPreview"] ?: ""
        
        val title = "New Comment from $commenterName"
        val body = commentPreview.take(100)
        val channelId = CHANNEL_NEW_COMMENT
        
        showNotification(
            title = title,
            body = body,
            channelId = channelId,
            notificationType = "new_comment",
            deepLinkData = mapOf(
                "destination" to "post_detail",
                "postId" to postId
            )
        )
    }

    /**
     * Show local notification with appropriate channel
     */
    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        notificationType: String,
        deepLinkData: Map<String, String>
    ) {
        try {
            // Create notification channel for Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(channelId)
            }

            // Build notification based on type
            val notificationBuilder = when (notificationType) {
                "new_interest" -> NotificationHelper.buildNewInterestNotification(
                    context = this,
                    title = title,
                    body = body,
                    postId = deepLinkData["postId"] ?: "",
                    userName = deepLinkData["userName"] ?: "Someone"
                )
                "new_message" -> NotificationHelper.buildNewMessageNotification(
                    context = this,
                    title = title,
                    body = body,
                    chatId = deepLinkData["chatId"] ?: "",
                    senderId = deepLinkData["senderId"] ?: "",
                    senderName = deepLinkData["senderName"] ?: "Unknown"
                )
                "poll_closing" -> NotificationHelper.buildPollClosingNotification(
                    context = this,
                    title = title,
                    body = body,
                    postId = deepLinkData["postId"] ?: "",
                    pollQuestion = deepLinkData["pollQuestion"] ?: ""
                )
                "new_comment" -> NotificationHelper.buildNewCommentNotification(
                    context = this,
                    title = title,
                    body = body,
                    postId = deepLinkData["postId"] ?: "",
                    commenterId = deepLinkData["commenterId"] ?: "",
                    commenterName = deepLinkData["commenterName"] ?: "Someone"
                )
                else -> NotificationHelper.buildGenericNotification(
                    context = this,
                    title = title,
                    body = body
                )
            }

            // Send notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())
            
            Log.d(TAG, "Notification shown: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }

    /**
     * Create notification channel for Android 8+
     */
    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val (name, description, importance) = when (channelId) {
                CHANNEL_NEW_INTEREST -> Triple(
                    "New Interests",
                    "Notifications when someone is interested in your post",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                CHANNEL_NEW_MESSAGE -> Triple(
                    "New Messages",
                    "Notifications for new chat messages",
                    NotificationManager.IMPORTANCE_HIGH  // Higher priority for messages
                )
                CHANNEL_POLL_CLOSING -> Triple(
                    "Poll Closing",
                    "Notifications when polls are closing soon",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                CHANNEL_NEW_COMMENT -> Triple(
                    "New Comments",
                    "Notifications when there are new comments on your posts",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                else -> Triple(
                    "Nexus Notifications",
                    "Updates from Nexus",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            }

            val channel = NotificationChannel(channelId, name, importance).apply {
                this.description = description
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $channelId")
        }
    }

    /**
     * Save FCM token to Firestore user document
     */
    private fun saveFCMTokenToFirestore(token: String) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userId = currentUser.uid
                val db = FirebaseFirestore.getInstance()
                
                db.collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token saved to Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to save FCM token: ${e.message}", e)
                    }
            } else {
                Log.w(TAG, "No authenticated user, cannot save FCM token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "NexusFCMService"
        
        // Notification channel IDs for Android 8+
        const val CHANNEL_NEW_INTEREST = "channel_new_interest"
        const val CHANNEL_NEW_MESSAGE = "channel_new_message"
        const val CHANNEL_POLL_CLOSING = "channel_poll_closing"
        const val CHANNEL_NEW_COMMENT = "channel_new_comment"
    }
}
