package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.core.analytics.NoAnalyticsTracker
import com.vocabee.android.core.platform.currentEpochMillis
import com.vocabee.android.core.platform.debugLog
import com.vocabee.android.feature.vocabulary.data.api.SearchResponse
import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApiException
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.LexicalRegisterTag
import com.vocabee.android.feature.vocabulary.domain.model.LexicalUnitKind
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordForm
import com.vocabee.android.feature.vocabulary.domain.model.WordSense

/** Тег для `adb logcat -s VocabeeSearch` — звідки прийшов кожен переклад. */
internal const val SearchLogTag = "VocabeeSearch"

/**
 * Calls the gateway's `/search` endpoint and adapts the response into the
 * presentation-layer [TranslationOption] list used by the Add Word overlay.
 */
class RemoteLexiconSearchUseCase(
    private val api: VocabeeApi,
    private val analytics: AnalyticsTracker = NoAnalyticsTracker,
) {
    suspend operator fun invoke(
        query: String,
        speakLang: String,
        learnLang: String,
        existingTranslations: Set<String>,
    ): Result {
        if (query.isBlank()) {
            return Result.Ok(
                query = query,
                options = emptyList(),
                tier = "anonymous",
                maxResults = 0,
                beeBalance = null,
            )
        }
        return try {
            val startedAt = currentEpochMillis()
            val response = api.search(
                query = query,
                speakLang = speakLang,
                learnLang = learnLang,
            )
            trackSearchResult(response, currentEpochMillis() - startedAt)
            val options = response.results.map { variant -> variant.toOption(existingTranslations) }
            Result.Ok(
                query = query,
                options = options,
                tier = response.tier,
                maxResults = response.maxResults,
                beeBalance = response.meta.beeBalance,
            )
        } catch (cause: VocabeeApiException) {
            analytics.track(
                "translation_search_failed",
                mapOf("query" to query, "status_code" to cause.statusCode, "message" to cause.errorMessage),
            )
            Result.Failure(
                query = query,
                statusCode = cause.statusCode,
                message = cause.errorMessage ?: "Network error",
            )
        } catch (cause: Throwable) {
            analytics.track(
                "translation_search_failed",
                mapOf("query" to query, "status_code" to null, "message" to cause.message),
            )
            Result.Failure(
                query = query,
                statusCode = null,
                message = cause.message ?: "Network error",
            )
        }
    }

    /**
     * Клієнтський зріз відповіді `/search`: сирі `source`/`origin` губляться
     * при мапінгу в [TranslationOption], тож фіксуємо джерело даних (база
     * проти AI) саме тут, поки воно ще в руках.
     */
    private fun trackSearchResult(response: SearchResponse, durationMs: Long) {
        val primary = response.results.firstOrNull { it.isPrimary } ?: response.results.firstOrNull()
        val dataSource = translationDataSource(response, primary)
        val cachedCount = response.results.count { it.cached }
        val origin = primary?.origin ?: response.meta.dictionaryOrigin

        debugLog(
            SearchLogTag,
            "q='${response.query}' source=$dataSource ms=$durationMs n=${response.results.size} " +
                "cached=$cachedCount/${response.results.size} triedProvider=${response.meta.triedProvider} " +
                "reason=${response.meta.providerReason ?: "-"} origin=${origin ?: "-"}",
        )

        analytics.track(
            "translation_search_result",
            mapOf(
                "query" to response.query,
                "detected_lang" to response.detectedLang,
                "known_lang" to response.knownLang,
                "learning_lang" to response.learningLang,
                "tier" to response.tier,
                "results_count" to response.results.size,
                "cached_results_count" to cachedCount,
                "bee_balance" to response.meta.beeBalance,
                "translation_origin" to origin,
                "provider_reason" to response.meta.providerReason,
                "tried_provider" to response.meta.triedProvider,
                "data_source" to dataSource,
                "duration_ms" to durationMs,
            ),
        )
    }

    sealed interface Result {
        val query: String

        data class Ok(
            override val query: String,
            val options: List<TranslationOption>,
            val tier: String,
            val maxResults: Int,
            val beeBalance: Int?,
        ) : Result

        data class Failure(
            override val query: String,
            val statusCode: Int?,
            val message: String,
        ) : Result
    }
}

/**
 * `providerReason` від сервера — авторитетне джерело: воно каже, чи сервер
 * реально ходив до провайдера. Якщо його немає (старий сервер), падаємо
 * назад на ознаку `cached` у першому результаті.
 */
internal fun translationDataSource(response: SearchResponse, primary: SearchVariant?): String =
    when (response.meta.providerReason) {
        "exact_cached" -> "database"
        "translated" -> if (isAiOrigin(primary?.origin)) "ai" else "provider"
        "not_a_word", "echo", "no_provider_data" -> "none"
        else -> when {
            primary == null -> "none"
            primary.cached -> "database"
            isAiOrigin(primary.origin) -> "ai"
            else -> "provider"
        }
    }

private fun isAiOrigin(origin: String?): Boolean =
    origin?.startsWith("openai-") == true || origin?.startsWith("ai-") == true

internal fun SearchVariant.toOption(existingTranslations: Set<String>): TranslationOption {
    val translation = knownWord
    val note = when {
        existingTranslations.contains(translation) ->
            TranslationOptionNote.AlreadyAdded(source = origin)
        source == "dictionary" -> TranslationOptionNote.Primary
        source == "translator" -> TranslationOptionNote.Primary
        source == "ai" -> TranslationOptionNote.Additional
        else -> TranslationOptionNote.Alternative
    }
    return TranslationOption(
        translationId = translationId,
        value = translation,
        note = note,
        alreadyAdded = existingTranslations.contains(translation),
        learningWord = learningWord,
        ipa = ipa,
        details = WordDetails(
            senseIndex = senseIndex,
            senses = senses.map { sense ->
                WordSense(
                    definition = sense.definition,
                    partOfSpeech = sense.partOfSpeech,
                    tags = sense.tags,
                    examples = sense.examples.map { it.text },
                    synonyms = sense.synonyms,
                    antonyms = sense.antonyms,
                )
            },
            synonyms = synonyms,
            antonyms = antonyms,
            forms = forms.map { WordForm(text = it.text, tags = it.tags) },
            partOfSpeech = partOfSpeech,
            lexicalUnitKind = lexicalUnitKind.toLexicalUnitKind(),
            registerTags = registerTags.mapNotNull(String::toLexicalRegisterTag).distinct(),
            expansion = expansion,
            translatedExpansion = translatedExpansion,
            meaning = meaning,
            literalTranslation = literalTranslation,
            usageExample = usageExample,
            usageExampleTranslation = usageExampleTranslation,
        ).takeUnless { it.isEmpty },
    )
}

private fun String.toLexicalUnitKind(): LexicalUnitKind = when (lowercase()) {
    "phrase" -> LexicalUnitKind.Phrase
    "expression" -> LexicalUnitKind.Expression
    "abbreviation" -> LexicalUnitKind.Abbreviation
    else -> LexicalUnitKind.Word
}

private fun String.toLexicalRegisterTag(): LexicalRegisterTag? = when (lowercase()) {
    "slang" -> LexicalRegisterTag.Slang
    "informal" -> LexicalRegisterTag.Informal
    "formal" -> LexicalRegisterTag.Formal
    "technical" -> LexicalRegisterTag.Technical
    "offensive" -> LexicalRegisterTag.Offensive
    "humorous" -> LexicalRegisterTag.Humorous
    "internet" -> LexicalRegisterTag.Internet
    else -> null
}
