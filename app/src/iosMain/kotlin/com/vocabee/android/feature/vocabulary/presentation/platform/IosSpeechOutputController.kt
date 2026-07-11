package com.vocabee.android.feature.vocabulary.presentation.platform

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/** AVSpeechSynthesizer-backed TTS — the iOS twin of AndroidSpeechOutputController. */
class IosSpeechOutputController : SpeechOutputController {
    private val synthesizer = AVSpeechSynthesizer()

    override val isSupported: Boolean = true

    override fun speak(text: String, languageTag: String) {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(IMMEDIATE_BOUNDARY)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)?.let { voice ->
            utterance.voice = voice
        }
        synthesizer.speakUtterance(utterance)
    }

    override fun shutdown() {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(IMMEDIATE_BOUNDARY)
        }
    }

    private companion object {
        val IMMEDIATE_BOUNDARY = AVSpeechBoundary.AVSpeechBoundaryImmediate
    }
}
