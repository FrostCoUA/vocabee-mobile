package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.SearchMeta
import com.vocabee.android.feature.vocabulary.data.api.SearchResponse
import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
import com.vocabee.android.feature.vocabulary.domain.usecase.translationDataSource
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Джерело даних, яке бачить мобілка: чи прийшов переклад із бази, чи його
 * щойно згенерував AI. Сервер каже це через `meta.providerReason`.
 */
class TranslationDataSourceTest {
    @Test
    fun cachedLookupIsReportedAsDatabaseEvenWhenTheRowWasOnceMadeByAi() {
        // Головна пастка: origin=openai-* на рядку, який лежить у Postgres.
        val variant = variant(origin = "openai-gpt-5.6-sol", cached = true)

        assertEquals(
            "database",
            translationDataSource(response("exact_cached", triedProvider = false, variant), variant),
        )
    }

    @Test
    fun freshAiGenerationIsReportedAsAi() {
        val variant = variant(origin = "openai-gpt-5.6-sol", cached = false)

        assertEquals(
            "ai",
            translationDataSource(response("translated", triedProvider = true, variant), variant),
        )
    }

    @Test
    fun freshNonAiProviderIsReportedAsProvider() {
        val variant = variant(origin = "deepl-free", cached = false)

        assertEquals(
            "provider",
            translationDataSource(response("translated", triedProvider = true, variant), variant),
        )
    }

    @Test
    fun searchesWithoutATranslationAreReportedAsNone() {
        for (reason in listOf("not_a_word", "echo", "no_provider_data")) {
            assertEquals(
                "none",
                translationDataSource(response(reason, triedProvider = true, null), null),
            )
        }
    }

    @Test
    fun fallsBackToTheCachedFlagWhenTheServerSendsNoReason() {
        val cached = variant(origin = "openai-gpt-5.6-sol", cached = true)
        val fresh = variant(origin = "openai-gpt-5.6-sol", cached = false)

        assertEquals("database", translationDataSource(response(null, false, cached), cached))
        assertEquals("ai", translationDataSource(response(null, false, fresh), fresh))
    }

    private fun variant(origin: String, cached: Boolean) = SearchVariant(
        knownWord = "бігти",
        learningWord = "run",
        source = "translator",
        origin = origin,
        isPrimary = true,
        cached = cached,
    )

    private fun response(
        providerReason: String?,
        triedProvider: Boolean,
        variant: SearchVariant?,
    ) = SearchResponse(
        query = "run",
        detectedLang = "en",
        isPhrase = false,
        knownLang = "uk",
        learningLang = "en",
        tier = "anonymous",
        maxResults = 50,
        results = listOfNotNull(variant),
        meta = SearchMeta(
            totalAvailable = if (variant == null) 0 else 1,
            triedProvider = triedProvider,
            providerReason = providerReason,
        ),
    )
}
