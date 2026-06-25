package com.vocabee.android.feature.vocabulary.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vocabee.android.feature.vocabulary.data.local.entity.TopicEntity
import com.vocabee.android.feature.vocabulary.data.local.entity.WordEntity

@Database(
    entities = [
        TopicEntity::class,
        WordEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(VocabeeTypeConverters::class)
abstract class VocabeeDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
}
