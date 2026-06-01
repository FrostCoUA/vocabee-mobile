package com.vocabee.android.domain.usecase

import com.vocabee.android.data.api.SearchVariant
import com.vocabee.android.data.api.VocabeeApi
import com.vocabee.android.data.api.VocabeeApiException
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote

/**
 * Calls the gateway's `/search` endpoint and adapts the response into the
 * presentation-layer [TranslationOption] list used by the Add Word overlay.
 */
class RemoteLexiconSearchUseCase(
    private val api: VocabeeApi,
) {
    suspend operator fun invoke(
        query: String,
        speakLang: String,
        learnLang: String,
        existingTranslations: Set<String>,
    ): Result {
        if (query.isBlank()) {
            return Result.Ok(query = query, options = emptyList(), tier = "anonymous", maxResults = 0)
        }
        return try {
            val response = api.search(
                query = query,
                speakLang = speakLang,
                learnLang = learnLang,
            )
            val options = response.results.map { variant -> variant.toOption(existingTranslations) }
            Result.Ok(
                query = query,
                options = options,
                tier = response.tier,
                maxResults = response.maxResults,
            )
        } catch (cause: VocabeeApiException) {
            Result.Failure(
                query = query,
                statusCode = cause.statusCode,
                message = cause.errorMessage ?: "Network error",
            )
        } catch (cause: Throwable) {
            Result.Failure(
                query = query,
                statusCode = null,
                message = cause.message ?: "Network error",
            )
        }
    }

    sealed interface Result {
        val query: String

        data class Ok(
            override val query: String,
            val options: List<TranslationOption>,
            val tier: String,
            val maxResults: Int,
        ) : Result

        data class Failure(
            override val query: String,
            val statusCode: Int?,
            val message: String,
        ) : Result
    }
}

private fun SearchVariant.toOption(existingTranslations: Set<String>): TranslationOption {
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
        value = translation,
        note = note,
        alreadyAdded = existingTranslations.contains(translation),
        learningWord = learningWord,
        ipa = ipa,
    )
}
