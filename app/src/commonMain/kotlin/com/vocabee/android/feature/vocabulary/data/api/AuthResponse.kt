package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokensResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)

@Serializable
data class GoogleAuthRequest(
    val idToken: String,
    val speakLang: String? = null,
    val learnLang: String? = null,
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val speakLang: String,
    val learnLang: String,
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean,
    val isAnonymous: Boolean,
    val isPremium: Boolean,
    val beeBalance: Int = 50,
    val createdAt: String,
    val updatedAt: String,
)
