package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable

/** `GET /v1/search/hints`: dictionary words that start with what the user has typed so far. */
@Serializable
data class SearchHintsResponse(
    val query: String,
    /** The typed text is itself a known word. */
    val exact: SearchHintResponse? = null,
    /** Look the text up now (a known word or a phrase); otherwise offer only [hints]. */
    val lookup: Boolean = false,
    val hints: List<SearchHintResponse> = emptyList(),
)

@Serializable
data class SearchHintResponse(
    val text: String,
    val lang: String,
)
