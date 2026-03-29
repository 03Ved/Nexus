package com.example.nexus.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.nexus.domain.model.IntentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserPrefsDataStore
 * Manages user preferences using DataStore
 * Handles app settings, user preferences, and cached data
 */
@Singleton
class UserPrefsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

    companion object {
        // Authentication
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")

        // App Settings
        val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LOCATION_SERVICES_ENABLED = booleanPreferencesKey("location_services_enabled")

        // Feed Preferences
        val DEFAULT_FEED_FILTER = stringPreferencesKey("default_feed_filter") // IntentType name
        val SHOW_EXPIRED_POSTS = booleanPreferencesKey("show_expired_posts")
        val POSTS_PER_PAGE = intPreferencesKey("posts_per_page")

        // Location & Campus
        val LAST_KNOWN_LATITUDE = doublePreferencesKey("last_known_latitude")
        val LAST_KNOWN_LONGITUDE = doublePreferencesKey("last_known_longitude")
        val CAMPUS_LOCATION_LABEL = stringPreferencesKey("campus_location_label")

        // Privacy & Security
        val PROFILE_VISIBILITY = stringPreferencesKey("profile_visibility") // "public", "friends", "private"
        val SHOW_ONLINE_STATUS = booleanPreferencesKey("show_online_status")
        val DATA_SYNC_ENABLED = booleanPreferencesKey("data_sync_enabled")

        // Performance & Cache
        val CACHE_SIZE_MB = intPreferencesKey("cache_size_mb")
        val AUTO_CLEAR_CACHE = booleanPreferencesKey("auto_clear_cache")
        val OFFLINE_MODE_ENABLED = booleanPreferencesKey("offline_mode_enabled")

        // Draft & Compose
        val AUTO_SAVE_DRAFTS = booleanPreferencesKey("auto_save_drafts")
        val DRAFT_RETENTION_DAYS = intPreferencesKey("draft_retention_days")

        // Chat & Messaging
        val MESSAGE_NOTIFICATIONS = booleanPreferencesKey("message_notifications")
        val SHOW_MESSAGE_PREVIEWS = booleanPreferencesKey("show_message_previews")

        // Onboarding & Tutorial
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val HAS_SEEN_FEED_TUTORIAL = booleanPreferencesKey("has_seen_feed_tutorial")
        val HAS_SEEN_COMPOSE_TUTORIAL = booleanPreferencesKey("has_seen_compose_tutorial")

        // App Usage Stats
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val LAST_APP_OPEN = longPreferencesKey("last_app_open")
        val TOTAL_POSTS_VIEWED = intPreferencesKey("total_posts_viewed")
        val TOTAL_MESSAGES_SENT = intPreferencesKey("total_messages_sent")
    }

    // Authentication Preferences
    val userId: Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userEmail: Flow<String?> = context.dataStore.data.map { it[USER_EMAIL] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    // App Settings
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val locationServicesEnabled: Flow<Boolean> = context.dataStore.data.map { it[LOCATION_SERVICES_ENABLED] ?: true }

    // Feed Preferences
    val defaultFeedFilter: Flow<IntentType?> = context.dataStore.data.map {
        it[DEFAULT_FEED_FILTER]?.let { filterName ->
            try {
                IntentType.valueOf(filterName)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
    val showExpiredPosts: Flow<Boolean> = context.dataStore.data.map { it[SHOW_EXPIRED_POSTS] ?: false }
    val postsPerPage: Flow<Int> = context.dataStore.data.map { it[POSTS_PER_PAGE] ?: 20 }

    // Location
    val lastKnownLatitude: Flow<Double?> = context.dataStore.data.map { it[LAST_KNOWN_LATITUDE] }
    val lastKnownLongitude: Flow<Double?> = context.dataStore.data.map { it[LAST_KNOWN_LONGITUDE] }
    val campusLocationLabel: Flow<String?> = context.dataStore.data.map { it[CAMPUS_LOCATION_LABEL] }

    // Privacy
    val profileVisibility: Flow<String> = context.dataStore.data.map { it[PROFILE_VISIBILITY] ?: "public" }
    val showOnlineStatus: Flow<Boolean> = context.dataStore.data.map { it[SHOW_ONLINE_STATUS] ?: true }
    val dataSyncEnabled: Flow<Boolean> = context.dataStore.data.map { it[DATA_SYNC_ENABLED] ?: true }

    // Performance
    val cacheSizeMb: Flow<Int> = context.dataStore.data.map { it[CACHE_SIZE_MB] ?: 100 }
    val autoClearCache: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CLEAR_CACHE] ?: false }
    val offlineModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[OFFLINE_MODE_ENABLED] ?: false }

    // Draft & Compose
    val autoSaveDrafts: Flow<Boolean> = context.dataStore.data.map { it[AUTO_SAVE_DRAFTS] ?: true }
    val draftRetentionDays: Flow<Int> = context.dataStore.data.map { it[DRAFT_RETENTION_DAYS] ?: 30 }

    // Chat
    val messageNotifications: Flow<Boolean> = context.dataStore.data.map { it[MESSAGE_NOTIFICATIONS] ?: true }
    val showMessagePreviews: Flow<Boolean> = context.dataStore.data.map { it[SHOW_MESSAGE_PREVIEWS] ?: true }

    // Onboarding
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }
    val hasSeenFeedTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_FEED_TUTORIAL] ?: false }
    val hasSeenComposeTutorial: Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_COMPOSE_TUTORIAL] ?: false }

    // Stats
    val appOpenCount: Flow<Int> = context.dataStore.data.map { it[APP_OPEN_COUNT] ?: 0 }
    val lastAppOpen: Flow<Long?> = context.dataStore.data.map { it[LAST_APP_OPEN] }
    val totalPostsViewed: Flow<Int> = context.dataStore.data.map { it[TOTAL_POSTS_VIEWED] ?: 0 }
    val totalMessagesSent: Flow<Int> = context.dataStore.data.map { it[TOTAL_MESSAGES_SENT] ?: 0 }

    // Authentication Methods
    suspend fun saveUserSession(userId: String, email: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID] = userId
            prefs[USER_EMAIL] = email
            prefs[USER_NAME] = name
            prefs[IS_LOGGED_IN] = true
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID)
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_NAME)
            prefs[IS_LOGGED_IN] = false
        }
    }

    // Settings Methods
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setLocationServicesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[LOCATION_SERVICES_ENABLED] = enabled }
    }

    suspend fun setDefaultFeedFilter(filter: IntentType?) {
        context.dataStore.edit {
            if (filter != null) {
                it[DEFAULT_FEED_FILTER] = filter.name
            } else {
                it.remove(DEFAULT_FEED_FILTER)
            }
        }
    }

    suspend fun setShowExpiredPosts(show: Boolean) {
        context.dataStore.edit { it[SHOW_EXPIRED_POSTS] = show }
    }

    suspend fun setPostsPerPage(count: Int) {
        context.dataStore.edit { it[POSTS_PER_PAGE] = count }
    }

    suspend fun saveLastKnownLocation(lat: Double?, lng: Double?, label: String?) {
        context.dataStore.edit { prefs ->
            if (lat != null && lng != null) {
                prefs[LAST_KNOWN_LATITUDE] = lat
                prefs[LAST_KNOWN_LONGITUDE] = lng
            } else {
                prefs.remove(LAST_KNOWN_LATITUDE)
                prefs.remove(LAST_KNOWN_LONGITUDE)
            }
            if (label != null) {
                prefs[CAMPUS_LOCATION_LABEL] = label
            } else {
                prefs.remove(CAMPUS_LOCATION_LABEL)
            }
        }
    }

    suspend fun setProfileVisibility(visibility: String) {
        context.dataStore.edit { it[PROFILE_VISIBILITY] = visibility }
    }

    suspend fun setShowOnlineStatus(show: Boolean) {
        context.dataStore.edit { it[SHOW_ONLINE_STATUS] = show }
    }

    suspend fun setDataSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DATA_SYNC_ENABLED] = enabled }
    }

    suspend fun setCacheSizeMb(size: Int) {
        context.dataStore.edit { it[CACHE_SIZE_MB] = size }
    }

    suspend fun setAutoClearCache(autoClear: Boolean) {
        context.dataStore.edit { it[AUTO_CLEAR_CACHE] = autoClear }
    }

    suspend fun setOfflineModeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[OFFLINE_MODE_ENABLED] = enabled }
    }

    suspend fun setAutoSaveDrafts(autoSave: Boolean) {
        context.dataStore.edit { it[AUTO_SAVE_DRAFTS] = autoSave }
    }

    suspend fun setDraftRetentionDays(days: Int) {
        context.dataStore.edit { it[DRAFT_RETENTION_DAYS] = days }
    }

    suspend fun setMessageNotifications(enabled: Boolean) {
        context.dataStore.edit { it[MESSAGE_NOTIFICATIONS] = enabled }
    }

    suspend fun setShowMessagePreviews(show: Boolean) {
        context.dataStore.edit { it[SHOW_MESSAGE_PREVIEWS] = show }
    }

    // Onboarding Methods
    suspend fun markOnboardingSeen() {
        context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true }
    }

    suspend fun markFeedTutorialSeen() {
        context.dataStore.edit { it[HAS_SEEN_FEED_TUTORIAL] = true }
    }

    suspend fun markComposeTutorialSeen() {
        context.dataStore.edit { it[HAS_SEEN_COMPOSE_TUTORIAL] = true }
    }

    // Stats Methods
    suspend fun incrementAppOpenCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[APP_OPEN_COUNT] ?: 0
            prefs[APP_OPEN_COUNT] = current + 1
            prefs[LAST_APP_OPEN] = System.currentTimeMillis()
        }
    }

    suspend fun incrementPostsViewed() {
        context.dataStore.edit { prefs ->
            val current = prefs[TOTAL_POSTS_VIEWED] ?: 0
            prefs[TOTAL_POSTS_VIEWED] = current + 1
        }
    }

    suspend fun incrementMessagesSent() {
        context.dataStore.edit { prefs ->
            val current = prefs[TOTAL_MESSAGES_SENT] ?: 0
            prefs[TOTAL_MESSAGES_SENT] = current + 1
        }
    }

    // Utility Methods
    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }

    // Synchronous getters for immediate access (use sparingly)
    fun getUserIdSync(): String? = runBlocking { userId.first() }
    fun getUserEmailSync(): String? = runBlocking { userEmail.first() }
    fun isLoggedInSync(): Boolean = runBlocking { isLoggedIn.first() }
    fun getThemeModeSync(): String = runBlocking { themeMode.first() }
    fun getNotificationsEnabledSync(): Boolean = runBlocking { notificationsEnabled.first() }
    fun getOfflineModeEnabledSync(): Boolean = runBlocking { offlineModeEnabled.first() }
}
