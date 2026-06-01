package com.vocabee.android.data.preferences

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
}
