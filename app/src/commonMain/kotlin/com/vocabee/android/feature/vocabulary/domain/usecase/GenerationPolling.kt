package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.data.api.GenerationMeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

internal const val GenerationWaitLimitMillis = 45 * 60 * 1_000L
private const val DefaultGenerationRetryMillis = 2_000L
private const val MinGenerationRetryMillis = 250L
private const val MaxGenerationRetryMillis = 10_000L

internal class GenerationFailedException(message: String) : Exception(message)

/**
 * Wait for one already-started generation. The poll callback must use its ID,
 * never repeat the original paid/AI-starting request.
 */
internal suspend fun <T : Any> awaitGeneration(
    initial: T,
    generationOf: (T) -> GenerationMeta?,
    poll: suspend (String) -> T,
    maxWaitMillis: Long = GenerationWaitLimitMillis,
    pause: suspend (Long) -> Unit = ::delay,
): T {
    return withTimeoutOrNull(maxWaitMillis) {
        var response = initial
        while (true) {
            val generation = generationOf(response) ?: return@withTimeoutOrNull response
            when (generation.status) {
                "complete" -> return@withTimeoutOrNull response
                "failed" -> throw GenerationFailedException(
                    "Не вдалося завершити генерацію. Спробуй ще раз.",
                )
                "generating" -> Unit
                else -> throw GenerationFailedException("Невідомий стан генерації. Спробуй ще раз.")
            }
            if (generation.id.isBlank()) {
                throw GenerationFailedException("Не вдалося продовжити генерацію. Спробуй ще раз.")
            }
            val retry = (generation.retryAfterMs ?: DefaultGenerationRetryMillis)
                .coerceIn(MinGenerationRetryMillis, MaxGenerationRetryMillis)
            pause(retry)
            response = try {
                poll(generation.id)
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                throw GenerationFailedException("Генерація недоступна. Спробуй ще раз.")
            }
        }
        @Suppress("UNREACHABLE_CODE")
        response
    } ?: throw GenerationFailedException("Очікування генерації закінчилося. Спробуй ще раз.")
}
