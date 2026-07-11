package com.vocabee.android.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.vocabee.android.feature.vocabulary.data.api.IosVocabeeHttpClientFactory
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApiConfig
import com.vocabee.android.feature.vocabulary.data.local.VOCABEE_DATABASE_NAME
import com.vocabee.android.feature.vocabulary.data.local.VocabeeDatabase
import com.vocabee.android.feature.vocabulary.data.preferences.IosPreferencesManager
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.presentation.platform.GoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.IosGoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.IosSpeechInputController
import com.vocabee.android.feature.vocabulary.presentation.platform.SpeechInputController
import io.ktor.client.HttpClient
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

val vocabeeIosModule = module {
    single<PreferencesManager> { IosPreferencesManager() }

    single { VocabeeApiConfig(baseUrl = vocabeeIosBaseUrl) }

    single<HttpClient> { IosVocabeeHttpClientFactory.create() }

    single<SpeechInputController> { IosSpeechInputController() }

    single<GoogleAuthController> {
        IosGoogleAuthController(
            client = get(),
            // iOS OAuth client (Google Cloud Console → Credentials). Client ids
            // are public identifiers, not secrets.
            clientId = "517295849460-qaupsajeta3n7r72goevecd43bh4kike.apps.googleusercontent.com",
        )
    }

    single<RoomDatabase.Builder<VocabeeDatabase>> {
        Room.databaseBuilder<VocabeeDatabase>(name = "${documentDirectory()}/$VOCABEE_DATABASE_NAME")
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "Не вдалося отримати шлях до Documents" }
}
