package com.vocabee.android.feature.vocabulary.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed implementation of [PreferencesManager]. We use a
 * dedicated file (`vocabee_prefs`) so this never collides with Activity-level
 * defaults that future libraries might write into the default file.
 *
 * All operations are synchronous — these prefs are tiny and the
 * read happens once during app startup off the splash screen, so apply()
 * is enough; no need for a Flow-based reactive layer.
 */
@Singleton
class AndroidPreferencesManager @Inject constructor(
    @ApplicationContext context: Context,
) : PreferencesManager {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit { putBoolean(KEY_ONBOARDED, value) }

    override var userLanguageCode: String?
        get() = prefs.getString(KEY_USER_LANG, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_USER_LANG) else putString(KEY_USER_LANG, value)
        }

    override var learningLanguageCode: String?
        get() = prefs.getString(KEY_LEARNING_LANG, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LEARNING_LANG) else putString(KEY_LEARNING_LANG, value)
        }

    override var darkThemeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_THEME, value) }

    override var beeBalance: Int
        get() = prefs.getInt(KEY_BEE_BALANCE, DEFAULT_BEE_BALANCE)
        set(value) = prefs.edit { putInt(KEY_BEE_BALANCE, value.coerceAtLeast(0)) }

    override var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_ACCESS_TOKEN) else putString(KEY_ACCESS_TOKEN, value)
        }

    override var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_REFRESH_TOKEN) else putString(KEY_REFRESH_TOKEN, value)
        }

    override var currentUserId: String?
        get() = prefs.getString(KEY_CURRENT_USER_ID, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_CURRENT_USER_ID) else putString(KEY_CURRENT_USER_ID, value)
        }

    override var lastAuthenticatedUserId: String?
        get() = prefs.getString(KEY_LAST_AUTH_USER_ID, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_AUTH_USER_ID) else putString(KEY_LAST_AUTH_USER_ID, value)
        }

    override var lastSyncAt: String?
        get() = prefs.getString(KEY_LAST_SYNC_AT, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_SYNC_AT) else putString(KEY_LAST_SYNC_AT, value)
        }

    override var localRevisionEpochMillis: Long
        get() = prefs.getLong(KEY_LOCAL_REVISION_EPOCH_MILLIS, 0L)
        set(value) = prefs.edit { putLong(KEY_LOCAL_REVISION_EPOCH_MILLIS, value.coerceAtLeast(0L)) }

    private companion object {
        const val PREFS_NAME = "vocabee_prefs"
        const val KEY_ONBOARDED = "has_completed_onboarding"
        const val KEY_USER_LANG = "user_language_code"
        const val KEY_LEARNING_LANG = "learning_language_code"
        const val KEY_DARK_THEME = "dark_theme_enabled"
        const val KEY_BEE_BALANCE = "bee_balance"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        const val KEY_LAST_AUTH_USER_ID = "last_authenticated_user_id"
        const val KEY_LAST_SYNC_AT = "last_sync_at"
        const val KEY_LOCAL_REVISION_EPOCH_MILLIS = "local_revision_epoch_millis"
        const val DEFAULT_BEE_BALANCE = 50
    }
}
