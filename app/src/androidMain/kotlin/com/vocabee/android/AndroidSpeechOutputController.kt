package com.vocabee.android

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.vocabee.android.platform.SpeechOutputController
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wraps Android's [TextToSpeech] under our [SpeechOutputController] contract.
 *
 * Initialisation is async — we queue up the first `speak()` request and play it
 * once the engine reports ready. Subsequent calls cancel the current utterance
 * and start the new one (avoids cascading audio when the user double-taps).
 */
class AndroidSpeechOutputController(context: Context) : SpeechOutputController {

    private val initialized = AtomicBoolean(false)
    private var pendingUtterance: Utterance? = null

    private val engine: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            initialized.set(true)
            val queued = pendingUtterance
            pendingUtterance = null
            if (queued != null) {
                doSpeak(queued.text, queued.languageTag)
            }
        } else {
            Log.w(TAG, "TextToSpeech init failed with status=$status")
        }
    }

    init {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = Unit
            @Deprecated("kept for API compatibility")
            override fun onError(utteranceId: String?) = Unit
            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.w(TAG, "TTS error utteranceId=$utteranceId errorCode=$errorCode")
            }
        })
    }

    override val isSupported: Boolean
        get() = true

    override fun speak(text: String, languageTag: String) {
        if (text.isBlank()) return
        if (!initialized.get()) {
            // Engine still warming up — remember the request and play once ready.
            pendingUtterance = Utterance(text = text, languageTag = languageTag)
            return
        }
        doSpeak(text, languageTag)
    }

    private fun doSpeak(text: String, languageTag: String) {
        val locale = Locale.forLanguageTag(languageTag)
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: $languageTag (result=$result)")
            return
        }
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vocabee-${text.hashCode()}")
    }

    override fun shutdown() {
        try {
            engine.stop()
            engine.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "TTS shutdown threw", e)
        }
    }

    private data class Utterance(val text: String, val languageTag: String)

    companion object {
        private const val TAG = "VocabeeTts"
    }
}
