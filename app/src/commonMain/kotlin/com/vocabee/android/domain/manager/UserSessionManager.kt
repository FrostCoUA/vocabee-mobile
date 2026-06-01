package com.vocabee.android.domain.manager

import com.vocabee.android.domain.model.DEFAULT_LOCAL_USER_KEY

interface UserSessionManager {
    val currentUserKey: String
}

class StaticUserSessionManager(
    override val currentUserKey: String = DEFAULT_LOCAL_USER_KEY,
) : UserSessionManager
