@file:OptIn(ExperimentalForeignApi::class)

package com.vocabee.android.feature.vocabulary.presentation.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS speech-to-text — the twin of AndroidSpeechInputController. Uses
 * [SFSpeechRecognizer] for transcription and [AVAudioEngine] to stream mic
 * audio into the recognition request. All user callbacks are marshalled to the
 * main queue (recognizer/tap fire on private queues).
 */
class IosSpeechInputController : SpeechInputController {

    private val audioEngine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest? = null
    private var task: SFSpeechRecognitionTask? = null
    private var listeningChanged: ((Boolean) -> Unit)? = null

    override val isSupported: Boolean = true

    override fun startListening(
        languageTag: String,
        alternativeLanguageTags: List<String>,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
    ) {
        listeningChanged = onListeningChanged

        SFSpeechRecognizer.requestAuthorization { status ->
            onMain {
                if (status != SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    onError("Немає дозволу на розпізнавання мовлення")
                    onListeningChanged(false)
                    return@onMain
                }
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    onMain {
                        if (!granted) {
                            onError("Немає дозволу на мікрофон")
                            onListeningChanged(false)
                        } else {
                            begin(languageTag, onPartialResult, onResult, onError, onListeningChanged)
                        }
                    }
                }
            }
        }
    }

    private fun begin(
        languageTag: String,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
    ) {
        val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = languageTag))
        if (recognizer == null || !recognizer.isAvailable()) {
            onError("Розпізнавання для цієї мови недоступне")
            onListeningChanged(false)
            return
        }

        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryRecord, error = null)
        session.setMode(AVAudioSessionModeMeasurement, error = null)
        session.setActive(true, error = null)

        val recognitionRequest = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = true
        }
        request = recognitionRequest

        val inputNode = audioEngine.inputNode
        // Read the format only AFTER the session is active. On a cold first
        // launch the hardware route can still be settling; an invalid format
        // would make installTapOnBus raise an Obj-C exception (uncatchable in
        // Kotlin/Native → hard crash), so bail out gracefully instead.
        val format = inputNode.outputFormatForBus(0u)
        if (format.sampleRate == 0.0 || format.channelCount == 0u) {
            teardown()
            onError("Мікрофон ще не готовий — спробуй ще раз")
            onListeningChanged(false)
            return
        }
        inputNode.installTapOnBus(bus = 0u, bufferSize = 1024u, format = format) { buffer, _ ->
            buffer?.let { request?.appendAudioPCMBuffer(it) }
        }

        audioEngine.prepare()
        if (!audioEngine.startAndReturnError(null)) {
            teardown()
            onError("Не вдалося запустити запис")
            onListeningChanged(false)
            return
        }

        onListeningChanged(true)

        task = recognizer.recognitionTaskWithRequest(recognitionRequest) { result, error ->
            onMain {
                if (result != null) {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        onResult(text)
                    } else {
                        onPartialResult(text)
                    }
                }
                if (error != null || result?.isFinal() == true) {
                    val hadText = result?.bestTranscription?.formattedString?.isNotBlank() == true
                    teardown()
                    onListeningChanged(false)
                    if (error != null && !hadText) {
                        onError("Не вдалося розпізнати")
                    }
                }
            }
        }
    }

    override fun stopListening() {
        // Flush what we have; the final result arrives through the task handler.
        request?.endAudio()
        if (audioEngine.running) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        onMain { listeningChanged?.invoke(false) }
    }

    private fun teardown() {
        if (audioEngine.running) {
            audioEngine.stop()
            audioEngine.inputNode.removeTapOnBus(0u)
        }
        request = null
        task = null
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }
}
