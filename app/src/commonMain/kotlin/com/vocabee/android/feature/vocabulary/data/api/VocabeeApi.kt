package com.vocabee.android.feature.vocabulary.data.api

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

    suspend fun loginWithGoogle(
        idToken: String,
        speakLang: String? = null,
        learnLang: String? = null,
    ): AuthTokensResponse

    suspend fun currentUser(): UserResponse

    suspend fun updateCurrentUser(request: UpdateProfileRequest): UserResponse

    suspend fun refreshSession(refreshToken: String): AuthTokensResponse

    suspend fun syncTopics(since: String? = null): SyncResponse

    suspend fun applySync(request: ApplySyncRequest): SyncResponse

    suspend fun claimRewardedAdBees(): UserResponse

    suspend fun fetchReferral(): ReferralResponse

    suspend fun submitSupport(request: SupportRequestBody): SupportResponse
}

class VocabeeApiException(
    val statusCode: Int?,
    val errorType: String?,
    val errorMessage: String?,
) : RuntimeException(errorMessage ?: "API request failed")
