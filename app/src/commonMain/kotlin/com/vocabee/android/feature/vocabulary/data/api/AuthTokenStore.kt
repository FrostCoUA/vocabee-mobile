package com.vocabee.android.feature.vocabulary.data.api

import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the current bearer token.
 *
 * Backed by preferences so a cold start can still check the gateway for sync.
 */
class AuthTokenStore(
    private val preferencesManager: PreferencesManager? = null,
) {
    private val tokenState = MutableStateFlow(preferencesManager?.accessToken)
    val token: StateFlow<String?> = tokenState.asStateFlow()
    private val sessionExpiredState = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = sessionExpiredState.asStateFlow()

    fun current(): String? {
        if (preferencesManager != null) {
            val persistedToken = preferencesManager.accessToken
            if (persistedToken != tokenState.value) {
                tokenState.value = persistedToken
            }
            return persistedToken
        }
        return tokenState.value
    }

    fun set(token: String?) {
        preferencesManager?.accessToken = token
        tokenState.value = token
    }

    fun set(tokens: AuthTokensResponse) {
        preferencesManager?.refreshToken = tokens.refreshToken
        set(tokens.accessToken)
        sessionExpiredState.value = false
    }

    fun refreshToken(): String? = preferencesManager?.refreshToken

    fun clear() {
        preferencesManager?.accessToken = null
        preferencesManager?.refreshToken = null
        tokenState.value = null
        sessionExpiredState.value = true
    }
}
