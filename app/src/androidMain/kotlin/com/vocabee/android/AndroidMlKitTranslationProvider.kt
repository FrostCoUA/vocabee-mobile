package com.vocabee.android

import android.content.Context
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.vocabee.android.platform.MachineTranslationProvider

class AndroidMlKitTranslationProvider(
    private val context: Context,
) : MachineTranslationProvider {
    override val isSupported: Boolean = true

    private val translators = mutableMapOf<String, Translator>()

    override fun translate(
        sourceLanguageCode: String,
        targetLanguageCode: String,
        text: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) return

        val sourceLanguage = TranslateLanguage.fromLanguageTag(sourceLanguageCode)
        val targetLanguage = TranslateLanguage.fromLanguageTag(targetLanguageCode)

        if (sourceLanguage == null || targetLanguage == null) {
            onError(context.getString(R.string.mlkit_error_unsupported_pair))
            return
        }

        if (sourceLanguage == targetLanguage) {
            onSuccess(cleanedText)
            return
        }

        val translator = translatorFor(sourceLanguage, targetLanguage)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                translator.translate(cleanedText)
                    .addOnSuccessListener { translatedText ->
                        onSuccess(translatedText.trim())
                    }
                    .addOnFailureListener { error ->
                        onError(error.localizedMessage ?: context.getString(R.string.mlkit_error_translate_failed))
                    }
            }
            .addOnFailureListener { error ->
                onError(error.localizedMessage ?: context.getString(R.string.mlkit_error_model_download_failed))
            }
    }

    override fun close() {
        translators.values.forEach { translator -> translator.close() }
        translators.clear()
    }

    private fun translatorFor(
        sourceLanguage: String,
        targetLanguage: String,
    ): Translator {
        val key = "$sourceLanguage:$targetLanguage"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
            Translation.getClient(options)
        }
    }
}
