package com.vocabee.android.feature.vocabulary.presentation.platform

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

class AndroidShareController(
    private val activity: Activity,
) : ShareController {
    override fun shareInvite(text: String, qrCodePng: ByteArray) {
        val shareDirectory = File(activity.cacheDir, "shared_invites").apply { mkdirs() }
        val qrCodeFile = File(shareDirectory, "vocabee-invite-qr.png").apply {
            writeBytes(qrCodePng)
        }
        val qrCodeUri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            qrCodeFile,
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, qrCodeUri)
            clipData = ClipData.newRawUri("QR-код запрошення Vocabee", qrCodeUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(sendIntent, null))
    }
}
