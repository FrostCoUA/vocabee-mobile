package com.vocabee.android.di

import android.content.Context
import androidx.room.Room
import com.vocabee.android.data.RoomVocabularyRepository
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.data.local.VocabeeDatabase
import com.vocabee.android.data.preferences.AndroidPreferencesManager
import com.vocabee.android.data.preferences.PreferencesManager
import com.vocabee.android.domain.manager.StaticUserSessionManager
import com.vocabee.android.domain.manager.UserSessionManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VocabeeRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindVocabularyRepository(
        repository: RoomVocabularyRepository,
    ): VocabularyRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesManager(
        manager: AndroidPreferencesManager,
    ): PreferencesManager
}

@Module
@InstallIn(SingletonComponent::class)
object VocabeePersistenceModule {
    @Provides
    @Singleton
    fun provideVocabeeDatabase(
        @ApplicationContext context: Context,
    ): VocabeeDatabase {
        return Room.databaseBuilder(
            context,
            VocabeeDatabase::class.java,
            "vocabee.db",
        )
            // Dev-mode: schema bumps wipe local data. We don't write proper Room
            // migrations yet because the server is the source of truth once auth
            // is wired in — a destructive bump here just makes the local cache
            // rebuild on next launch.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserSessionManager(): UserSessionManager {
        return StaticUserSessionManager()
    }
}
