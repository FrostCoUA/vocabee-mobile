package com.vocabee.android

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.vocabee.android.di.vocabeeCommonModule
import com.vocabee.android.di.vocabeeIosModule
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoteLexiconSearchUseCase
import com.vocabee.android.feature.vocabulary.presentation.VocabeeApp
import com.vocabee.android.feature.vocabulary.presentation.VocabeeStore
import com.vocabee.android.feature.vocabulary.presentation.platform.GoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.IosSpeechOutputController
import org.koin.core.Koin
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

/** Started once per process; matches VocabeeApplication.onCreate on Android. */
private val koin: Koin by lazy {
    startKoin {
        modules(vocabeeCommonModule, vocabeeIosModule)
    }.koin
}

/**
 * iOS entry point: shared Compose UI + Room persistence + Koin, wired the same
 * way as Android. Google auth / rewarded ads / speech-to-text keep their
 * common No*-fallbacks until the native SDK integrations land.
 */
@Suppress("unused", "FunctionName") // called from Swift
fun MainViewController(): UIViewController = ComposeUIViewController {
    val preferencesManager = remember { koin.get<PreferencesManager>() }
    val store = remember {
        VocabeeStore(
            repository = koin.get<VocabularyRepository>(),
            userSessionManager = koin.get<UserSessionManager>(),
            preferencesManager = preferencesManager,
        )
    }

    VocabeeApp(
        store = store,
        speechOutputController = remember { IosSpeechOutputController() },
        googleAuthController = remember { koin.get<GoogleAuthController>() },
        remoteLexiconSearch = remember { koin.get<RemoteLexiconSearchUseCase>() },
        api = remember { koin.get<VocabeeApi>() },
        preferencesManager = preferencesManager,
    )
}
