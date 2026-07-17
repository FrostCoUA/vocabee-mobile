package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryResponse
import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryTokenResponse
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken

class ContextGlossaryUseCase(
    private val api: VocabeeApi,
) {
    suspend operator fun invoke(
        sentence: String,
        sourceLang: String,
        targetLang: String,
    ): ContextGlossary? {
        val response = api.buildContextGlossary(
            sentence = sentence,
            sourceLang = sourceLang,
            targetLang = targetLang,
        )
        return response.toValidatedGlossary()
    }
}

internal fun ContextGlossaryResponse.toValidatedGlossary(): ContextGlossary? {
    if (sentence.isBlank() || sourceLang.isBlank() || targetLang.isBlank()) return null
    if (tokens.isEmpty()) return null
    val mapped = tokens.map { token -> token.toValidatedToken(sentence) ?: return null }
    val ordered = mapped.sortedBy(ContextGlossaryToken::start)
    if (ordered.zipWithNext().any { (left, right) -> left.endExclusive > right.start }) return null
    return ContextGlossary(
        sentence = sentence,
        sourceLang = sourceLang,
        targetLang = targetLang,
        tokens = ordered,
    )
}

private fun ContextGlossaryTokenResponse.toValidatedToken(sentence: String): ContextGlossaryToken? {
    if (translation.isBlank() || start < 0 || endExclusive <= start || endExclusive > sentence.length) {
        return null
    }
    if (sentence.substring(start, endExclusive) != surface) return null
    return ContextGlossaryToken(
        surface = surface,
        normalized = normalized,
        start = start,
        endExclusive = endExclusive,
        translation = translation.trim(),
        lemma = lemma?.trim()?.takeIf(String::isNotEmpty),
    )
}
