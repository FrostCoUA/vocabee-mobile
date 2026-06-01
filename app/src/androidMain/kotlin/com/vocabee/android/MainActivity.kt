package com.vocabee.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.vocabee.android.presentation.VocabeeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: VocabeeViewModel by viewModels()

    /**
     * Pre-registered RECORD_AUDIO launcher. Registering here (before
     * onCreate completes via setContent) is what makes it a valid
     * ActivityResultLauncher — registerForActivityResult must be called
     * before STARTED.
     */
    private val recordAudioLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // We just need to ask once on cold start. The speech controller
            // re-checks granted state on each tap, so the result is implicit.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Vocabee)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Ask for the mic permission as soon as the UI is composed, NOT
            // when the user first taps the mic button. Without this, the
            // first tap fires an instant onError("permission") in the speech
            // controller and the user sees the recording cancel on touch.
            LaunchedEffect(Unit) {
                val granted = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

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
                    preferencesManager = viewModel.preferencesManager,
                    onExitApp = { finish() },
                )
            }
        }
    }
}
