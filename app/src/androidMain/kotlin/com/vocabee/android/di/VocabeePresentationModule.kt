package com.vocabee.android.di

import android.content.Context
import com.vocabee.android.AndroidMlKitTranslationProvider
import com.vocabee.android.platform.MachineTranslationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object VocabeePresentationModule {
    @Provides
    @ViewModelScoped
    fun provideMachineTranslationProvider(
        @ApplicationContext context: Context,
    ): MachineTranslationProvider {
        return AndroidMlKitTranslationProvider(context)
    }
}
