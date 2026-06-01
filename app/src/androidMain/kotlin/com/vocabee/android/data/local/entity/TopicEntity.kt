package com.vocabee.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vocabee.android.domain.model.SyncStatus

@Entity(
    tableName = "vocabulary_topics",
    indices = [
        Index(value = ["user_key"]),
        Index(value = ["user_key", "updated_at_epoch_millis"]),
    ],
)
data class TopicEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "user_key")
    val userKey: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "source_language_code")
    val sourceLanguageCode: String,
    @ColumnInfo(name = "target_language_code")
    val targetLanguageCode: String,
    @ColumnInfo(name = "cover_index")
    val coverIndex: Int,
    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus,
)
