package com.vocabee.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.vocabee.android.presentation.VocabeeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: VocabeeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Vocabee)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val speechInputController = remember { AndroidSpeechInputController(this) }
            val speechOutputController = remember { AndroidSpeechOutputController(this) }
            val stringResolver = remember { AndroidVocabeeStringResolver(this) }
            val colorResolver = remember { AndroidVocabeeColorResolver(this) }

            DisposableEffect(speechInputController) {
                onDispose {
                    speechInputController.destroy()
                }
            }
            DisposableEffect(speechOutputController) {
                onDispose {
                    speechOutputController.shutdown()
                }
            }

            CompositionLocalProvider(
                LocalVocabeeStrings provides stringResolver,
                LocalVocabeeColors provides colorResolver,
            ) {
                VocabeeApp(
                    store = viewModel.store,
                    speechInputController = speechInputController,
                    speechOutputController = speechOutputController,
                    remoteLexiconSearch = viewModel.remoteLexiconSearch,
                )
            }
        }
    }
}
