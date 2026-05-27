package com.vocabee.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vocabee.android.platform.SpeechInputController

class AndroidSpeechInputController(
    private val activity: ComponentActivity,
) : SpeechInputController {
    override val isSupported: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(activity)

    private var speechRecognizer: SpeechRecognizer? = null

    override fun startListening(
        languageTag: String,
        onPartialResult: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
    ) {
        if (!isSupported) {
            onListeningChanged(false)
            onError(activity.getString(R.string.voice_error_unsupported_device))
            return
        }

        if (!hasRecordAudioPermission()) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO,
            )
            onListeningChanged(false)
            onError(activity.getString(R.string.voice_error_microphone_permission))
            return
        }

        destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
            setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningChanged(true)
                    }

                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        onListeningChanged(false)
                    }

                    override fun onError(error: Int) {
                        onListeningChanged(false)
                        onError(error.toSpeechMessage())
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningChanged(false)
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val bestMatch = matches.firstOrNull().orEmpty()
                        if (bestMatch.isBlank()) {
                            onError(activity.getString(R.string.voice_error_no_match))
                        } else {
                            onResult(bestMatch)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                            .firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                }
            )
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        onListeningChanged(true)
        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Int.toSpeechMessage(): String {
        return when (this) {
            SpeechRecognizer.ERROR_AUDIO -> activity.getString(R.string.voice_error_audio)
            SpeechRecognizer.ERROR_CLIENT -> activity.getString(R.string.voice_error_client)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> activity.getString(R.string.voice_error_no_permission)
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> activity.getString(R.string.voice_error_network)
            SpeechRecognizer.ERROR_NO_MATCH -> activity.getString(R.string.voice_error_no_match)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> activity.getString(R.string.voice_error_busy)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> activity.getString(R.string.voice_error_timeout)
            SpeechRecognizer.ERROR_SERVER -> activity.getString(R.string.voice_error_server)
            else -> activity.getString(R.string.voice_error_generic)
        }
    }

    private companion object {
        const val REQUEST_RECORD_AUDIO = 42
    }
}
