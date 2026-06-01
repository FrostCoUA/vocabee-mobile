package com.vocabee.android.platform

/**
 * Speaks a piece of text in the requested language. Backed by Android's
 * `TextToSpeech` on Android; a no-op fallback is supplied for previews / common
 * tests where audio output is unavailable.
 */
interface SpeechOutputController {
    /** Whether the platform actually has a working TTS engine for this controller. */
    val isSupported: Boolean

    /**
     * Speak [text] using the given [languageTag] (BCP-47, e.g. `en-US`, `uk-UA`).
     * Returns immediately; audio plays asynchronously. Calling [speak] again
     * cancels any in-progress utterance.
     */
    fun speak(text: String, languageTag: String)

    /** Stop any current utterance and release platform resources. */
    fun shutdown()
}

object NoSpeechOutputController : SpeechOutputController {
    override val isSupported: Boolean = false
    override fun speak(text: String, languageTag: String) = Unit
    override fun shutdown() = Unit
}
