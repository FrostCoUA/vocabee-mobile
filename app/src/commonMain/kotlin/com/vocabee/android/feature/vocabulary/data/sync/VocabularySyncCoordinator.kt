package com.vocabee.android.feature.vocabulary.data.sync

import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Outcome of one serialized vocabulary sync attempt. */
internal sealed interface VocabularySyncAttempt<out T> {
    data class Applied<T>(
        val response: T,
        val capturedLocalRevision: Long,
    ) : VocabularySyncAttempt<T>

    data object AccountChanged : VocabularySyncAttempt<Nothing>

    data object LocalRevisionChanged : VocabularySyncAttempt<Nothing>
}

/**
 * Serializes vocabulary requests and rejects responses whose account or local
 * change token no longer matches the lease captured before the request.
 */
internal class VocabularySyncCoordinator(
    private val preferencesManager: PreferencesManager,
    private val activeUserKey: () -> String?,
) {
    private val mutex = Mutex()

    suspend fun <T> run(
        userKey: String,
        expectedLocalRevision: Long? = null,
        request: suspend (capturedLocalRevision: Long) -> T,
        apply: (T) -> Unit,
    ): VocabularySyncAttempt<T> = mutex.withLock {
        if (activeUserKey() != userKey) {
            return@withLock VocabularySyncAttempt.AccountChanged
        }

        val capturedLocalRevision = preferencesManager.localRevisionEpochMillis(userKey)
        if (
            expectedLocalRevision != null &&
            capturedLocalRevision != expectedLocalRevision
        ) {
            return@withLock VocabularySyncAttempt.LocalRevisionChanged
        }
        val response = request(capturedLocalRevision)

        if (activeUserKey() != userKey) {
            return@withLock VocabularySyncAttempt.AccountChanged
        }
        if (preferencesManager.localRevisionEpochMillis(userKey) != capturedLocalRevision) {
            return@withLock VocabularySyncAttempt.LocalRevisionChanged
        }

        apply(response)
        VocabularySyncAttempt.Applied(response, capturedLocalRevision)
    }
}
