package com.example.hanaparal.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

/**
 * RemoteConfigManager
 *
 * Manages Firebase Remote Config values for the Hanap Aral app.
 * Superusers can update these values from the Firebase console
 * without requiring an app update.
 *
 * Managed values:
 *  - max_members_per_group   : Max members allowed in a study group
 *  - group_creation_enabled  : Toggle to enable/disable group creation
 *  - announcement_header     : Global announcement text shown in HomeFragment
 *
 * Usage:
 *  1. Call RemoteConfigManager.init() once in MainActivity.onCreate()
 *  2. Fetch values with RemoteConfigManager.fetchAndActivate { ... }
 *  3. Read values anywhere using the getter functions below
 */
object RemoteConfigManager {

    // -------------------------------------------------------------------------
    // Keys — must match exactly what is set in Firebase Console
    // -------------------------------------------------------------------------
    const val KEY_MAX_MEMBERS         = "max_members_per_group"
    const val KEY_GROUP_TOGGLE        = "group_creation_enabled"
    const val KEY_ANNOUNCEMENT_HEADER = "announcement_header"

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    // -------------------------------------------------------------------------
    // Init — call this once in MainActivity.onCreate()
    // -------------------------------------------------------------------------
    fun init() {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // set to 0 for testing
            .build()

        remoteConfig.setConfigSettingsAsync(settings)

        val defaults = mapOf(
            KEY_MAX_MEMBERS         to 10L,
            KEY_GROUP_TOGGLE        to true,
            KEY_ANNOUNCEMENT_HEADER to ""
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    // -------------------------------------------------------------------------
    // Fetch & Activate — call this in MainActivity or HomeFragment
    // -------------------------------------------------------------------------
    fun fetchAndActivate(onComplete: (success: Boolean) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /** Maximum number of members allowed per study group (default: 10) */
    fun getMaxMembers(): Int {
        return remoteConfig.getLong(KEY_MAX_MEMBERS).toInt()
    }

    /** Whether study group creation is currently enabled (default: true) */
    fun isGroupCreationEnabled(): Boolean {
        return remoteConfig.getBoolean(KEY_GROUP_TOGGLE)
    }

    /** Global announcement header text shown in HomeFragment (default: empty) */
    fun getAnnouncementHeader(): String {
        return remoteConfig.getString(KEY_ANNOUNCEMENT_HEADER)
    }
}