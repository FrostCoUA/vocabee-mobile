package com.vocabee.android.feature.vocabulary.presentation.platform

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class AndroidGoogleAuthController(
    private val context: Context,
    private val webClientId: String,
) : GoogleAuthController {
    private val credentialManager = CredentialManager.create(context)

    override suspend fun requestIdToken(): GoogleAuthResult {
        if (webClientId.isBlank()) return GoogleAuthResult.NotConfigured

        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = try {
            credentialManager.getCredential(context, request).credential
        } catch (cause: GetCredentialCancellationException) {
            Log.w(Tag, "Google sign-in was interrupted by Credential Manager", cause)
            return GoogleAuthResult.Failure(
                "Google перервав вхід після вибору акаунта. Перевір Android OAuth client: package com.vocabee.android і SHA-1 цієї збірки."
            )
        } catch (_: NoCredentialException) {
            return GoogleAuthResult.Failure(
                "Google не знайшов акаунт для входу. Перевір Android OAuth client: package com.vocabee.android і debug SHA-1."
            )
        } catch (cause: GetCredentialException) {
            return GoogleAuthResult.Failure(cause.message ?: "Не вдалося відкрити Google авторизацію")
        }

        if (
            credential !is CustomCredential ||
            credential.type !in SupportedGoogleCredentialTypes
        ) {
            return GoogleAuthResult.Failure("Google не повернув id token: ${credential.type}")
        }

        return try {
            GoogleAuthResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } catch (_: GoogleIdTokenParsingException) {
            GoogleAuthResult.Failure("Не вдалося прочитати Google id token")
        }
    }

    private companion object {
        const val Tag = "VocabeeGoogleAuth"

        val SupportedGoogleCredentialTypes = setOf(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
        )
    }
}
