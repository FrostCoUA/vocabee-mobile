package com.vocabee.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val speechInputController = remember { AndroidSpeechInputController(this) }
            val machineTranslationProvider = remember { AndroidMlKitTranslationProvider(this) }
            val stringResolver = remember { AndroidVocabeeStringResolver(this) }

            DisposableEffect(speechInputController, machineTranslationProvider) {
                onDispose {
                    speechInputController.destroy()
                    machineTranslationProvider.close()
                }
            }

            CompositionLocalProvider(LocalVocabeeStrings provides stringResolver) {
                VocabeeApp(
                    speechInputController = speechInputController,
                    machineTranslationProvider = machineTranslationProvider,
                )
            }
        }
    }
}
