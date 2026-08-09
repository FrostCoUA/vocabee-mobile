package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.SyncResponse
import com.vocabee.android.feature.vocabulary.data.sync.VocabularySyncAttempt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupLexiconSyncTest {
    @Test
    fun schemaUpgradeForcesFullPullEvenWithAnExistingDeltaCursor() = runBlocking {
        val calls = mutableListOf<String?>()

        val snapshot = fetchStartupVocabularySnapshot(
            since = "2026-08-09T12:00:00.000Z",
            forceFullLexiconRefresh = true,
            syncTopics = { since ->
                calls += since
                emptyResponse()
            },
        )

        assertEquals(listOf<String?>(null), calls)
        assertEquals(1, snapshot?.lexiconSchemaVersion)
    }

    @Test
    fun currentSchemaAndEmptyDeltaDoNotTriggerFullPull() = runBlocking {
        val cursor = "2026-08-09T12:00:00.000Z"
        val calls = mutableListOf<String?>()

        val snapshot = fetchStartupVocabularySnapshot(
            since = cursor,
            forceFullLexiconRefresh = false,
            syncTopics = { since ->
                calls += since
                emptyResponse()
            },
        )

        assertEquals(listOf(cursor), calls)
        assertNull(snapshot)
    }

    @Test
    fun staleDestructiveSyncAlwaysCompletesItsErrorCallback() {
        var done = false
        var error: String? = null

        handleVocabularySyncAttempt(
            attempt = VocabularySyncAttempt.LocalRevisionChanged,
            onDone = { done = true },
            onError = { error = it },
        )

        assertEquals(false, done)
        assertEquals("Локальні дані змінилися. Повтори синхронізацію.", error)
    }

    private fun emptyResponse(): SyncResponse = SyncResponse(
        topics = emptyList(),
        words = emptyList(),
        deletedTopicIds = emptyList(),
        deletedWordIds = emptyList(),
        serverTime = "2026-08-10T00:00:00.000Z",
    )
}
