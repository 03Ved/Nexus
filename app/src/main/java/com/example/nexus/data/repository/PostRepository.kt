package com.example.nexus.data.repository

import com.example.nexus.data.local.dao.PostDao
import com.example.nexus.data.local.entity.PostEntity
import com.example.nexus.domain.model.IntentType
import com.example.nexus.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import com.example.nexus.domain.model.Comment

class PostRepository @Inject constructor(
    private val firebaseFirestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val postDao: PostDao
) {

    fun getAllPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val listener = firebaseFirestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Post::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            posts.forEach { post ->
                                postDao.insertPost(post.toEntity())
                            }
                        } catch (e: Exception) { }
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { listener.remove() }
    }

    fun getPostsByType(intentType: IntentType): Flow<Result<List<Post>>> = callbackFlow {
        val listener = firebaseFirestore.collection("posts")
            .whereEqualTo("type", intentType.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Post::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.success(posts))
                }
            }
        awaitClose { listener.remove() }
    }

    fun getComments(postId: String): Flow<Result<List<Comment>>> = callbackFlow {
        val listener = firebaseFirestore.collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(Comment::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.success(comments))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addComment(postId: String, text: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("User not authenticated"))
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                postId = postId,
                authorId = currentUser.uid,
                authorName = currentUser.displayName ?: "Anonymous",
                text = text,
                createdAt = System.currentTimeMillis()
            )
            firebaseFirestore.collection("posts")
                .document(postId)
                .collection("comments")
                .document(comment.id)
                .set(comment)
                .await()
            firebaseFirestore.collection("posts")
                .document(postId)
                .update("commentCount",
                    com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(post: Post): Result<String> = withContext(Dispatchers.IO) {
        try {
            postDao.insertPost(post.toEntity())
            val docRef = firebaseFirestore.collection("posts").document(post.id)
            docRef.set(post).await()
            postDao.markPostsAsSynced(listOf(post.id), System.currentTimeMillis())
            Result.success(post.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markInterested(postId: String): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            firebaseFirestore.collection("posts").document(postId)
                .update("interestedUserIds",
                    com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unmarkInterested(postId: String): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            firebaseFirestore.collection("posts").document(postId)
                .update("interestedUserIds",
                    com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleInterested(postId: String): Result<Boolean> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            val postDoc = firebaseFirestore.collection("posts")
                .document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
                ?: return Result.failure(Exception("Post not found"))
            val isInterested = post.interestedUserIds.contains(currentUserId)
            if (isInterested) {
                unmarkInterested(postId)
                Result.success(false)
            } else {
                markInterested(postId)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementReactCount(postId: String): Result<Unit> {
        return try {
            firebaseFirestore.collection("posts").document(postId)
                .update("reactCount",
                    com.google.firebase.firestore.FieldValue.increment(1))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val postDoc = firebaseFirestore.collection("posts")
                .document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
                ?: return Result.failure(Exception("Post not found"))
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val currentUserId = firebaseAuth.currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            val postDoc = firebaseFirestore.collection("posts")
                .document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
                ?: return Result.failure(Exception("Post not found"))
            if (post.authorId != currentUserId) {
                return Result.failure(Exception("Only post author can delete"))
            }
            firebaseFirestore.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUnsyncedPosts(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val unsyncedPosts = postDao.getUnsyncedPosts()
            var successCount = 0
            for (entity in unsyncedPosts) {
                try {
                    val post = entity.toPost()
                    firebaseFirestore.collection("posts")
                        .document(entity.id).set(post).await()
                    postDao.markPostsAsSynced(
                        listOf(entity.id),
                        System.currentTimeMillis()
                    )
                    successCount++
                } catch (e: Exception) {
                    postDao.markPostSyncError(entity.id, e.message ?: "Sync failed")
                }
            }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearLocalCache(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            postDao.clearAllPosts()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpiredPosts(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val deletedCount = postDao.deleteExpiredPosts(currentTime)
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun PostEntity.toPost(): Post = Post(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorAvatar = authorAvatar,
        type = type,
        title = title,
        description = description,
        localImagePath = localImagePath,
        pollOptions = pollOptions,
        tags = tags,
        latitude = latitude,
        longitude = longitude,
        locationLabel = locationLabel,
        expiresAt = expiresAt,
        createdAt = createdAt,
        reactCount = reactCount,
        commentCount = commentCount,
        interestedUserIds = interestedUserIds,
        likedUserIds = likedUserIds
    )

    private fun Post.toEntity(): PostEntity = PostEntity(
        id = id,
        authorId = authorId,
        authorName = authorName,
        authorAvatar = authorAvatar,
        type = type,
        title = title,
        description = description,
        localImagePath = localImagePath,
        pollOptions = pollOptions,
        tags = tags,
        latitude = latitude,
        longitude = longitude,
        locationLabel = locationLabel,
        expiresAt = expiresAt,
        createdAt = createdAt,
        reactCount = reactCount,
        commentCount = commentCount,
        interestedUserIds = interestedUserIds,
        isSyncedWithFirestore = false,
        lastSyncedAt = null,
        syncError = null,
        likedUserIds = likedUserIds
    )
}