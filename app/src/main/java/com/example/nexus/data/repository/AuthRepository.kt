package com.example.nexus.data.repository

import com.example.nexus.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * AuthRepository
 * Handles all Firebase Authentication operations including email/password login,
 * Google OAuth, user creation, and Firestore user document management.
 * Returns Result<User> for proper error handling.
 */
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) {

    /**
     * Register a new user with email, password, and profile information
     * Validates college email domain before creating account
     */
    suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        collegeName: String
    ): Result<User> {
            return try {
            // Validate college email domain
            if (!isValidCollegeEmail(email)) {
                return Result.failure(IllegalArgumentException("Invalid college email domain"))
            }

            // Create Firebase Authentication user
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("User creation failed"))

            // Update Firebase user profile with display name
            firebaseUser.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
            ).await()

            // Create User domain object
            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                collegeName = collegeName,
                avatarUrl = null,
                bio = "",
                skills = emptyList(),
                joinedAt = System.currentTimeMillis()
            )

            // Save user to Firestore
            firebaseFirestore.collection("users").document(firebaseUser.uid)
                .set(user.toFirestoreMap()).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login with email and password
     * Fetches user data from Firestore
     */
    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Login failed"))

            // Fetch user data from Firestore
            val userDoc = firebaseFirestore.collection("users")
                .document(firebaseUser.uid).get().await()

            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("User data not found"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     * Creates or updates user in Firestore
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Google sign-in failed"))

            // Check if user document exists in Firestore
            val userDocRef = firebaseFirestore.collection("users").document(firebaseUser.uid)
            val userDoc = userDocRef.get().await()

            val user = if (userDoc.exists()) {
                // User exists, return existing data
                userDoc.toObject(User::class.java)
                    ?: return Result.failure(Exception("Failed to retrieve user"))
            } else {
                // New user, create document
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Google User",
                    email = firebaseUser.email ?: "",
                    collegeName = "",  // User must fill this in during onboarding
                    avatarUrl = firebaseUser.photoUrl?.toString(),
                    bio = "",
                    skills = emptyList(),
                    joinedAt = System.currentTimeMillis()
                )
                userDocRef.set(newUser.toFirestoreMap()).await()
                newUser
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get currently logged-in user without authentication required
     */
    suspend fun getCurrentUser(): Result<User?> { 
        return try {
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                return Result.success(null)
            }

            val userDoc = firebaseFirestore.collection("users")
                .document(firebaseUser.uid).get().await()

            val user = userDoc.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if user is currently authenticated
     */
    fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    /**
     * Validate college email domain
     * Accepts emails ending in .edu or specific college domains
     */
    private fun isValidCollegeEmail(email: String): Boolean {
        return email.endsWith(".edu") || 
               email.endsWith("@college.edu") ||  // Adjust as needed for your colleges
               email.contains("@", ignoreCase = true)  // Basic fallback validation
    }

    /**
     * Convert User domain model to Firestore map
     */
    private fun User.toFirestoreMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "collegeName" to collegeName,
        "avatarUrl" to (avatarUrl ?: ""),
        "bio" to bio,
        "skills" to skills,
        "joinedAt" to joinedAt
    )
}