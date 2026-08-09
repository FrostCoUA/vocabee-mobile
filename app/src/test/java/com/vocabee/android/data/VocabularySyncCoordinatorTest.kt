package com.vocabee.android.data

import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import com.vocabee.android.feature.vocabulary.data.sync.VocabularySyncAttempt
import com.vocabee.android.feature.vocabulary.data.sync.VocabularySyncCoordinator
import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VocabularySyncCoordinatorTest {
    @Test
    fun responseForPreviousAccountIsRejectedAfterAccountSwitch() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply { currentUserId = UserA }
        val coordinator = coordinator(preferences)
        val requestStarted = CompletableDeferred<Unit>()
        val finishRequest = CompletableDeferred<Unit>()
        var applied: String? = null
        var outcome: VocabularySyncAttempt<String>? = null

        val sync = launch {
            outcome = coordinator.run(
                userKey = UserA,
                request = {
                    requestStarted.complete(Unit)
                    finishRequest.await()
                    "old-account-response"
                },
                apply = { applied = it },
            )
        }

        requestStarted.await()
        preferences.currentUserId = UserB
        finishRequest.complete(Unit)
        sync.join()

        assertEquals(VocabularySyncAttempt.AccountChanged, outcome)
        assertNull(applied)
    }

    @Test
    fun responseIsRejectedWhenLocalEditChangesRevisionDuringRequest() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            currentUserId = UserA
            setLocalRevisionEpochMillis(UserA, 4L)
        }
        val coordinator = coordinator(preferences)
        val requestStarted = CompletableDeferred<Unit>()
        val finishRequest = CompletableDeferred<Unit>()
        var applied: String? = null
        var outcome: VocabularySyncAttempt<String>? = null

        val sync = launch {
            outcome = coordinator.run(
                userKey = UserA,
                request = {
                    requestStarted.complete(Unit)
                    finishRequest.await()
                    "stale-response"
                },
                apply = { applied = it },
            )
        }

        requestStarted.await()
        preferences.setLocalRevisionEpochMillis(UserA, 5L)
        finishRequest.complete(Unit)
        sync.join()

        assertEquals(VocabularySyncAttempt.LocalRevisionChanged, outcome)
        assertNull(applied)
        assertEquals(5L, preferences.localRevisionEpochMillis(UserA))
    }

    @Test
    fun delayedSnapshotLeaseIsRejectedBeforeStartingAnotherFullPull() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            currentUserId = UserA
            setLocalRevisionEpochMillis(UserA, 2L)
        }
        val coordinator = coordinator(preferences)
        var requestCount = 0

        val outcome = coordinator.run(
            userKey = UserA,
            expectedLocalRevision = 1L,
            request = {
                requestCount += 1
                "must-not-run"
            },
            apply = {},
        )

        assertEquals(VocabularySyncAttempt.LocalRevisionChanged, outcome)
        assertEquals(0, requestCount)
    }

    @Test
    fun concurrentRequestsAreSerializedSoResponsesCannotReorder() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply { currentUserId = UserA }
        val coordinator = coordinator(preferences)
        val firstStarted = CompletableDeferred<Unit>()
        val finishFirst = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val applied = mutableListOf<String>()

        val first = launch {
            coordinator.run(
                userKey = UserA,
                request = {
                    firstStarted.complete(Unit)
                    finishFirst.await()
                    "first"
                },
                apply = { applied += it },
            )
        }
        firstStarted.await()
        val second = launch {
            coordinator.run(
                userKey = UserA,
                request = {
                    secondStarted.complete(Unit)
                    "second"
                },
                apply = { applied += it },
            )
        }

        yield()
        assertFalse(secondStarted.isCompleted)
        finishFirst.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf("first", "second"), applied)
    }

    @Test
    fun cursorAndLocalRevisionAreIsolatedForEveryUserIncludingLocalUser() {
        val preferences = InMemoryPreferencesManager()

        preferences.setLastSyncAt(UserA, "cursor-a")
        preferences.setLastSyncAt(UserB, "cursor-b")
        preferences.setLocalRevisionEpochMillis(UserA, 2L)
        preferences.setLocalRevisionEpochMillis(DEFAULT_LOCAL_USER_KEY, 7L)

        assertEquals("cursor-a", preferences.lastSyncAt(UserA))
        assertEquals("cursor-b", preferences.lastSyncAt(UserB))
        assertNull(preferences.lastSyncAt(DEFAULT_LOCAL_USER_KEY))
        assertEquals(2L, preferences.localRevisionEpochMillis(UserA))
        assertEquals(0L, preferences.localRevisionEpochMillis(UserB))
        assertEquals(7L, preferences.localRevisionEpochMillis(DEFAULT_LOCAL_USER_KEY))
    }

    private fun coordinator(preferences: InMemoryPreferencesManager) =
        VocabularySyncCoordinator(
            preferencesManager = preferences,
            activeUserKey = { preferences.currentUserId },
        )

    private companion object {
        const val UserA = "user-a"
        const val UserB = "user-b"
    }
}
