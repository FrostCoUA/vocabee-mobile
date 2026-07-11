package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ReferralResponse(
    val code: String,
    val link: String,
)

@Serializable
data class SupportRequestBody(
    val topic: String,
    val message: String,
    val email: String? = null,
)

@Serializable
data class SupportResponse(
    val id: String,
)
