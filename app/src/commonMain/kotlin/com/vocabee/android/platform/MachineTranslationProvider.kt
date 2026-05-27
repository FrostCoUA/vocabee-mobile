package com.vocabee.android.platform

interface MachineTranslationProvider {
    val isSupported: Boolean

    fun translate(
        sourceLanguageCode: String,
        targetLanguageCode: String,
        text: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    )

    fun close()
}

object NoMachineTranslationProvider : MachineTranslationProvider {
    override val isSupported: Boolean = false

    override fun translate(
        sourceLanguageCode: String,
        targetLanguageCode: String,
        text: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        onError("Translation is not available on this platform")
    }

    override fun close() = Unit
}
