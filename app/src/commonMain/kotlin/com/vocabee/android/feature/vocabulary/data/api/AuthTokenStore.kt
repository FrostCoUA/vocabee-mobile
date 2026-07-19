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
    private val sessionNeedsReauthState = MutableStateFlow(false)

    /**
     * Сесію не вдалося оновити — треба запропонувати новий вхід. Це **не** вихід із
     * акаунта: токени лишаються на місці, щоб наступна спроба могла відновити сесію.
     */
    val sessionNeedsReauth: StateFlow<Boolean> = sessionNeedsReauthState.asStateFlow()

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
        sessionNeedsReauthState.value = false
    }

    fun refreshToken(): String? = preferencesManager?.refreshToken

    /**
     * Сигнал для UI, що сесію треба поновити входом. Токени навмисно лишаються:
     * причина може бути тимчасовою (сервер перезапустився, мережа), і наступний
     * виклик має шанс підняти сесію без участі юзера.
     */
    fun markSessionNeedsReauth() {
        sessionNeedsReauthState.value = true
    }

    /** Повний вихід. Викликається лише явним logout — ніколи автоматично. */
    fun clear() {
        preferencesManager?.accessToken = null
        preferencesManager?.refreshToken = null
        tokenState.value = null
        sessionNeedsReauthState.value = false
    }
}
