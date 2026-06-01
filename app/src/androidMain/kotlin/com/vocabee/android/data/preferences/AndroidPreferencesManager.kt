package com.vocabee.android.data.preferences

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
 * All operations are synchronous — these prefs are tiny (3 keys) and the
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

    private companion object {
        const val PREFS_NAME = "vocabee_prefs"
        const val KEY_ONBOARDED = "has_completed_onboarding"
        const val KEY_USER_LANG = "user_language_code"
        const val KEY_LEARNING_LANG = "learning_language_code"
    }
}
