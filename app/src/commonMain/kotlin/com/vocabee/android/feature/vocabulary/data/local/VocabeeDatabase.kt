package com.vocabee.android.feature.vocabulary.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.vocabee.android.feature.vocabulary.data.local.entity.TopicEntity
import com.vocabee.android.feature.vocabulary.data.local.entity.WordEntity

@Database(
    entities = [
        TopicEntity::class,
        WordEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
@TypeConverters(VocabeeTypeConverters::class)
@ConstructedBy(VocabeeDatabaseConstructor::class)
abstract class VocabeeDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
}

// Room KSP generates the per-platform `actual` implementations.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object VocabeeDatabaseConstructor : RoomDatabaseConstructor<VocabeeDatabase> {
    override fun initialize(): VocabeeDatabase
}
