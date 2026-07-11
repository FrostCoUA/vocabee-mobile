package com.vocabee.android.di

import androidx.room.Room
import androidx.room.RoomDatabase
import com.vocabee.android.BuildConfig
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApiConfig
import com.vocabee.android.feature.vocabulary.data.api.VocabeeHttpClientFactory
import com.vocabee.android.feature.vocabulary.data.local.VOCABEE_DATABASE_NAME
import com.vocabee.android.feature.vocabulary.data.local.VocabeeDatabase
import com.vocabee.android.feature.vocabulary.data.preferences.AndroidPreferencesManager
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.presentation.VocabeeViewModel
import io.ktor.client.HttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val vocabeeAndroidModule = module {
    single<PreferencesManager> { AndroidPreferencesManager(androidContext()) }

    single {
        // Sourced from `vocabee.api.baseUrl` in `local.properties` (gitignored).
        // Emulator default is `http://10.0.2.2:3000`.
        VocabeeApiConfig(baseUrl = BuildConfig.VOCABEE_API_BASE_URL)
    }

    single<HttpClient> { VocabeeHttpClientFactory.create(debugLogging = BuildConfig.DEBUG) }

    single<RoomDatabase.Builder<VocabeeDatabase>> {
        val context = androidContext()
        Room.databaseBuilder<VocabeeDatabase>(
            context = context,
            name = context.getDatabasePath(VOCABEE_DATABASE_NAME).absolutePath,
        )
    }

    viewModel { VocabeeViewModel(get(), get(), get(), get()) }
}
