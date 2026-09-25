package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.data.api.GenerationMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerationPollingTest {
    private data class Response(val value: String, val generation: GenerationMeta? = null)

    @Test
    fun pendingPollsItsGenerationIdUntilComplete() = runBlocking {
        val polled = mutableListOf<String>()
        val ready = awaitGeneration(
            initial = Response("", GenerationMeta("job-1", "generating", 200)),
            generationOf = Response::generation,
            poll = { id ->
                polled += id
                Response("готово", GenerationMeta(id, "complete"))
            },
            pause = {},
        )

        assertEquals("готово", ready.value)
        assertEquals(listOf("job-1"), polled)
    }

    @Test
    fun failedGenerationNeverReturnsEmptyResultAsSuccess() = runBlocking {
        val failure = assertThrows(GenerationFailedException::class.java) {
            runBlocking {
                awaitGeneration(
                    initial = Response("", GenerationMeta("job-2", "failed")),
                    generationOf = Response::generation,
                    poll = { error("poll should not run") },
                )
            }
        }
        assertTrue(failure.message.orEmpty().contains("Спробуй ще раз"))
    }

    @Test
    fun unavailableGenerationGivesRetryableUkrainianError() = runBlocking {
        val failure = assertThrows(GenerationFailedException::class.java) {
            runBlocking {
                awaitGeneration(
                    initial = Response("", GenerationMeta("job-lost", "generating")),
                    generationOf = Response::generation,
                    poll = { error("HTTP 404") },
                    pause = {},
                )
            }
        }
        assertTrue(failure.message.orEmpty().contains("Генерація недоступна"))
    }

    @Test
    fun boundedWaitGivesRetryableError() = runBlocking {
        val failure = assertThrows(GenerationFailedException::class.java) {
            runBlocking {
                awaitGeneration(
                    initial = Response("", GenerationMeta("job-3", "generating")),
                    generationOf = Response::generation,
                    poll = { error("poll should not run") },
                    maxWaitMillis = 20,
                    pause = { delay(100) },
                )
            }
        }
        assertTrue(failure.message.orEmpty().contains("Очікування"))
    }

    @Test
    fun cancellationStopsPendingPoll() = runBlocking {
        val job = async {
            awaitGeneration(
                initial = Response("", GenerationMeta("job-4", "generating")),
                generationOf = Response::generation,
                poll = { error("poll should not run") },
                pause = { delay(60_000) },
            )
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertThrows(CancellationException::class.java) { runBlocking { job.await() } }
        Unit
    }
}
