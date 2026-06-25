package com.vocabee.android.feature.vocabulary.presentation.platform

sealed interface GoogleAuthResult {
    data class Success(val idToken: String) : GoogleAuthResult
    data object NotConfigured : GoogleAuthResult
    data object Cancelled : GoogleAuthResult
    data class Failure(val message: String) : GoogleAuthResult
}

interface GoogleAuthController {
    suspend fun requestIdToken(): GoogleAuthResult
}

object NoGoogleAuthController : GoogleAuthController {
    override suspend fun requestIdToken(): GoogleAuthResult = GoogleAuthResult.NotConfigured
}
