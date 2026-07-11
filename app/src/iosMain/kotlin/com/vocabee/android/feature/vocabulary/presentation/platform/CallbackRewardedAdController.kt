package com.vocabee.android.feature.vocabulary.presentation.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Bridges the native (Swift/AdMob) rewarded-ad presenter into the shared
 * [RewardedAdController] interface. The Swift side only speaks primitives — a
 * result code plus an optional message — so all [RewardedAdResult] construction
 * stays in Kotlin and Swift needs no fragile framework-type bindings.
 *
 * Result codes: 0 = reward earned, 1 = dismissed without reward, 2 = failed.
 */
class CallbackRewardedAdController(
    private val present: (onResult: (Int, String?) -> Unit) -> Unit,
) : RewardedAdController {
    override suspend fun showRewardedAd(): RewardedAdResult =
        suspendCancellableCoroutine { continuation ->
            present { code, message ->
                if (!continuation.isActive) return@present
                continuation.resume(
                    when (code) {
                        0 -> RewardedAdResult.RewardEarned
                        1 -> RewardedAdResult.Dismissed
                        else -> RewardedAdResult.Failed(message ?: "Не вдалося показати рекламу")
                    },
                )
            }
        }
}
