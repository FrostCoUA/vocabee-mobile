package com.vocabee.android.feature.vocabulary.presentation.platform

sealed interface RewardedAdResult {
    data object RewardEarned : RewardedAdResult
    data object Dismissed : RewardedAdResult
    data class Failed(val message: String) : RewardedAdResult
}

interface RewardedAdController {
    suspend fun showRewardedAd(): RewardedAdResult
}

object NoRewardedAdController : RewardedAdController {
    override suspend fun showRewardedAd(): RewardedAdResult {
        return RewardedAdResult.Failed("Реклама недоступна на цій платформі")
    }
}
