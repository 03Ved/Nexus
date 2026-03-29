package com.example.nexus.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.nexus.MainActivity
import com.example.nexus.R

/**
 * NotificationHelper
 * Helper object for building NotificationCompat builders for different notification types
 * Handles deep linking to appropriate screens based on notification type
 */
object NotificationHelper {

    /**
     * Build notification for new_interest type
     * Deep links to post detail screen
     */
    fun buildNewInterestNotification(
        context: Context,
        title: String,
        body: String,
        postId: String,
        userName: String
    ): NotificationCompat.Builder {
        val deepLinkIntent = createDeepLinkIntent(
            context = context,
            destination = "post_detail",
            extraData = mapOf(
                "postId" to postId,
                "userName" to userName,
                "notification_type" to "new_interest"
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            "new_interest_$postId".hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NexusFCMService.CHANNEL_NEW_INTEREST)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Build notification for new_message type
     * Deep links to chat detail screen with HIGH priority
     */
    fun buildNewMessageNotification(
        context: Context,
        title: String,
        body: String,
        chatId: String,
        senderId: String,
        senderName: String
    ): NotificationCompat.Builder {
        val deepLinkIntent = createDeepLinkIntent(
            context = context,
            destination = "chat_detail",
            extraData = mapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "senderName" to senderName,
                "notification_type" to "new_message"
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            "new_message_$chatId".hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NexusFCMService.CHANNEL_NEW_MESSAGE)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 250, 250, 250))
    }

    /**
     * Build notification for poll_closing type
     * Deep links to post detail screen with poll
     */
    fun buildPollClosingNotification(
        context: Context,
        title: String,
        body: String,
        postId: String,
        pollQuestion: String
    ): NotificationCompat.Builder {
        val deepLinkIntent = createDeepLinkIntent(
            context = context,
            destination = "post_detail",
            extraData = mapOf(
                "postId" to postId,
                "pollQuestion" to pollQuestion,
                "notification_type" to "poll_closing"
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            "poll_closing_$postId".hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NexusFCMService.CHANNEL_POLL_CLOSING)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Build notification for new_comment type
     * Deep links to post detail screen with comments
     */
    fun buildNewCommentNotification(
        context: Context,
        title: String,
        body: String,
        postId: String,
        commenterId: String,
        commenterName: String
    ): NotificationCompat.Builder {
        val deepLinkIntent = createDeepLinkIntent(
            context = context,
            destination = "post_detail",
            extraData = mapOf(
                "postId" to postId,
                "commenterId" to commenterId,
                "commenterName" to commenterName,
                "notification_type" to "new_comment"
            )
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            "new_comment_$postId".hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NexusFCMService.CHANNEL_NEW_COMMENT)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Generic notification builder
     * Used as fallback for unknown notification types
     */
    fun buildGenericNotification(
        context: Context,
        title: String,
        body: String
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", "generic")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "generic".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NexusFCMService.CHANNEL_NEW_INTEREST)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    /**
     * Create a deep link intent based on destination
     * Routes to appropriate screen with navigation parameters
     */
    private fun createDeepLinkIntent(
        context: Context,
        destination: String,
        extraData: Map<String, String>
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", destination)
            
            // Add all extra data to intent
            extraData.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
    }

    /**
     * Build stylized notification with action buttons (future enhancement)
     */
    fun buildNotificationWithActions(
        context: Context,
        builder: NotificationCompat.Builder,
        actions: List<NotificationAction>
    ): NotificationCompat.Builder {
        actions.forEach { action ->
            val actionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", action.name)
                action.extraData.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            val actionPendingIntent = PendingIntent.getActivity(
                context,
                action.name.hashCode(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(0, action.name, actionPendingIntent)
        }

        return builder
    }

    /**
     * Data class for notification action buttons
     */
    data class NotificationAction(
        val name: String,
        val extraData: Map<String, String> = emptyMap()
    )
}
