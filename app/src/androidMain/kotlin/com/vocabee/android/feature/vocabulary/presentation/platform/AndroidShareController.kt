package com.vocabee.android.feature.vocabulary.presentation.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidShareController(
    private val activity: Activity,
) : ShareController {
    override fun shareInvite(text: String, qrCodePng: ByteArray) {
        activity.startActivity(Intent.createChooser(buildSendIntent(text, qrCodePng), null))
    }

    /**
     * Пакети видимі лише завдяки `<queries>` в AndroidManifest (Android 11+),
     * інакше resolveActivity завжди повертає null.
     */
    override fun installedMessengers(): List<InviteMessenger> =
        InviteMessenger.entries.filter { messenger ->
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(messenger.packageId)
            }.resolveActivity(activity.packageManager) != null
        }

    override fun shareInviteTo(messenger: InviteMessenger, text: String, qrCodePng: ByteArray) {
        val intent = buildSendIntent(text, qrCodePng).apply { setPackage(messenger.packageId) }
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Месенджер зник між резолвом і тапом — падаємо у системний share sheet.
            shareInvite(text, qrCodePng)
        }
    }

    private fun buildSendIntent(text: String, qrCodePng: ByteArray): Intent {
        val shareDirectory = File(activity.cacheDir, "shared_invites").apply { mkdirs() }
        val qrCodeFile = File(shareDirectory, "vocabee-invite-qr.png").apply {
            writeBytes(qrCodePng)
        }
        val qrCodeUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            qrCodeFile,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, qrCodeUri)
            clipData = ClipData.newRawUri("QR-код запрошення Vocabee", qrCodeUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
