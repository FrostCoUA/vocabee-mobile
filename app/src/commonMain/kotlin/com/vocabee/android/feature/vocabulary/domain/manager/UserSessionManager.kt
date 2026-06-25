package com.vocabee.android.feature.vocabulary.domain.manager

import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY

interface UserSessionManager {
    val currentUserKey: String
}

class StaticUserSessionManager(
    override val currentUserKey: String = DEFAULT_LOCAL_USER_KEY,
) : UserSessionManager

class PreferencesUserSessionManager(
    private val preferencesManager: PreferencesManager,
) : UserSessionManager {
    override val currentUserKey: String
        get() = preferencesManager.currentUserId ?: DEFAULT_LOCAL_USER_KEY
}
