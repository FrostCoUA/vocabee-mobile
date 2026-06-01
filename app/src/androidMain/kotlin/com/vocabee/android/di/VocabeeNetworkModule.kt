package com.vocabee.android.di

import com.vocabee.android.BuildConfig
import com.vocabee.android.data.api.AuthTokenStore
import com.vocabee.android.data.api.KtorVocabeeApi
import com.vocabee.android.data.api.VocabeeApi
import com.vocabee.android.data.api.VocabeeApiConfig
import com.vocabee.android.data.api.VocabeeHttpClientFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VocabeeNetworkModule {
    @Provides
    @Singleton
    fun provideApiConfig(): VocabeeApiConfig =
        // Sourced from `vocabee.api.baseUrl` in `local.properties` (gitignored) via the
        // BuildConfig field injected from app/build.gradle.kts. Emulator default is
        // `http://10.0.2.2:3000`; set it to your machine's LAN IP (e.g.
        // `http://192.168.x.y:3000`) when running on a real device on the same Wi-Fi.
        VocabeeApiConfig(baseUrl = BuildConfig.VOCABEE_API_BASE_URL)

    @Provides
    @Singleton
    fun provideAuthTokenStore(): AuthTokenStore = AuthTokenStore()

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient =
        VocabeeHttpClientFactory.create(debugLogging = BuildConfig.DEBUG)

    @Provides
    @Singleton
    fun provideVocabeeApi(
        client: HttpClient,
        config: VocabeeApiConfig,
        tokenStore: AuthTokenStore,
    ): VocabeeApi = KtorVocabeeApi(client, config, tokenStore)
}
