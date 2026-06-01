package com.vocabee.android.di

import android.content.Context
import androidx.room.Room
import com.vocabee.android.data.RoomVocabularyRepository
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.data.local.VocabeeDatabase
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideUserSessionManager(): UserSessionManager {
        return StaticUserSessionManager()
    }
}
