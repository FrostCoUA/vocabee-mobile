package com.vocabee.android.feature.vocabulary.presentation.platform

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vocabee.android.R
import com.vocabee.android.feature.vocabulary.presentation.platform.SpeechInputController

class AndroidSpeechInputController(
    private val activity: ComponentActivity,
) : SpeechInputController {
    override val isSupported: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(activity)

    private var speechRecognizer: SpeechRecognizer? = null
    private var activeSessionId = 0
    private var stopRequested = false

    override fun startListening(
        languageTag: String,
        alternativeLanguageTags: List<String>,
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
            // Fire the system request and report the interruption reason back to
            // the UI so the user gets the same snackbar feedback as other speech
            // recognizer interruptions.
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
        val sessionId = ++activeSessionId
        stopRequested = false
        val recognizer = SpeechRecognizer.createSpeechRecognizer(activity)
        speechRecognizer = recognizer

        fun isCurrentSession(): Boolean {
            return sessionId == activeSessionId && speechRecognizer === recognizer
        }

        fun finishSession() {
            if (!isCurrentSession()) return
            onListeningChanged(false)
            recognizer.destroy()
            speechRecognizer = null
        }

        recognizer.apply {
            setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (!isCurrentSession()) return
                        onListeningChanged(true)
                    }

                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        if (!isCurrentSession()) return
                        onListeningChanged(false)
                    }

                    override fun onError(error: Int) {
                        if (!isCurrentSession()) return
                        val shouldIgnoreStopError = stopRequested &&
                            (error == SpeechRecognizer.ERROR_CLIENT ||
                                error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
                        finishSession()
                        if (shouldIgnoreStopError) return
                        onError(error.toSpeechMessage())
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isCurrentSession()) return
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val bestMatch = matches.firstOrNull().orEmpty()
                        finishSession()
                        if (bestMatch.isBlank()) {
                            onError(activity.getString(R.string.voice_error_no_match))
                        } else {
                            onResult(bestMatch)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        if (!isCurrentSession()) return
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

        val recognitionLanguages = (listOf(languageTag) + alternativeLanguageTags)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val primaryLanguage = recognitionLanguages.firstOrNull() ?: languageTag

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLanguage)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLanguage)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && recognitionLanguages.size > 1) {
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
                putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES,
                    ArrayList(recognitionLanguages),
                )
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                    ArrayList(recognitionLanguages),
                )
            }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Android's stock silence-detection is very aggressive (~1.5s) — users
            // type-pause-think and the recognizer fires onEndOfSpeech / onResults
            // before they're done. Push the thresholds out so a slow speaker has
            // room to finish. The recognizer also won't end before MINIMUM_LENGTH.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4_000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                3_000L,
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                3_000L,
            )
        }

        onListeningChanged(true)
        speechRecognizer?.startListening(intent)
    }

    override fun stopListening() {
        stopRequested = true
        speechRecognizer?.stopListening()
    }

    fun destroy() {
        activeSessionId += 1
        stopRequested = false
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
            SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> activity.getString(R.string.voice_error_too_many_requests)
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> activity.getString(R.string.voice_error_network)
            SpeechRecognizer.ERROR_NO_MATCH -> activity.getString(R.string.voice_error_no_match)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> activity.getString(R.string.voice_error_busy)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> activity.getString(R.string.voice_error_timeout)
            SpeechRecognizer.ERROR_SERVER -> activity.getString(R.string.voice_error_server)
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> activity.getString(R.string.voice_error_server_disconnected)
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> activity.getString(R.string.voice_error_language_not_supported)
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> activity.getString(R.string.voice_error_language_unavailable)
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> activity.getString(R.string.voice_error_cannot_check_support)
            SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> {
                activity.getString(R.string.voice_error_cannot_listen_to_download_events)
            }
            else -> activity.getString(R.string.voice_error_unknown_code, this)
        }
    }

    private companion object {
        const val REQUEST_RECORD_AUDIO = 42
    }
}
