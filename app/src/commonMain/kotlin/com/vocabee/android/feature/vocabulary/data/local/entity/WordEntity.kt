package com.vocabee.android.feature.vocabulary.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus

@Entity(
    tableName = "vocabulary_words",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topic_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["topic_id"]),
        Index(value = ["user_key"]),
        Index(value = ["user_key", "topic_id"]),
        Index(value = ["user_key", "added_at_epoch_millis"]),
    ],
)
data class WordEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "user_key")
    val userKey: String,
    @ColumnInfo(name = "topic_id")
    val topicId: String,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "ipa")
    val ipa: String? = null,
    /**
     * Serialized [com.vocabee.android.feature.vocabulary.domain.model.WordDetails] (senses, synonyms,
     * antonyms, forms). Stored as a single JSON column to avoid spawning child
     * tables for read-only enrichment that the server can re-supply anytime.
     */
    @ColumnInfo(name = "details_json")
    val detailsJson: String? = null,
    @ColumnInfo(name = "knowledge_percent")
    val knowledgePercent: Int = 0,
    @ColumnInfo(name = "added_at_epoch_millis")
    val addedAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus,
)
