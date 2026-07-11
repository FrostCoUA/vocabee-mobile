package com.vocabee.android.feature.vocabulary.presentation.platform

/** Opens the platform share sheet with plain text (referral link etc.). */
interface ShareController {
    fun shareText(text: String)
}

object NoShareController : ShareController {
    override fun shareText(text: String) = Unit
}
