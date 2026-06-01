package com.vocabee.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vocabee.android.data.local.entity.TopicEntity
import com.vocabee.android.data.local.entity.WordEntity

@Database(
    entities = [
        TopicEntity::class,
        WordEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(VocabeeTypeConverters::class)
abstract class VocabeeDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
}
