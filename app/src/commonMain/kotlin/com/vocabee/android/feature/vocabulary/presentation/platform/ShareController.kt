package com.vocabee.android.feature.vocabulary.presentation.platform

/**
 * Месенджери зі швидких кнопок екрана «Запросити друзів» (design board 12).
 * Показуються ЛИШЕ встановлені на пристрої — порядок фіксований цим enum.
 */
enum class InviteMessenger(val packageId: String) {
    Telegram("org.telegram.messenger"),
    WhatsApp("com.whatsapp"),
    Viber("com.viber.voip"),
}

/** Opens the platform share sheet with referral copy and its PNG QR code. */
interface ShareController {
    fun shareInvite(text: String, qrCodePng: ByteArray)

    /** Встановлені месенджери; порожній список = показуємо лише кнопку «Ще». */
    fun installedMessengers(): List<InviteMessenger> = emptyList()

    /** Пряме відправлення в месенджер; фолбек — системний share sheet. */
    fun shareInviteTo(messenger: InviteMessenger, text: String, qrCodePng: ByteArray) =
        shareInvite(text, qrCodePng)
}

object NoShareController : ShareController {
    override fun shareInvite(text: String, qrCodePng: ByteArray) = Unit
}
