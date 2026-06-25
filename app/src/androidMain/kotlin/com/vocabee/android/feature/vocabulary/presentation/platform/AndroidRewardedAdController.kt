package com.vocabee.android.feature.vocabulary.presentation.platform

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidRewardedAdController(
    private val activity: Activity,
    private val adUnitId: String,
) : RewardedAdController {

    private var rewardedAd: RewardedAd? = null
    private var isLoading: Boolean = false

    init {
        loadRewardedAd()
    }

    override suspend fun showRewardedAd(): RewardedAdResult {
        val readyAd = rewardedAd
        return if (readyAd != null) {
            showLoadedAd(readyAd)
        } else {
            suspendCancellableCoroutine { continuation ->
                loadRewardedAd { loadedAd, error ->
                    if (!continuation.isActive) return@loadRewardedAd
                    if (loadedAd == null) {
                        continuation.resume(
                            RewardedAdResult.Failed(error ?: "Реклама ще не готова"),
                        )
                    } else {
                        rewardedAd = loadedAd
                        showLoadedAd(loadedAd) { result ->
                            if (continuation.isActive) continuation.resume(result)
                        }
                    }
                }
            }
        }
    }

    private suspend fun showLoadedAd(ad: RewardedAd): RewardedAdResult {
        return suspendCancellableCoroutine { continuation ->
            showLoadedAd(ad) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }
    }

    private fun showLoadedAd(
        ad: RewardedAd,
        onResult: (RewardedAdResult) -> Unit,
    ) {
        var earnedReward = false
        var resultDelivered = false

        fun deliver(result: RewardedAdResult) {
            if (resultDelivered) return
            resultDelivered = true
            onResult(result)
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
                deliver(if (earnedReward) RewardedAdResult.RewardEarned else RewardedAdResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd()
                deliver(RewardedAdResult.Failed(adError.message))
            }
        }

        rewardedAd = null
        ad.show(activity) {
            earnedReward = true
        }
    }

    private fun loadRewardedAd(
        onComplete: ((RewardedAd?, String?) -> Unit)? = null,
    ) {
        if (isLoading) {
            onComplete?.invoke(null, "Реклама ще завантажується")
            return
        }
        isLoading = true
        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                    onComplete?.invoke(ad, null)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    onComplete?.invoke(null, error.message)
                }
            },
        )
    }
}
