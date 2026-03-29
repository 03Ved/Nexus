package com.example.nexus.data.repository

import android.util.Log
import com.example.nexus.domain.model.Chat
import com.example.nexus.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query.Direction
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {

    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    /**
     * Get all chats for current user — excludes deleted chats
     */
    fun getAllChatsForUser(): Flow<Result<List<Chat>>> = callbackFlow {
        if (currentUserId.isEmpty()) {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val listener = firebaseFirestore.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching chats: ${error.message}")
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)
                    }.filter { !it.isDeleted }
                    trySend(Result.success(chats))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get chats owned by current user (admin)
     */
    fun getOwnedChats(): Flow<Result<List<Chat>>> = callbackFlow {
        if (currentUserId.isEmpty()) {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val listener = firebaseFirestore.collection("chats")
            .whereEqualTo("adminId", currentUserId)
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)
                    }.filter { !it.isDeleted }
                    trySend(Result.success(chats))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get chats user joined (not admin)
     */
    fun getJoinedChats(): Flow<Result<List<Chat>>> = callbackFlow {
        if (currentUserId.isEmpty()) {
            trySend(Result.failure(Exception("User not authenticated")))
            close()
            return@callbackFlow
        }

        val listener = firebaseFirestore.collection("chats")
            .whereArrayContains("participantIds", currentUserId)
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)
                    }.filter { !it.isDeleted && it.adminId != currentUserId }
                    trySend(Result.success(chats))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get messages for a chat in real time
     */
    fun getMessagesForChat(chatId: String): Flow<Result<List<Message>>> = callbackFlow {
        val listener = firebaseFirestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("sentAt", Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Message::class.java)
                    }
                    trySend(Result.success(messages))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Send a message in a chat
     */
    suspend fun sendMessage(
        chatId: String,
        text: String,
        localImagePath: String? = null
    ): Result<Message> {
        return try {
            if (text.isBlank() && localImagePath == null) {
                return Result.failure(Exception("Message cannot be empty"))
            }
            val messageId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val message = Message(
                id = messageId,
                chatId = chatId,
                senderId = currentUserId,
                text = text,
                localImagePath = localImagePath,
                sentAt = timestamp
            )
            firebaseFirestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .await()
            firebaseFirestore.collection("chats")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to (text.takeIf { it.isNotBlank() } ?: "[Image]"),
                        "lastMessageAt" to timestamp
                    )
                ).await()
            Result.success(message)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a group chat when a post is created
     * Called automatically from ComposePostViewModel
     */
    suspend fun createGroupChatForPost(
        postId: String,
        postTitle: String
    ): Result<Chat> {
        return try {
            val chatId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val chat = Chat(
                id = chatId,
                postId = postId,
                name = postTitle,
                adminId = currentUserId,
                participantIds = listOf(currentUserId),
                lastMessage = "",
                lastMessageAt = timestamp,
                isGroupChat = true,
                isDeleted = false,
                createdAt = timestamp
            )
            firebaseFirestore.collection("chats")
                .document(chatId)
                .set(chat)
                .await()
            Result.success(chat)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating group chat: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Join a chat when user clicks "I'm In" on a post
     * Finds the chat linked to the post and adds current user
     */
    suspend fun joinChatForPost(postId: String): Result<Chat> {
        return try {
            val snapshot = firebaseFirestore.collection("chats")
                .whereEqualTo("postId", postId)
                .get()
                .await()

            val chatDoc = snapshot.documents.firstOrNull()
                ?: return Result.failure(Exception("Chat not found for this post"))

            val chat = chatDoc.toObject(Chat::class.java)
                ?: return Result.failure(Exception("Failed to parse chat"))

            if (currentUserId in chat.participantIds) {
                return Result.success(chat)
            }

            val updatedParticipants = chat.participantIds + currentUserId
            firebaseFirestore.collection("chats")
                .document(chat.id)
                .update("participantIds", updatedParticipants)
                .await()

            Result.success(chat.copy(participantIds = updatedParticipants))
        } catch (e: Exception) {
            Log.e(TAG, "Error joining chat: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Check if current user has already joined the chat for a post
     */
    suspend fun hasUserJoinedChat(postId: String): Result<Boolean> {
        return try {
            val snapshot = firebaseFirestore.collection("chats")
                .whereEqualTo("postId", postId)
                .get()
                .await()

            val chat = snapshot.documents.firstOrNull()
                ?.toObject(Chat::class.java)
                ?: return Result.success(false)

            Result.success(currentUserId in chat.participantIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leave a chat — only available to members, not admin
     */
    suspend fun leaveChat(chatId: String): Result<Unit> {
        return try {
            val doc = firebaseFirestore.collection("chats")
                .document(chatId)
                .get()
                .await()

            val chat = doc.toObject(Chat::class.java)
                ?: return Result.failure(Exception("Chat not found"))

            if (chat.adminId == currentUserId) {
                return Result.failure(Exception("Admin cannot leave — use delete instead"))
            }

            val updatedParticipants = chat.participantIds.filter { it != currentUserId }
            firebaseFirestore.collection("chats")
                .document(chatId)
                .update("participantIds", updatedParticipants)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error leaving chat: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Delete a chat — only available to admin
     * Sets isDeleted = true so it disappears from all participants' views
     */
    suspend fun deleteChat(chatId: String): Result<Unit> {
        return try {
            val doc = firebaseFirestore.collection("chats")
                .document(chatId)
                .get()
                .await()

            val chat = doc.toObject(Chat::class.java)
                ?: return Result.failure(Exception("Chat not found"))

            if (chat.adminId != currentUserId) {
                return Result.failure(Exception("Only admin can delete this group"))
            }

            firebaseFirestore.collection("chats")
                .document(chatId)
                .update("isDeleted", true)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting chat: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get chat by ID
     */
    suspend fun getChatById(chatId: String): Result<Chat> {
        return try {
            val doc = firebaseFirestore.collection("chats")
                .document(chatId)
                .get()
                .await()
            val chat = doc.toObject(Chat::class.java)
                ?: return Result.failure(Exception("Chat not found"))
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "ChatRepository"
    }
}