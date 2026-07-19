package com.vocabee.android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.MobileAds
import com.vocabee.android.core.presentation.designsystem.AndroidVocabeeColorResolver
import com.vocabee.android.core.presentation.designsystem.AndroidVocabeeStringResolver
import com.vocabee.android.core.presentation.designsystem.LocalVocabeeColors
import com.vocabee.android.core.presentation.designsystem.LocalVocabeeStrings
import com.vocabee.android.feature.vocabulary.presentation.VocabeeApp
import com.vocabee.android.feature.vocabulary.presentation.VocabeeViewModel
import com.vocabee.android.feature.vocabulary.presentation.platform.AndroidGoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.AndroidRewardedAdController
import com.vocabee.android.feature.vocabulary.presentation.platform.AndroidSpeechInputController
import com.vocabee.android.feature.vocabulary.presentation.platform.AndroidShareController
import com.vocabee.android.feature.vocabulary.presentation.platform.AndroidSpeechOutputController

class MainActivity : ComponentActivity() {
    private val viewModel: VocabeeViewModel by viewModel()

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
        Thread {
            MobileAds.initialize(this) {}
        }.start()
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
            val googleAuthController = remember {
                AndroidGoogleAuthController(
                    context = this,
                    webClientId = BuildConfig.VOCABEE_GOOGLE_WEB_CLIENT_ID,
                )
            }
            val shareController = remember { AndroidShareController(this) }
            val rewardedAdController = remember {
                AndroidRewardedAdController(
                    activity = this,
                    adUnitId = BuildConfig.VOCABEE_ADMOB_REWARDED_AD_UNIT_ID,
                )
            }
            val stringResolver = remember { AndroidVocabeeStringResolver(this) }
            val colorResolver = remember { AndroidVocabeeColorResolver(this) }
            val darkThemeEnabled = viewModel.store.state.darkThemeEnabled

            SideEffect {
                val transparent = Color.TRANSPARENT
                val systemBarStyle = if (darkThemeEnabled) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle,
                    navigationBarStyle = systemBarStyle,
                )
            }

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
                    googleAuthController = googleAuthController,
                    rewardedAdController = rewardedAdController,
                    shareController = shareController,
                    remoteLexiconSearch = viewModel.remoteLexiconSearch,
                    api = viewModel.api,
                    preferencesManager = viewModel.preferencesManager,
                    showRawTranslationSearchErrors = BuildConfig.DEBUG,
                    onExitApp = { finish() },
                )
            }
        }
    }
}
