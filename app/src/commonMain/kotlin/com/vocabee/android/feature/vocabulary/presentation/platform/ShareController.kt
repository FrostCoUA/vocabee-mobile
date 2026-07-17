package com.vocabee.android.feature.vocabulary.presentation.platform

/** Opens the platform share sheet with referral copy and its PNG QR code. */
interface ShareController {
    fun shareInvite(text: String, qrCodePng: ByteArray)
}

object NoShareController : ShareController {
    override fun shareInvite(text: String, qrCodePng: ByteArray) = Unit
}
