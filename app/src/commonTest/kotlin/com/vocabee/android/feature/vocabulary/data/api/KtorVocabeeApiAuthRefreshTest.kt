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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtorVocabeeApiAuthRefreshTest {
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
     * Вихід із акаунта робить тільки явний logout.
     */
    @Test
    fun failedRefreshKeepsTheLocalSession() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "stored-refresh"
        }
        val tokenStore = AuthTokenStore(preferences)
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search", "/v1/auth/refresh" -> unauthorizedResponse()
                    else -> error("Unexpected path: ${request.url.encodedPath}")
                }
            },
            config = VocabeeApiConfig(baseUrl = "https://test.vocabee"),
            tokenStore = tokenStore,
        )

        runCatching { api.search("bee", "uk", "en") }

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
        val api = KtorVocabeeApi(
            client = clientWithEngine { request ->
                when (request.url.encodedPath) {
                    "/v1/search" -> if (request.headers[HttpHeaders.Authorization] == "Bearer fresh") {
                        respond(searchResponse(), HttpStatusCode.OK, jsonHeaders())
                    } else {
                        unauthorizedResponse()
                    }
                    "/v1/auth/refresh" -> if (request.bodyText().contains("rotated-away")) {
                        // Паралельний refresh уже завершив ротацію і поклав свіжу пару в prefs.
                        preferences.refreshToken = "current-refresh"
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
