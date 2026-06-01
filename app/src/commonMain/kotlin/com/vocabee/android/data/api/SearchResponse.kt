package com.vocabee.android.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** DTO mirror of the gateway's `SearchResponseDto`. */
@Serializable
data class SearchResponse(
    val query: String,
    val detectedLang: String,
    val isPhrase: Boolean,
    val knownLang: String,
    val learningLang: String,
    val tier: String,
    val maxResults: Int,
    val results: List<SearchVariant>,
    val meta: SearchMeta,
)

@Serializable
data class SearchVariant(
    val knownWord: String,
    val learningWord: String,
    val ipa: String? = null,
    val audioUrl: String? = null,
    val partOfSpeech: List<String> = emptyList(),
    val examples: List<SearchExample> = emptyList(),
    val source: String,
    val origin: String,
    val confidence: Double? = null,
    val isPrimary: Boolean,
    val cached: Boolean,
)

@Serializable
data class SearchExample(
    val text: String,
    val translation: String? = null,
)

@Serializable
data class SearchMeta(
    val totalAvailable: Int,
    val dictionarySource: String? = null,
    val dictionaryOrigin: String? = null,
)

@Serializable
data class ApiErrorBody(
    val statusCode: Int? = null,
    val message: String? = null,
    @SerialName("error") val errorType: String? = null,
)
