package com.vocabee.android.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the current bearer token.
 *
 * For now this is an in-memory store. Anonymous (unauthenticated) callers leave the
 * token as `null` — that maps directly to the gateway's anonymous-tier search.
 * When auth UI is wired in, persist tokens via DataStore and update this store.
 */
class AuthTokenStore {
    private val tokenState = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = tokenState.asStateFlow()

    fun current(): String? = tokenState.value

    fun set(token: String?) {
        tokenState.value = token
    }

    fun clear() {
        tokenState.value = null
    }
}
