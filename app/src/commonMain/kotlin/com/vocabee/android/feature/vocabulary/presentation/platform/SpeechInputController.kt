package com.vocabee.android.feature.vocabulary.presentation.platform

interface SpeechInputController {
    val isSupported: Boolean

    fun startListening(
        languageTag: String,
        alternativeLanguageTags: List<String> = emptyList(),
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
    )

    fun stopListening()
}

object NoSpeechInputController : SpeechInputController {
    override val isSupported: Boolean = false

    override fun startListening(
        languageTag: String,
        alternativeLanguageTags: List<String>,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
    ) {
        onListeningChanged(false)
        onError("Voice input is not available on this platform")
    }

    override fun stopListening() = Unit
}
