package com.vocabee.android.feature.vocabulary.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vocabee.android.feature.vocabulary.data.RoomVocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.data.local.VocabeeDatabase
import com.vocabee.android.feature.vocabulary.data.preferences.AndroidPreferencesManager
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.domain.manager.PreferencesUserSessionManager
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
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
    private val Migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE vocabulary_words
                ADD COLUMN knowledge_percent INTEGER NOT NULL DEFAULT 0
                """.trimIndent(),
            )
        }
    }

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
            .addMigrations(Migration3To4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserSessionManager(
        preferencesManager: PreferencesManager,
    ): UserSessionManager {
        return PreferencesUserSessionManager(preferencesManager)
    }
}
