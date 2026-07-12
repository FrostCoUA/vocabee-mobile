@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.vocabee.android.feature.vocabulary.presentation.platform

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive

/** AVSpeechSynthesizer-backed TTS — the iOS twin of AndroidSpeechOutputController. */
class IosSpeechOutputController : SpeechOutputController {
    private val synthesizer = AVSpeechSynthesizer()

    override val isSupported: Boolean = true

    override fun speak(text: String, languageTag: String) {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(IMMEDIATE_BOUNDARY)
        }
        // Категорію треба виставляти перед КОЖНИМ мовленням: дефолтний
        // .soloAmbient поважає беззвучний перемикач (на пристрої це «кнопка
        // не працює»), а після голосового вводу сесія взагалі лишається в
        // .record (STT ставить свою категорію так само щоразу). Playback
        // ігнорує mute switch; mixWithOthers не глушить чужу музику і не
        // потребує деактивації після фрази.
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayback,
            withOptions = AVAudioSessionCategoryOptionMixWithOthers,
            error = null,
        )
        session.setActive(true, null)

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
