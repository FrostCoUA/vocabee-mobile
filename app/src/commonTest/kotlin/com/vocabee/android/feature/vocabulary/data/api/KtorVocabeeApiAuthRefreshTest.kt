package com.vocabee.android.feature.vocabulary.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun invalidRefreshTokenClearsTheLocalSession() = runBlocking {
        val preferences = InMemoryPreferencesManager().apply {
            accessToken = "expired"
            refreshToken = "invalid-refresh"
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

        assertNull(preferences.accessToken)
        assertNull(preferences.refreshToken)
        assertTrue(tokenStore.sessionExpired.value)
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
