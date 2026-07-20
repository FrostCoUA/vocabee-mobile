package com.vocabee.android.feature.vocabulary.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Ktor-backed [VocabeeApi]. Constructed with the platform HTTP client (built by
 * `VocabeeHttpClientFactory.create(...)` on Android).
 */
class KtorVocabeeApi(
    private val client: HttpClient,
    private val config: VocabeeApiConfig,
    private val tokenStore: AuthTokenStore,
) : VocabeeApi, SessionExpiryObservable {

    override val sessionNeedsReauth = tokenStore.sessionNeedsReauth

    /**
     * A refresh token can only be used once. Serialising refreshes prevents two
     * simultaneous 401 responses from racing and revoking each other's session.
     */
    private val refreshMutex = Mutex()

    override suspend fun search(
        query: String,
        speakLang: String,
        learnLang: String,
    ): SearchResponse {
        return withOptionalAccessToken { accessToken ->
            executeRequest {
                client.get("${config.baseUrl}/v1/search") {
                    parameter("q", query)
                    parameter("speak", speakLang)
                    parameter("learn", learnLang)
                    accessToken?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun submitQualityFeedback(
        request: QualityFeedbackRequest,
    ): QualityFeedbackResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/translation-feedback") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun buildContextGlossary(
        sentence: String,
        sourceLang: String,
        targetLang: String,
    ): ContextGlossaryResponse {
        return withOptionalAccessToken { accessToken ->
            executeRequest {
                client.post("${config.baseUrl}/v1/search/context-glossary") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        ContextGlossaryRequest(
                            sentence = sentence,
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                        ),
                    )
                    accessToken?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun loginWithGoogle(
        idToken: String,
        speakLang: String?,
        learnLang: String?,
    ): AuthTokensResponse {
        val tokens = executeRequest {
            client.post("${config.baseUrl}/v1/auth/google") {
                contentType(ContentType.Application.Json)
                setBody(
                    GoogleAuthRequest(
                        idToken = idToken,
                        speakLang = speakLang,
                        learnLang = learnLang,
                    ),
                )
            }.body<AuthTokensResponse>()
        }
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun currentUser(): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.get("${config.baseUrl}/v1/auth/me") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun updateCurrentUser(request: UpdateProfileRequest): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.patch("${config.baseUrl}/v1/me") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    /**
     * Приватний навмисно: ротація мусить іти лише через [renewSession] під мьютексом,
     * інакше два обміни одним токеном вбивають сесію.
     */
    private suspend fun refreshSession(refreshToken: String): AuthTokensResponse {
        val tokens = executeRequest {
            client.post("${config.baseUrl}/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }.body<AuthTokensResponse>()
        }
        tokenStore.set(tokens)
        return tokens
    }

    override suspend fun syncTopics(since: String?): SyncResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/topics/sync") {
                    contentType(ContentType.Application.Json)
                    setBody(SyncRequest(since))
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun applySync(request: ApplySyncRequest): SyncResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/topics/sync/apply") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun claimRewardedAdBees(): UserResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/wallet/rewarded-ad") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun fetchReferral(): ReferralResponse {
        return withFreshAccessToken {
            executeRequest {
                client.get("${config.baseUrl}/v1/referral/me") {
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    override suspend fun submitSupport(request: SupportRequestBody): SupportResponse {
        return withFreshAccessToken {
            executeRequest {
                client.post("${config.baseUrl}/v1/support") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                    tokenStore.current()?.let { token -> bearerAuth(token) }
                }.body()
            }
        }
    }

    /**
     * Для публічних ендпоінтів (D2: `/search` працює без акаунта). Мертва сесія —
     * 401 і на запит, і на refresh — не має їх блокувати: повторюємо запит
     * анонімно, без bearer. Токени навмисно не чистимо (див.
     * [AuthTokenStore.markSessionNeedsReauth]) — прапорець reauth уже стоїть,
     * UI запропонує вхід, а пошук тим часом працює у гостьовому режимі.
     */
    private suspend fun <T> withOptionalAccessToken(
        request: suspend (accessToken: String?) -> T,
    ): T {
        return try {
            withFreshAccessToken { request(tokenStore.current()) }
        } catch (cause: VocabeeApiException) {
            if (cause.statusCode != 401) throw cause
            request(null)
        }
    }

    /** Replays one request after renewing an expired access token. */
    private suspend fun <T> withFreshAccessToken(request: suspend () -> T): T {
        val failedAccessToken = tokenStore.current()
        if (failedAccessToken == null) {
            // Access протух і був стертий, але сесія жива — піднімаємо її з refresh.
            if (tokenStore.refreshToken() == null) return request()
            renewSession(failedAccessToken = null)
            return request()
        }
        return try {
            request()
        } catch (cause: VocabeeApiException) {
            if (cause.statusCode != 401) throw cause
            renewSession(failedAccessToken)
            request()
        }
    }

    /**
     * Єдина точка ротації сесії. Refresh одноразовий на сервері, тож два паралельні
     * обміни одним і тим самим токеном вбивали б сесію — [refreshMutex] цього не пускає,
     * а [NonCancellable] гарантує, що вже відкликаний сервером токен буде замінений
     * локально навіть якщо викликач зник (юзер пішов з екрана).
     */
    private suspend fun renewSession(failedAccessToken: String?) {
        refreshMutex.withLock {
            // Another concurrent request has already completed the rotation.
            if (failedAccessToken != null && tokenStore.current() != failedAccessToken) return
            var presentedToken = tokenStore.refreshToken() ?: run {
                tokenStore.markSessionNeedsReauth()
                throw sessionRenewalFailed()
            }
            repeat(MaxRefreshAttempts) {
                try {
                    withContext(NonCancellable) { refreshSession(presentedToken) }
                    return
                } catch (cause: VocabeeApiException) {
                    if (cause.statusCode != 401) throw cause
                    // Токен уже обміняли деінде — пробуємо тим, що лежить у сховищі зараз.
                    val storedToken = tokenStore.refreshToken()
                    if (storedToken == null || storedToken == presentedToken) {
                        tokenStore.markSessionNeedsReauth()
                        throw cause
                    }
                    presentedToken = storedToken
                }
            }
            tokenStore.markSessionNeedsReauth()
            throw sessionRenewalFailed()
        }
    }

    private fun sessionRenewalFailed() = VocabeeApiException(
        statusCode = 401,
        errorType = "unauthorized",
        errorMessage = "Потрібна повторна авторизація.",
    )

    private suspend fun <T> executeRequest(request: suspend () -> T): T = try {
        request()
    } catch (cause: ClientRequestException) {
        throw cause.toApiException()
    } catch (cause: ServerResponseException) {
        throw cause.toApiException()
    } catch (cause: ResponseException) {
        throw cause.toApiException()
    }

    private suspend fun ResponseException.toApiException(): VocabeeApiException {
        val body = runCatching { response.body<ApiErrorBody>() }.getOrNull()
        return VocabeeApiException(
            statusCode = response.statusValue(),
            errorType = body?.errorType,
            errorMessage = body?.message ?: response.statusValueDescription(),
        )
    }
}

/** Одна повторна спроба на випадок, якщо refresh уже обміняли паралельно. */
private const val MaxRefreshAttempts = 2

private fun HttpResponse.statusValue(): Int = status.value
private fun HttpResponse.statusValueDescription(): String = status.description
