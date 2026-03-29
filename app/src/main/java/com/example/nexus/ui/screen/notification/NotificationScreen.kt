package com.example.nexus.ui.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * NotificationScreen
 * Displays a local log of all push notifications grouped by type
 * Shows notification history with:
 * - Grouping by notification type (Interest, Message, Poll, Comment)
 * - Timestamp and message preview
 * - Options to delete individual or all notifications
 * - Empty state when no notifications
 */
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val groupedNotifications by viewModel.groupedNotifications.collectAsStateWithLifecycle()
    val emptyState by viewModel.isEmpty.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            NotificationScreenHeader(
                onClearAll = { viewModel.clearAllNotifications() }
            )

            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (emptyState) {
                // Empty state
                EmptyNotificationState()
            } else {
                // Notifications grouped by type
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    groupedNotifications.forEach { (type, notifications) ->
                        // Type header
                        item {
                            NotificationTypeHeader(notificationType = type)
                        }

                        // Notifications of this type
                        items(
                            items = notifications,
                            key = { notification -> notification.id }
                        ) { notification ->
                            NotificationItem(
                                notification = notification,
                                onDelete = { viewModel.deleteNotification(notification.id) }
                            )
                        }

                        // Divider
                        item {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Notification Screen Header
 */
@Composable
private fun NotificationScreenHeader(
    onClearAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            IconButton(onClick = onClearAll) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Clear all notifications",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Notification Type Header
 * Shows the notification type (e.g., "New Interests", "Messages")
 */
@Composable
private fun NotificationTypeHeader(notificationType: String) {
    val displayName = when (notificationType) {
        "new_interest" -> "New Interests"
        "new_message" -> "Messages"
        "poll_closing" -> "Poll Reminders"
        "new_comment" -> "New Comments"
        else -> "Notifications"
    }

    val backgroundColor = when (notificationType) {
        "new_interest" -> MaterialTheme.colorScheme.primaryContainer
        "new_message" -> MaterialTheme.colorScheme.tertiaryContainer
        "poll_closing" -> MaterialTheme.colorScheme.secondaryContainer
        "new_comment" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when (notificationType) {
                        "new_interest" -> MaterialTheme.colorScheme.primary
                        "new_message" -> MaterialTheme.colorScheme.tertiary
                        "poll_closing" -> MaterialTheme.colorScheme.secondary
                        "new_comment" -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Individual Notification Item
 */
@Composable
private fun NotificationItem(
    notification: NotificationData,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Notification title
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Notification body/message
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Timestamp
            Text(
                text = formatNotificationTime(notification.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete notification",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Empty Notifications State
 */
@Composable
private fun EmptyNotificationState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No Notifications Yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "When you get new messages, interests, or comments, they'll appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * Format notification timestamp
 * e.g., "2 minutes ago", "Today at 3:45 PM", "Yesterday"
 */
private fun formatNotificationTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - timestamp
    val diffSec = diffMs / 1000
    val diffMin = diffSec / 60
    val diffHour = diffMin / 60
    val diffDay = diffHour / 24

    return when {
        diffMin < 1 -> "just now"
        diffMin < 60 -> "${diffMin}m ago"
        diffHour < 24 -> "${diffHour}h ago"
        diffDay < 1 -> "today"
        diffDay == 1L -> "yesterday"
        diffDay < 7 -> "${diffDay}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(timestamp)
        }
    }
}

/**
 * NotificationData class for displaying in UI
 * Maps from Room entity to display model
 */
data class NotificationData(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val deepLinkDestination: String? = null,
    val deepLinkValue: String? = null
)
