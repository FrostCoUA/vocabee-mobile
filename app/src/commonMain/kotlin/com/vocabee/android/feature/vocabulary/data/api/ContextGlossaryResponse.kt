package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ContextGlossaryRequest(
    val sentence: String,
    val sourceLang: String,
    val targetLang: String,
)

@Serializable
data class ContextGlossaryResponse(
    val sentence: String,
    val sourceLang: String,
    val targetLang: String,
    val tokens: List<ContextGlossaryTokenResponse>,
)

@Serializable
data class ContextGlossaryTokenResponse(
    val surface: String,
    val normalized: String,
    val start: Int,
    val endExclusive: Int,
    val translation: String,
    val lemma: String? = null,
)
