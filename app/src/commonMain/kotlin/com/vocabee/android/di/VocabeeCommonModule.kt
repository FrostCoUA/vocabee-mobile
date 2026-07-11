package com.vocabee.android.di

import com.vocabee.android.feature.vocabulary.data.RoomVocabularyRepository
import com.vocabee.android.feature.vocabulary.data.api.AuthTokenStore
import com.vocabee.android.feature.vocabulary.data.api.KtorVocabeeApi
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.local.VocabeeDatabase
import com.vocabee.android.feature.vocabulary.data.local.buildVocabeeDatabase
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.manager.PreferencesUserSessionManager
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoteLexiconSearchUseCase
import org.koin.dsl.module

/**
 * Platform-agnostic wiring. Platform modules must additionally provide:
 * [com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager],
 * `RoomDatabase.Builder<VocabeeDatabase>`, `HttpClient` and
 * [com.vocabee.android.feature.vocabulary.data.api.VocabeeApiConfig].
 */
val vocabeeCommonModule = module {
    single<VocabeeDatabase> { buildVocabeeDatabase(get()) }
    single<VocabularyRepository> { RoomVocabularyRepository(get()) }
    single<UserSessionManager> { PreferencesUserSessionManager(get()) }
    single { AuthTokenStore(get()) }
    single<VocabeeApi> { KtorVocabeeApi(get(), get(), get()) }
    single { RemoteLexiconSearchUseCase(get()) }
}
