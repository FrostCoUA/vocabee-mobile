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

    /** Per-user server cursor. A missing value intentionally forces a full pull. */
    fun lastSyncAt(userKey: String): String?

    fun setLastSyncAt(userKey: String, value: String?)

    /** Per-user dirty/change token used to reject stale sync responses. */
    fun localRevisionEpochMillis(userKey: String): Long

    fun setLocalRevisionEpochMillis(userKey: String, value: Long)

    /** Last lexicon details schema successfully applied to this user's local snapshots. */
    fun appliedLexiconSchemaVersion(userKey: String): Int

    fun setAppliedLexiconSchemaVersion(userKey: String, version: Int)

    /** Стрік активності в днях поспіль; 0 = ще жодного зафіксованого дня. */
    var streakDays: Int

    /** Локальна північ (epoch millis) останнього дня з активністю — база для стріку. */
    var lastActiveDayStartMillis: Long

    /** Скільки раундів тренувань завершено на цьому пристрої (класика + контекст). */
    var practiceRoundsCompleted: Int
}

/**
 * Volatile fallback used by Compose previews and unit tests that don't have
 * a real Android Context (so SharedPreferences is unavailable). Behaves like
 * a clean install — never reports onboarding as completed.
 */
class InMemoryPreferencesManager : PreferencesManager {
    private val lastSyncAtByUser = mutableMapOf<String, String>()
    private val localRevisionByUser = mutableMapOf<String, Long>()
    private val appliedLexiconSchemaVersions = mutableMapOf<String, Int>()

    override var hasCompletedOnboarding: Boolean = false
    override var userLanguageCode: String? = null
    override var learningLanguageCode: String? = null
    override var darkThemeEnabled: Boolean = false
    override var beeBalance: Int = 50
    override var accessToken: String? = null
    override var refreshToken: String? = null
    override var currentUserId: String? = null
    override var lastAuthenticatedUserId: String? = null
    override fun lastSyncAt(userKey: String): String? = lastSyncAtByUser[userKey]

    override fun setLastSyncAt(userKey: String, value: String?) {
        if (value == null) {
            lastSyncAtByUser.remove(userKey)
        } else {
            lastSyncAtByUser[userKey] = value
        }
    }

    override fun localRevisionEpochMillis(userKey: String): Long =
        localRevisionByUser[userKey] ?: 0L

    override fun setLocalRevisionEpochMillis(userKey: String, value: Long) {
        localRevisionByUser[userKey] = value.coerceAtLeast(0L)
    }

    override fun appliedLexiconSchemaVersion(userKey: String): Int =
        appliedLexiconSchemaVersions[userKey] ?: 0

    override fun setAppliedLexiconSchemaVersion(userKey: String, version: Int) {
        appliedLexiconSchemaVersions[userKey] = version.coerceAtLeast(0)
    }

    override var streakDays: Int = 0
    override var lastActiveDayStartMillis: Long = 0L
    override var practiceRoundsCompleted: Int = 0
}
