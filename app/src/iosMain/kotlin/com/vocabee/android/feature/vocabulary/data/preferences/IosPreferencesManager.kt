package com.vocabee.android.feature.vocabulary.data.preferences

import platform.Foundation.NSUserDefaults

/**
 * NSUserDefaults-backed [PreferencesManager] — the iOS twin of
 * AndroidPreferencesManager (SharedPreferences).
 */
class IosPreferencesManager : PreferencesManager {
    private val defaults = NSUserDefaults.standardUserDefaults

    private fun string(key: String): String? = defaults.stringForKey(key)

    private fun setString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }

    override var hasCompletedOnboarding: Boolean
        get() = defaults.boolForKey(KEY_ONBOARDING)
        set(value) = defaults.setBool(value, KEY_ONBOARDING)

    override var userLanguageCode: String?
        get() = string(KEY_USER_LANG)
        set(value) = setString(KEY_USER_LANG, value)

    override var learningLanguageCode: String?
        get() = string(KEY_LEARNING_LANG)
        set(value) = setString(KEY_LEARNING_LANG, value)

    override var darkThemeEnabled: Boolean
        get() = defaults.boolForKey(KEY_DARK_THEME)
        set(value) = defaults.setBool(value, KEY_DARK_THEME)

    override var beeBalance: Int
        get() = if (defaults.objectForKey(KEY_BEE_BALANCE) != null) {
            defaults.integerForKey(KEY_BEE_BALANCE).toInt()
        } else {
            DEFAULT_BEE_BALANCE
        }
        set(value) = defaults.setInteger(value.toLong(), KEY_BEE_BALANCE)

    override var accessToken: String?
        get() = string(KEY_ACCESS_TOKEN)
        set(value) = setString(KEY_ACCESS_TOKEN, value)

    override var refreshToken: String?
        get() = string(KEY_REFRESH_TOKEN)
        set(value) = setString(KEY_REFRESH_TOKEN, value)

    override var currentUserId: String?
        get() = string(KEY_CURRENT_USER)
        set(value) = setString(KEY_CURRENT_USER, value)

    override var lastAuthenticatedUserId: String?
        get() = string(KEY_LAST_AUTH_USER)
        set(value) = setString(KEY_LAST_AUTH_USER, value)

    override var lastSyncAt: String?
        get() = string(KEY_LAST_SYNC_AT)
        set(value) = setString(KEY_LAST_SYNC_AT, value)

    override var localRevisionEpochMillis: Long
        get() = defaults.integerForKey(KEY_LOCAL_REVISION)
        set(value) = defaults.setInteger(value, KEY_LOCAL_REVISION)

    override var streakDays: Int
        get() = defaults.integerForKey(KEY_STREAK_DAYS).toInt()
        set(value) = defaults.setInteger(value.coerceAtLeast(0).toLong(), KEY_STREAK_DAYS)

    override var lastActiveDayStartMillis: Long
        get() = defaults.integerForKey(KEY_LAST_ACTIVE_DAY_START)
        set(value) = defaults.setInteger(value.coerceAtLeast(0L), KEY_LAST_ACTIVE_DAY_START)

    override var practiceRoundsCompleted: Int
        get() = defaults.integerForKey(KEY_PRACTICE_ROUNDS).toInt()
        set(value) = defaults.setInteger(value.coerceAtLeast(0).toLong(), KEY_PRACTICE_ROUNDS)

    private companion object {
        const val KEY_ONBOARDING = "hasCompletedOnboarding"
        const val KEY_USER_LANG = "userLanguageCode"
        const val KEY_LEARNING_LANG = "learningLanguageCode"
        const val KEY_DARK_THEME = "darkThemeEnabled"
        const val KEY_BEE_BALANCE = "beeBalance"
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_REFRESH_TOKEN = "refreshToken"
        const val KEY_CURRENT_USER = "currentUserId"
        const val KEY_LAST_AUTH_USER = "lastAuthenticatedUserId"
        const val KEY_LAST_SYNC_AT = "lastSyncAt"
        const val KEY_LOCAL_REVISION = "localRevisionEpochMillis"
        const val KEY_STREAK_DAYS = "streakDays"
        const val KEY_LAST_ACTIVE_DAY_START = "lastActiveDayStartMillis"
        const val KEY_PRACTICE_ROUNDS = "practiceRoundsCompleted"
        const val DEFAULT_BEE_BALANCE = 50
    }
}
