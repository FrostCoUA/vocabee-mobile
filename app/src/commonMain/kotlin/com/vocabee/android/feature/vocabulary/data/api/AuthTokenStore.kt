package com.vocabee.android.feature.vocabulary.data.api

import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthSessionLease(
    val generation: Long,
    val accessToken: String?,
    val refreshToken: String?,
)

private data class AuthSessionState(
    val generation: Long,
    val accessToken: String?,
    val refreshToken: String?,
)

/**
 * Single source of truth for the current bearer token.
 *
 * Backed by preferences so a cold start can still check the gateway for sync.
 */
class AuthTokenStore(
    private val preferencesManager: PreferencesManager? = null,
) {
    private val sessionMutex = Mutex()
    private val sessionState = MutableStateFlow(
        AuthSessionState(
            generation = 0L,
            accessToken = preferencesManager?.accessToken,
            refreshToken = preferencesManager?.refreshToken,
        ),
    )
    private val tokenState = MutableStateFlow(sessionState.value.accessToken)
    val token: StateFlow<String?> = tokenState.asStateFlow()
    private val sessionNeedsReauthState = MutableStateFlow(false)

    /**
     * Сесію не вдалося оновити — треба запропонувати новий вхід. Це **не** вихід із
     * акаунта: токени лишаються на місці, щоб наступна спроба могла відновити сесію.
     */
    val sessionNeedsReauth: StateFlow<Boolean> = sessionNeedsReauthState.asStateFlow()

    fun current(): String? = sessionState.value.accessToken

    fun refreshToken(): String? = sessionState.value.refreshToken

    internal fun captureSession(): AuthSessionLease = sessionState.value.let { state ->
        AuthSessionLease(
            generation = state.generation,
            accessToken = state.accessToken,
            refreshToken = state.refreshToken,
        )
    }

    /** A Google login starts a new bearer owner, even when it is the same account. */
    suspend fun replaceSession(tokens: AuthTokensResponse) {
        sessionMutex.withLock {
            val next = AuthSessionState(
                generation = sessionState.value.generation + 1L,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            )
            persist(next)
            sessionState.value = next
            tokenState.value = next.accessToken
            sessionNeedsReauthState.value = false
        }
    }

    /**
     * Commits one-time refresh rotation only if logout/new login has not replaced
     * the session while the HTTP exchange was in flight.
     */
    suspend fun rotateSession(
        tokens: AuthTokensResponse,
        expectedGeneration: Long,
    ): Boolean = sessionMutex.withLock {
        val current = sessionState.value
        if (current.generation != expectedGeneration) return@withLock false
        val next = current.copy(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
        )
        persist(next)
        sessionState.value = next
        tokenState.value = next.accessToken
        sessionNeedsReauthState.value = false
        true
    }

    /**
     * Сигнал для UI, що сесію треба поновити входом. Токени навмисно лишаються:
     * причина може бути тимчасовою (сервер перезапустився, мережа), і наступний
     * виклик має шанс підняти сесію без участі юзера.
     */
    suspend fun markSessionNeedsReauth(expectedGeneration: Long): Boolean =
        sessionMutex.withLock {
            if (sessionState.value.generation != expectedGeneration) {
                return@withLock false
            }
            sessionNeedsReauthState.value = true
            true
        }

    /** Повний вихід інвалідовує всі запити/refresh старого bearer-власника. */
    suspend fun clear() {
        sessionMutex.withLock {
            val next = AuthSessionState(
                generation = sessionState.value.generation + 1L,
                accessToken = null,
                refreshToken = null,
            )
            persist(next)
            sessionState.value = next
            tokenState.value = null
            sessionNeedsReauthState.value = false
        }
    }

    private fun persist(state: AuthSessionState) {
        preferencesManager?.accessToken = state.accessToken
        preferencesManager?.refreshToken = state.refreshToken
    }
}
