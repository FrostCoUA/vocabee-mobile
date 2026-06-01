package com.vocabee.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vocabee.android.data.local.entity.TopicEntity
import com.vocabee.android.data.local.entity.WordEntity
import com.vocabee.android.domain.model.SyncStatus

@Dao
interface VocabularyDao {
    @Query(
        """
        SELECT *
        FROM vocabulary_topics
        WHERE user_key = :userKey
        ORDER BY created_at_epoch_millis ASC
        """,
    )
    fun topicsForUser(userKey: String): List<TopicEntity>

    @Query(
        """
        SELECT id
        FROM vocabulary_topics
        WHERE user_key = :userKey
            AND source_language_code = 'uk'
            AND target_language_code = 'en'
        """,
    )
    fun legacyUkrainianToEnglishTopicIds(userKey: String): List<String>

    @Query(
        """
        SELECT *
        FROM vocabulary_words
        WHERE user_key = :userKey AND topic_id IN (:topicIds)
        ORDER BY added_at_epoch_millis DESC
        """,
    )
    fun wordsForTopics(
        userKey: String,
        topicIds: List<String>,
    ): List<WordEntity>

    @Query(
        """
        SELECT *
        FROM vocabulary_topics
        WHERE user_key = :userKey AND id = :topicId
        LIMIT 1
        """,
    )
    fun topicById(
        userKey: String,
        topicId: String,
    ): TopicEntity?

    @Query(
        """
        SELECT COUNT(*)
        FROM vocabulary_words
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND (
                LOWER(source) = LOWER(:source)
                OR LOWER(translation) = LOWER(:source)
                OR LOWER(source) = LOWER(:translation)
                OR LOWER(translation) = LOWER(:translation)
            )
        """,
    )
    fun duplicateWordCount(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertWord(word: WordEntity)

    @Query(
        """
        UPDATE vocabulary_topics
        SET updated_at_epoch_millis = :updatedAtEpochMillis,
            sync_status = :syncStatus
        WHERE user_key = :userKey AND id = :topicId
        """,
    )
    fun updateTopicAfterWordInsert(
        userKey: String,
        topicId: String,
        updatedAtEpochMillis: Long,
        syncStatus: SyncStatus,
    )

    @Query(
        """
        UPDATE vocabulary_topics
        SET source_language_code = 'en',
            target_language_code = 'uk'
        WHERE user_key = :userKey AND id IN (:topicIds)
        """,
    )
    fun migrateLegacyTopicLanguageDirection(
        userKey: String,
        topicIds: List<String>,
    )

    @Query(
        """
        UPDATE vocabulary_words
        SET source = translation,
            translation = source
        WHERE user_key = :userKey AND topic_id IN (:topicIds)
        """,
    )
    fun migrateLegacyWordLanguageDirection(
        userKey: String,
        topicIds: List<String>,
    )
}
