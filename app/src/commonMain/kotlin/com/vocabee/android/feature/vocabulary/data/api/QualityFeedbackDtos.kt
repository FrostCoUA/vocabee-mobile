package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable

/** Body accepted by POST /v1/translation-feedback. */
@Serializable
data class QualityFeedbackRequest(
    val targetType: String,
    val targetId: String,
    val comment: String? = null,
)

@Serializable
data class QualityFeedbackResponse(
    val targetType: String,
    val targetId: String,
    val accepted: Boolean,
    val qualityScore: Int,
    val regenerationThreshold: Int,
)
