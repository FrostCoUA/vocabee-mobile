package com.vocabee.android.feature.vocabulary.data.preferences

/**
 * Persistent key-value bag for app-level user choices that need to survive a
 * relaunch but don't belong in Room (which is reserved for synced vocabulary
 * content).
 *
 * Right now we store:
 *  - whether the user has finished the first-launch flow (splash → onboarding
 *    → auth → language picker). If `true` we skip straight to Main on next
 *    launch.
 *  - the two language codes the user picked. Defaults restore from here on
 *    every launch so the dictionaries default to the same pair every time.
 *  - whether the app should render in dark theme.
 *  - the user's local coin balance used for rewarded-ad translation credits.
 *  - auth/session and sync cursors.
 *
 * The Android implementation is backed by SharedPreferences; tests and
 * previews use [InMemoryPreferencesManager].
 */
interface PreferencesManager {
    var hasCompletedOnboarding: Boolean

    /** ISO-639 code of the user's speaking (known) language, or `null` when unset. */
    var userLanguageCode: String?

    /** ISO-639 code of the language the user is learning, or `null` when unset. */
    var learningLanguageCode: String?

    var darkThemeEnabled: Boolean

    var beeBalance: Int

    var accessToken: String?

    var refreshToken: String?

    var currentUserId: String?

    var lastAuthenticatedUserId: String?

    var lastSyncAt: String?

    var localRevisionEpochMillis: Long
}

/**
 * Volatile fallback used by Compose previews and unit tests that don't have
 * a real Android Context (so SharedPreferences is unavailable). Behaves like
 * a clean install — never reports onboarding as completed.
 */
class InMemoryPreferencesManager : PreferencesManager {
    override var hasCompletedOnboarding: Boolean = false
    override var userLanguageCode: String? = null
    override var learningLanguageCode: String? = null
    override var darkThemeEnabled: Boolean = false
    override var beeBalance: Int = 50
    override var accessToken: String? = null
    override var refreshToken: String? = null
    override var currentUserId: String? = null
    override var lastAuthenticatedUserId: String? = null
    override var lastSyncAt: String? = null
    override var localRevisionEpochMillis: Long = 0L
}
