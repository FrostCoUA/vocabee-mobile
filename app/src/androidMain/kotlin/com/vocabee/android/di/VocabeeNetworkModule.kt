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
    /**
     * `10.0.2.2` is the Android emulator's host loopback; swap with your LAN IP for a
     * real device, or with the prod hostname when shipping. Wire to BuildConfig once
     * we have flavors set up.
     */
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000"

    @Provides
    @Singleton
    fun provideApiConfig(): VocabeeApiConfig = VocabeeApiConfig(baseUrl = DEFAULT_BASE_URL)

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
