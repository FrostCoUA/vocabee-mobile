package com.vocabee.android.feature.vocabulary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vocabee.android.feature.vocabulary.data.local.entity.TopicEntity
import com.vocabee.android.feature.vocabulary.data.local.entity.WordEntity
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus

@Dao
interface VocabularyDao {
    @Query(
        """
        SELECT *
        FROM vocabulary_topics
        WHERE user_key = :userKey
            AND sync_status != 'PendingDelete'
        ORDER BY created_at_epoch_millis ASC
        """,
    )
    suspend fun topicsForUser(userKey: String): List<TopicEntity>

    @Query(
        """
        SELECT *
        FROM vocabulary_topics
        WHERE user_key = :userKey
        ORDER BY created_at_epoch_millis ASC
        """,
    )
    suspend fun topicsForUserIncludingDeleted(userKey: String): List<TopicEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM vocabulary_topics
        WHERE user_key = :userKey
            AND sync_status != 'PendingDelete'
        """,
    )
    suspend fun topicCountForUser(userKey: String): Int

    @Query(
        """
        SELECT id
        FROM vocabulary_topics
        WHERE user_key = :userKey
            AND source_language_code = 'uk'
            AND target_language_code = 'en'
        """,
    )
    suspend fun legacyUkrainianToEnglishTopicIds(userKey: String): List<String>

    @Query(
        """
        SELECT *
        FROM vocabulary_words
        WHERE user_key = :userKey AND topic_id IN (:topicIds)
            AND sync_status != 'PendingDelete'
        ORDER BY added_at_epoch_millis DESC
        """,
    )
    suspend fun wordsForTopics(
        userKey: String,
        topicIds: List<String>,
    ): List<WordEntity>

    @Query(
        """
        SELECT *
        FROM vocabulary_words
        WHERE user_key = :userKey AND topic_id IN (:topicIds)
        ORDER BY added_at_epoch_millis DESC
        """,
    )
    suspend fun wordsForTopicsIncludingDeleted(
        userKey: String,
        topicIds: List<String>,
    ): List<WordEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM vocabulary_words
        WHERE user_key = :userKey
            AND sync_status != 'PendingDelete'
        """,
    )
    suspend fun wordCountForUser(userKey: String): Int

    @Query(
        """
        SELECT *
        FROM vocabulary_topics
        WHERE user_key = :userKey AND id = :topicId
        LIMIT 1
        """,
    )
    suspend fun topicById(
        userKey: String,
        topicId: String,
    ): TopicEntity?

    /**
     * Reject only EXACT (source, translation) pair duplicates. The same
     * English source can map to multiple Ukrainian translations (different
     * senses of "play": "грати", "гра", "вистава" …) — we want all of them.
     * Previously this also blocked source-vs-source matches, which silently
     * dropped every variant after the first.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM vocabulary_words
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND LOWER(source) = LOWER(:source)
            AND LOWER(translation) = LOWER(:translation)
        """,
    )
    suspend fun duplicateWordCount(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
    ): Int

    @Query(
        """
        SELECT *
        FROM vocabulary_words
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND id = :wordId
            AND sync_status != 'PendingDelete'
        LIMIT 1
        """,
    )
    suspend fun wordById(
        userKey: String,
        topicId: String,
        wordId: String,
    ): WordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTopics(topics: List<TopicEntity>)

    @Query(
        """
        DELETE FROM vocabulary_topics
        WHERE user_key = :userKey AND id = :topicId
        """,
    )
    suspend fun deleteTopic(
        userKey: String,
        topicId: String,
    ): Int

    @Query(
        """
        UPDATE vocabulary_topics
        SET sync_status = :syncStatus,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE user_key = :userKey
            AND id = :topicId
            AND sync_status != 'PendingDelete'
        """,
    )
    suspend fun markTopicDeleted(
        userKey: String,
        topicId: String,
        updatedAtEpochMillis: Long,
        syncStatus: SyncStatus,
    ): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWord(word: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWords(words: List<WordEntity>)

    @Query(
        """
        UPDATE vocabulary_topics
        SET updated_at_epoch_millis = :updatedAtEpochMillis,
            sync_status = :syncStatus
        WHERE user_key = :userKey AND id = :topicId
        """,
    )
    suspend fun updateTopicAfterWordInsert(
        userKey: String,
        topicId: String,
        updatedAtEpochMillis: Long,
        syncStatus: SyncStatus,
    )

    /**
     * Hard-delete by translation. We use translation rather than id because the
     * caller (Add Word overlay) keys off `option.value` (= the translation text)
     * and doesn't otherwise hold a word id. Local-only delete for now; once
     * topic-word sync is wired, switch to a soft delete with
     * `sync_status = PendingDelete` so the server picks it up.
     */
    @Query(
        """
        DELETE FROM vocabulary_words
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND LOWER(translation) = LOWER(:translation)
        """,
    )
    suspend fun deleteWordByTranslation(
        userKey: String,
        topicId: String,
        translation: String,
    ): Int

    @Query(
        """
        UPDATE vocabulary_words
        SET sync_status = :syncStatus,
            updated_at_epoch_millis = :updatedAtEpochMillis
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND LOWER(translation) = LOWER(:translation)
            AND sync_status != 'PendingDelete'
        """,
    )
    suspend fun markWordDeletedByTranslation(
        userKey: String,
        topicId: String,
        translation: String,
        updatedAtEpochMillis: Long,
        syncStatus: SyncStatus,
    ): Int

    @Query(
        """
        UPDATE vocabulary_words
        SET knowledge_percent = :knowledgePercent,
            updated_at_epoch_millis = :updatedAtEpochMillis,
            sync_status = :syncStatus
        WHERE user_key = :userKey
            AND topic_id = :topicId
            AND id = :wordId
            AND sync_status != 'PendingDelete'
        """,
    )
    suspend fun updateWordKnowledgePercent(
        userKey: String,
        topicId: String,
        wordId: String,
        knowledgePercent: Int,
        updatedAtEpochMillis: Long,
        syncStatus: SyncStatus,
    ): Int

    @Query(
        """
        UPDATE vocabulary_topics
        SET sync_status = 'Synced'
        WHERE user_key = :userKey
        """,
    )
    suspend fun markTopicsSynced(userKey: String)

    @Query(
        """
        UPDATE vocabulary_words
        SET sync_status = 'Synced'
        WHERE user_key = :userKey
        """,
    )
    suspend fun markWordsSynced(userKey: String)

    @Query(
        """
        DELETE FROM vocabulary_words
        WHERE user_key = :userKey
            AND sync_status = 'PendingDelete'
        """,
    )
    suspend fun purgePendingDeletedWords(userKey: String)

    @Query(
        """
        DELETE FROM vocabulary_topics
        WHERE user_key = :userKey
            AND sync_status = 'PendingDelete'
        """,
    )
    suspend fun purgePendingDeletedTopics(userKey: String)

    @Query(
        """
        DELETE FROM vocabulary_topics
        WHERE user_key = :userKey
        """,
    )
    suspend fun deleteTopicsForUser(userKey: String)

    @Query(
        """
        DELETE FROM vocabulary_words
        WHERE user_key = :userKey
        """,
    )
    suspend fun deleteWordsForUser(userKey: String)

    @Query(
        """
        UPDATE vocabulary_topics
        SET user_key = :toUserKey
        WHERE user_key = :fromUserKey
        """,
    )
    suspend fun moveTopicsToUser(
        fromUserKey: String,
        toUserKey: String,
    ): Int

    @Query(
        """
        UPDATE vocabulary_words
        SET user_key = :toUserKey
        WHERE user_key = :fromUserKey
        """,
    )
    suspend fun moveWordsToUser(
        fromUserKey: String,
        toUserKey: String,
    ): Int

    @Query(
        """
        UPDATE vocabulary_topics
        SET source_language_code = 'en',
            target_language_code = 'uk'
        WHERE user_key = :userKey AND id IN (:topicIds)
        """,
    )
    suspend fun migrateLegacyTopicLanguageDirection(
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
    suspend fun migrateLegacyWordLanguageDirection(
        userKey: String,
        topicIds: List<String>,
    )
}
