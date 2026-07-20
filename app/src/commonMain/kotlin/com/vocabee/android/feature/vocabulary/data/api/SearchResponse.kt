package com.vocabee.android.feature.vocabulary.data.api

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
    val translationId: String = "",
    val knownWord: String,
    val learningWord: String,
    val ipa: String? = null,
    val audioUrl: String? = null,
    val partOfSpeech: List<String> = emptyList(),
    val examples: List<SearchExample> = emptyList(),
    val senses: List<SearchSense> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val forms: List<SearchForm> = emptyList(),
    /** Індекс sense'а (в [senses]), який рендерить цей переклад; null — не атрибутовано. */
    val senseIndex: Int? = null,
    val lexicalUnitKind: String = "word",
    val registerTags: List<String> = emptyList(),
    val expansion: String? = null,
    val translatedExpansion: String? = null,
    val meaning: String? = null,
    val literalTranslation: String? = null,
    val usageExample: String? = null,
    val usageExampleTranslation: String? = null,
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
data class SearchSense(
    val definition: String,
    val partOfSpeech: String? = null,
    val tags: List<String> = emptyList(),
    val examples: List<SearchExample> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
)

@Serializable
data class SearchForm(
    val text: String,
    val tags: List<String> = emptyList(),
)

@Serializable
data class SearchMeta(
    val totalAvailable: Int,
    val dictionarySource: String? = null,
    val dictionaryOrigin: String? = null,
    val beeBalance: Int? = null,
    /** True лише коли сервер реально ходив до провайдера перекладу (а не віддав із бази). */
    val triedProvider: Boolean = false,
    /** `exact_cached` — з бази; `translated` — згенеровано; `not_a_word`/`echo`/`no_provider_data` — без перекладу. */
    val providerReason: String? = null,
)

@Serializable
data class ApiErrorBody(
    val statusCode: Int? = null,
    val message: String? = null,
    @SerialName("error") val errorType: String? = null,
)
