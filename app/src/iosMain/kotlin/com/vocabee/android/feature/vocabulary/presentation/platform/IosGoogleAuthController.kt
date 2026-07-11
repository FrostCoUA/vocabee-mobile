package com.vocabee.android.feature.vocabulary.presentation.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Google sign-in without the GoogleSignIn SDK: a standard OAuth
 * authorization-code flow with PKCE through the system
 * [ASWebAuthenticationSession]. Returns the Google idToken that the gateway
 * verifies (audience = this iOS client id).
 */
class IosGoogleAuthController(
    private val client: HttpClient,
    private val clientId: String,
) : GoogleAuthController {

    // Google's iOS clients redirect to the reversed client id scheme.
    private val redirectScheme = clientId
        .removeSuffix(".apps.googleusercontent.com")
        .let { id -> "com.googleusercontent.apps.$id" }
    private val redirectUri = "$redirectScheme:/oauth2redirect"

    override suspend fun requestIdToken(): GoogleAuthResult {
        if (clientId.isBlank()) return GoogleAuthResult.NotConfigured

        val codeVerifier = randomUrlSafeString()
        val authCode = when (val outcome = authorize(codeVerifier)) {
            is AuthorizeOutcome.Code -> outcome.value
            AuthorizeOutcome.Cancelled -> return GoogleAuthResult.Cancelled
            is AuthorizeOutcome.Failed -> return GoogleAuthResult.Failure(outcome.message)
        }

        return try {
            val tokens: GoogleTokenResponse = client.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = parameters {
                    append("client_id", clientId)
                    append("code", authCode)
                    append("code_verifier", codeVerifier)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", redirectUri)
                },
            ).body()
            val idToken = tokens.idToken
            if (idToken.isNullOrBlank()) {
                GoogleAuthResult.Failure("Google не повернув idToken")
            } else {
                GoogleAuthResult.Success(idToken)
            }
        } catch (cause: Exception) {
            GoogleAuthResult.Failure(cause.message ?: "Не вдалося обміняти код на токен")
        }
    }

    private sealed interface AuthorizeOutcome {
        data class Code(val value: String) : AuthorizeOutcome
        data object Cancelled : AuthorizeOutcome
        data class Failed(val message: String) : AuthorizeOutcome
    }

    private suspend fun authorize(codeVerifier: String): AuthorizeOutcome =
        suspendCancellableCoroutine { continuation ->
            dispatch_async(dispatch_get_main_queue()) {
                val authUrl = NSURLComponents(string = "https://accounts.google.com/o/oauth2/v2/auth").apply {
                    queryItems = listOf(
                        NSURLQueryItem(name = "client_id", value = clientId),
                        NSURLQueryItem(name = "redirect_uri", value = redirectUri),
                        NSURLQueryItem(name = "response_type", value = "code"),
                        NSURLQueryItem(name = "scope", value = "openid email profile"),
                        NSURLQueryItem(name = "code_challenge", value = sha256Base64Url(codeVerifier)),
                        NSURLQueryItem(name = "code_challenge_method", value = "S256"),
                    )
                }.URL

                if (authUrl == null) {
                    continuation.resume(AuthorizeOutcome.Failed("Некоректний auth URL"))
                    return@dispatch_async
                }

                val session = ASWebAuthenticationSession(
                    uRL = authUrl,
                    callbackURLScheme = redirectScheme,
                ) { callbackUrl: NSURL?, error: NSError? ->
                    if (!continuation.isActive) return@ASWebAuthenticationSession
                    when {
                        error != null -> {
                            if (error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin) {
                                continuation.resume(AuthorizeOutcome.Cancelled)
                            } else {
                                continuation.resume(
                                    AuthorizeOutcome.Failed(
                                        error.localizedDescription ?: "Помилка авторизації",
                                    ),
                                )
                            }
                        }

                        callbackUrl != null -> {
                            val code = NSURLComponents(uRL = callbackUrl, resolvingAgainstBaseURL = false)
                                .queryItems
                                ?.filterIsInstance<NSURLQueryItem>()
                                ?.firstOrNull { it.name == "code" }
                                ?.value
                            if (code.isNullOrBlank()) {
                                continuation.resume(AuthorizeOutcome.Failed("Відповідь Google без коду"))
                            } else {
                                continuation.resume(AuthorizeOutcome.Code(code))
                            }
                        }

                        else -> continuation.resume(AuthorizeOutcome.Failed("Порожня відповідь Google"))
                    }
                }
                session.presentationContextProvider = presentationContextProvider
                session.prefersEphemeralWebBrowserSession = false
                if (!session.start()) {
                    if (continuation.isActive) {
                        continuation.resume(AuthorizeOutcome.Failed("Не вдалося відкрити вікно входу"))
                    }
                }
                continuation.invokeOnCancellation {
                    dispatch_async(dispatch_get_main_queue()) { session.cancel() }
                }
            }
        }

    private val presentationContextProvider =
        object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
            override fun presentationAnchorForWebAuthenticationSession(
                session: ASWebAuthenticationSession,
            ): ASPresentationAnchor {
                val windows = UIApplication.sharedApplication.windows.filterIsInstance<UIWindow>()
                return windows.firstOrNull { it.isKeyWindow() } ?: windows.first()
            }
        }

    @Serializable
    private data class GoogleTokenResponse(
        @SerialName("id_token") val idToken: String? = null,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun randomUrlSafeString(byteCount: Int = 32): String {
    val bytes = ByteArray(byteCount)
    bytes.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, byteCount.toULong(), pinned.addressOf(0))
    }
    return bytes.toBase64Url()
}

@OptIn(ExperimentalForeignApi::class)
private fun sha256Base64Url(input: String): String {
    val inputBytes = input.encodeToByteArray()
    val digest = memScoped {
        val out = allocArray<kotlinx.cinterop.UByteVar>(CC_SHA256_DIGEST_LENGTH)
        inputBytes.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), inputBytes.size.toUInt(), out)
        }
        out.readBytes(CC_SHA256_DIGEST_LENGTH)
    }
    return digest.toBase64Url()
}

@OptIn(ExperimentalEncodingApi::class)
private fun ByteArray.toBase64Url(): String {
    return Base64.UrlSafe.encode(this).trimEnd('=')
}
