package com.vocabee.android.data.api

interface VocabeeApi {
    /**
     * Calls `GET /v1/search?q=&speak=&learn=` on the gateway. Returns the parsed
     * response on success or throws [VocabeeApiException] otherwise.
     *
     * If the [AuthTokenStore] holds a token it is sent as a bearer header — the gateway
     * uses that to bump the tier from `anonymous` to `registered`/`premium`. Without a
     * token the gateway responds with up to 3 variants.
     */
    suspend fun search(
        query: String,
        speakLang: String,
        learnLang: String,
    ): SearchResponse
}

class VocabeeApiException(
    val statusCode: Int?,
    val errorType: String?,
    val errorMessage: String?,
) : RuntimeException(errorMessage ?: "API request failed")
