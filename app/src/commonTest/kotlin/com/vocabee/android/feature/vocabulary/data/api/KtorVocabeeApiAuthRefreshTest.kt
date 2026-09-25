package com.vocabee.android.feature.vocabulary.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoteLexiconSearchUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorVocabeeApiAuthRefreshTest {
    @Test
    fun generationPollsUseDedicatedGetEndpoints() = runBlocking {
        val paths = mutableListOf<String>()
        val api = apiWithEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val body = if (path.contains("context-glossary")) {
                """{"sentence":"I run.","sourceLang":"en","targetLang":"uk","tokens":[],"generation":{"id":"job-2","status":"generating","retryAfterMs":2000}}"""
            } else {
                searchResponse().replace(
                    "\"totalAvailable\":0",
                    "\"totalAvailable\":0,\"generation\":{\"id\":\"job-1\",\"status\":\"complete\"}",
                )
            }
            respond(body, HttpStatusCode.OK, jsonHeaders())
        }

        val search = api.pollSearchGeneration("job-1")
        val glossary = api.pollContextGlossaryGeneration("job-2")

        assertEquals(
            listOf(
                "/v1/search/generations/job-1",
                "/v1/search/context-glossary/generations/job-2",
            ),
            paths,
        )
        assertEquals("complete", search.meta.generation?.status)
        assertEquals("generating", glossary.generation?.status)
    }

    @Test
    fun searchGenerationPollPreservesInitialLanguageOrientation() = runBlocking {
        val requests = mutableListOf<Triple<String, String?, String?>>()
        val api = apiWithEngine { request ->
            requests += Triple(
                request.url.encodedPath,
                request.url.parameters["speak"],
                request.url.parameters["learn"],
            )
            val body = if (request.url.encodedPath == "/v1/search") {
                searchResponse().replace(
                    "\"totalAvailable\":0",
                    "\"totalAvailable\":0,\"generation\":{\"id\":\"job-oriented\",\"status\":\"generating\",\"retryAfterMs\":1}",
                )
            } else {
                searchResponse()
            }
            respond(body, HttpStatusCode.OK, jsonHeaders())
        }

        RemoteLexiconSearchUseCase(api)("run", "en", "uk", emptySet())

        assertEquals(listOf("/v1/search", "/v1/search/generations/job-oriented"), requests.map { it.first })
        assertEquals(listOf("en", "en"), requests.map { it.second })
        assertEquals(listOf("uk", "uk"), requests.map { it.third })
    }

    @Test
    fun expiredAccessTokenRefreshesAndReplaysTheRequest() = runBlocking {
        val requests = mutableListOf<String>()
        val api = apiWithEngine { request ->
            val path = request.url.encodedPath
            requests += "$path ${request.headers[HttpHeaders.Authorization].orEmpty()}"
            when (path) {
                "/v1/search" -> if (request.headers[HttpHeaders.Authorization] == "Bearer expired") {
                    unauthorizedResponse()
                } else {
                    respond(
                        content = searchResponse(),
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders(),
                    )
                }
                "/v1/auth/refresh" -> respond(
                    content = """{"accessToken":"fresh","refreshToken":"rotated","expiresIn":900}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
                else -> error("Unexpected path: $path")
            }
        }

        val result = api.search("bee", "uk", "en")

        assertEquals("bee", result.query)
        assertEquals(
            listOf(
                "/v1/search Bearer expired",
                "/v1/auth/refresh ",
                "/v1/search Bearer fresh",
            ),
            requests,
        )
    }

    /**
     * Відмова refresh — не привід гасити сесію: юзер лишається залогіненим,
     * токени лишаються в prefs, щоб наступна спроба могла відновити сесію.
     * Вихід із акаунта робить тільки явний logout. Але публічний `/search`
     * (D2) при цьому не блокується — він повторюється анонімно, без bearer.
     */
    @Test
    fun failedRefreshKeepsTheLocalSessionAndFallsBackToAnonymousSearch() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "stored-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val requests = mutableListOf<String>()
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                val path = request.url.encodedPath
                requests += "$path ${request.headers[HttpHeaders.Authorization].orEmpty()}"
                when (path) {
                    "/v1/search" -> if (request.headers[HttpHeaders.Authorization] == null) {
                        respond(searchResponse(), HttpStatusCode.OK, jsonHeaders())
                    } else {
                        unauthorizedResponse()
                    }
                    "/v1/auth/refresh" -> unauthorizedResponse()
                    else -> error("Unexpected path: $path")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        val result = api.search("bee", "uk", "en")

        assertEquals("bee", result.query)
        assertEquals(
            listOf(
                "/v1/search Bearer expired",
                "/v1/auth/refresh ",
                "/v1/search ",
            ),
            requests,
        )
        assertEquals("expired", preferences.accessToken)
        assertEquals("stored-refresh", preferences.refreshToken)
        // UI лише пропонує повторний вхід — сесія локально жива.
        assertTrue(tokenStore.sessionNeedsReauth.value)
    }

    /**
     * Ротація одноразова: якщо інший запит уже обміняв refresh, поки цей був у польоті,
     * пред'явлений токен повертає 401. Це не мертва сесія — треба повторити зі свіжим
     * токеном із prefs, а не викидати юзера.
     */
    @Test
    fun rotatedRefreshTokenIsRetriedInsteadOfDroppingTheSession() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "rotated-away"
        }
        val tokenStore = AuthTokenStore(preferences)
        val sessionGeneration = tokenStore.captureSession().generation
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> if (request.headers[HttpHeaders.Authorization] == "Bearer fresh") {
                        respond(searchResponse(), HttpStatusCode.OK, jsonHeaders())
                    } else {
                        unauthorizedResponse()
                    }
                    "/v1/auth/refresh" -> if (request.bodyText().contains("rotated-away")) {
                        // Паралельний refresh уже завершив ротацію тієї самої сесії.
                        tokenStore.rotateSession(
                            AuthTokensResponse(
                                accessToken = "concurrent-fresh",
                                refreshToken = "current-refresh",
                                expiresIn = 900,
                            ),
                            expectedGeneration = sessionGeneration,
                        )
                        unauthorizedResponse()
                    } else {
                        respond(
                            content = """{"accessToken":"fresh","refreshToken":"next","expiresIn":900}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders(),
                        )
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        val result = api.search("bee", "uk", "en")

        assertEquals("bee", result.query)
        assertEquals("next", preferences.refreshToken)
        assertFalse(tokenStore.sessionNeedsReauth.value)
    }

    /**
     * Сервер відкликає пред'явлений refresh у момент обробки. Якщо скасувати корутину
     * (юзер вийшов з екрана) до запису нової пари, локально лишиться вже відкликаний
     * токен — сесія стає мертвою назавжди. Тому ротація дописується поза скасуванням.
     */
    @Test
    fun cancelledCallerStillPersistsTheRotatedTokens() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "original-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val refreshReceived = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> unauthorizedResponse()
                    "/v1/auth/refresh" -> {
                        refreshReceived.complete(Unit)
                        releaseRefresh.await()
                        respond(
                            content = """{"accessToken":"fresh","refreshToken":"rotated","expiresIn":900}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders(),
                        )
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        val job = launch { runCatching { api.search("bee", "uk", "en") } }
        refreshReceived.await()
        job.cancel()
        releaseRefresh.complete(Unit)
        job.join()
        // Ротація дописується поза скасуванням — даємо їй завершитись.
        withTimeout(5_000) {
            while (preferences.refreshToken == "original-refresh") yield()
        }

        assertEquals("rotated", preferences.refreshToken)
        assertEquals("fresh", preferences.accessToken)
        assertFalse(tokenStore.sessionNeedsReauth.value)
    }

    @Test
    fun staleRefreshCannotOverwriteANewerLoginSession() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "account-a-expired"
            refreshToken = "account-a-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val refreshReceived = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> unauthorizedResponse()
                    "/v1/auth/refresh" -> {
                        refreshReceived.complete(Unit)
                        releaseRefresh.await()
                        respond(
                            content = """{"accessToken":"account-a-fresh","refreshToken":"account-a-rotated","expiresIn":900}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders(),
                        )
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )
        val outcome = CompletableDeferred<Result<SearchResponse>>()

        val requestJob = launch {
            outcome.complete(runCatching { api.search("bee", "uk", "en") })
        }
        refreshReceived.await()
        tokenStore.replaceSession(
            AuthTokensResponse(
                accessToken = "account-b-access",
                refreshToken = "account-b-refresh",
                expiresIn = 900,
            ),
        )
        releaseRefresh.complete(Unit)
        requestJob.join()

        val error = assertNotNull(outcome.await().exceptionOrNull() as? VocabeeApiException)
        assertEquals("auth_session_changed", error.errorType)
        assertEquals("account-b-access", tokenStore.current())
        assertEquals("account-b-refresh", tokenStore.refreshToken())
        assertEquals("account-b-access", preferences.accessToken)
        assertEquals("account-b-refresh", preferences.refreshToken)
    }

    @Test
    fun staleRefreshCannotRestoreALoggedOutSession() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "account-a-expired"
            refreshToken = "account-a-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val refreshReceived = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> unauthorizedResponse()
                    "/v1/auth/refresh" -> {
                        refreshReceived.complete(Unit)
                        releaseRefresh.await()
                        respond(
                            content = """{"accessToken":"account-a-fresh","refreshToken":"account-a-rotated","expiresIn":900}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders(),
                        )
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )
        val outcome = CompletableDeferred<Result<SearchResponse>>()

        val requestJob = launch {
            outcome.complete(runCatching { api.search("bee", "uk", "en") })
        }
        refreshReceived.await()
        tokenStore.clear()
        releaseRefresh.complete(Unit)
        requestJob.join()

        val error = assertNotNull(outcome.await().exceptionOrNull() as? VocabeeApiException)
        assertEquals("auth_session_changed", error.errorType)
        assertNull(tokenStore.current())
        assertNull(tokenStore.refreshToken())
        assertNull(preferences.accessToken)
        assertNull(preferences.refreshToken)
    }

    @Test
    fun mutatingRequestIsNeverReplayedWithAnotherAccountsBearer() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "account-a-access"
            refreshToken = "account-a-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val applyBearers = mutableListOf<String?>()
        var refreshRequests = 0
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/topics/sync/apply" -> {
                        applyBearers += request.headers[HttpHeaders.Authorization]
                        tokenStore.replaceSession(
                            AuthTokensResponse(
                                accessToken = "account-b-access",
                                refreshToken = "account-b-refresh",
                                expiresIn = 900,
                            ),
                        )
                        unauthorizedResponse()
                    }
                    "/v1/auth/refresh" -> {
                        refreshRequests += 1
                        error("A stale request must not refresh another account's session")
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        val result = runCatching {
            api.applySync(
                ApplySyncRequest(
                    expectedUserId = "00000000-0000-4000-8000-00000000000a",
                    topics = emptyList(),
                    words = emptyList(),
                ),
            )
        }

        val error = assertNotNull(result.exceptionOrNull() as? VocabeeApiException)
        assertEquals("auth_session_changed", error.errorType)
        assertEquals(listOf<String?>("Bearer account-a-access"), applyBearers)
        assertEquals(0, refreshRequests)
        assertEquals("account-b-access", tokenStore.current())
        assertEquals("account-b-refresh", tokenStore.refreshToken())
    }

    @Test
    fun concurrentRequestsWithoutAccessShareOneRefreshRotation() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = null
            refreshToken = "shared-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val refreshReceived = CompletableDeferred<Unit>()
        val releaseRefresh = CompletableDeferred<Unit>()
        val searchBearers = mutableListOf<String?>()
        var refreshRequests = 0
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/auth/refresh" -> {
                        refreshRequests += 1
                        refreshReceived.complete(Unit)
                        releaseRefresh.await()
                        respond(
                            content = """{"accessToken":"shared-access","refreshToken":"rotated-refresh","expiresIn":900}""",
                            status = HttpStatusCode.OK,
                            headers = jsonHeaders(),
                        )
                    }
                    "/v1/search" -> {
                        searchBearers += request.headers[HttpHeaders.Authorization]
                        respond(searchResponse(), HttpStatusCode.OK, jsonHeaders())
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        val first = async { api.search("bee", "uk", "en") }
        refreshReceived.await()
        val second = async(start = CoroutineStart.UNDISPATCHED) {
            api.search("bee", "uk", "en")
        }
        releaseRefresh.complete(Unit)

        assertEquals("bee", first.await().query)
        assertEquals("bee", second.await().query)
        assertEquals(1, refreshRequests)
        assertEquals(
            listOf<String?>("Bearer shared-access", "Bearer shared-access"),
            searchBearers,
        )
        assertEquals("shared-access", tokenStore.current())
        assertEquals("rotated-refresh", tokenStore.refreshToken())
    }

    @Test
    fun successfulResponseFromAChangedSessionIsRejected() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "account-a-access"
            refreshToken = "account-a-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val responseStarted = CompletableDeferred<Unit>()
        val releaseResponse = CompletableDeferred<Unit>()
        val searchBearers = mutableListOf<String?>()
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> {
                        searchBearers += request.headers[HttpHeaders.Authorization]
                        responseStarted.complete(Unit)
                        releaseResponse.await()
                        respond(searchResponse(), HttpStatusCode.OK, jsonHeaders())
                    }
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )
        val outcome = CompletableDeferred<Result<SearchResponse>>()

        val requestJob = launch {
            outcome.complete(runCatching { api.search("bee", "uk", "en") })
        }
        responseStarted.await()
        tokenStore.replaceSession(
            AuthTokensResponse(
                accessToken = "account-b-access",
                refreshToken = "account-b-refresh",
                expiresIn = 900,
            ),
        )
        releaseResponse.complete(Unit)
        requestJob.join()

        val result = outcome.await()
        assertTrue(result.isFailure)
        val error = assertNotNull(result.exceptionOrNull() as? VocabeeApiException)
        assertEquals("auth_session_changed", error.errorType)
        assertEquals(listOf<String?>("Bearer account-a-access"), searchBearers)
        assertEquals("account-b-access", tokenStore.current())
        assertEquals("account-b-refresh", tokenStore.refreshToken())
    }

    private fun apiWithEngine(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): KtorVocabeeApi {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "original-refresh"
        }
        return KtorVocabeeApi(
            client = clientWithEngine(handler),
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = AuthTokenStore(preferences),
        )
    }

    private fun clientWithEngine(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = HttpClient(MockEngine { request -> handler(request) }) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = true
    }

    private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

    private fun MockRequestHandleScope.unauthorizedResponse() = respond(
        content = """{"statusCode":401,"error":"unauthorized","message":"Потрібна повторна авторизація."}""",
        status = HttpStatusCode.Unauthorized,
        headers = jsonHeaders(),
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, "application/json")

    private fun searchResponse() = """
        {
          "query":"bee", "detectedLang":"en", "isPhrase":false,
          "knownLang":"uk", "learningLang":"en", "tier":"registered", "maxResults":50,
          "results":[],
          "meta":{"totalAvailable":0}
        }
    """.trimIndent()
}
