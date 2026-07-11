package com.vocabee.android.feature.vocabulary.data.local

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

const val VOCABEE_DATABASE_NAME = "vocabee.db"

private val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            ALTER TABLE vocabulary_words
            ADD COLUMN knowledge_percent INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
    }
}

private val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            ALTER TABLE vocabulary_topics
            ADD COLUMN icon_index INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
    }
}

/**
 * Applies the shared Room configuration to a platform-provided builder
 * (Android supplies a Context-based one, iOS a file-path one).
 */
fun buildVocabeeDatabase(builder: RoomDatabase.Builder<VocabeeDatabase>): VocabeeDatabase {
    return builder
        .addMigrations(Migration3To4, Migration4To5)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
