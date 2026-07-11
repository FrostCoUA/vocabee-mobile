package com.vocabee.android.feature.vocabulary.presentation.platform

import android.app.Activity
import android.content.Intent

class AndroidShareController(
    private val activity: Activity,
) : ShareController {
    override fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        activity.startActivity(Intent.createChooser(sendIntent, null))
    }
}
