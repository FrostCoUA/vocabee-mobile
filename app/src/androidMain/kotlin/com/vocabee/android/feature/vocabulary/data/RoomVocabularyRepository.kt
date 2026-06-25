package com.vocabee.android.feature.vocabulary.data

import com.vocabee.android.feature.vocabulary.data.local.VocabeeDatabase
import com.vocabee.android.feature.vocabulary.data.local.VocabularyDao
import com.vocabee.android.feature.vocabulary.data.local.entity.TopicEntity
import com.vocabee.android.feature.vocabulary.data.local.entity.WordEntity
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus
import com.vocabee.android.feature.vocabulary.domain.model.TopicUpdatedLabel
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

private const val MILLIS_PER_DAY = 86_400_000L

private val detailsCodec = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

class RoomVocabularyRepository @Inject constructor(
    private val database: VocabeeDatabase,
) : VocabularyRepository {
    private val vocabularyDao: VocabularyDao = database.vocabularyDao()

    override val supportedLanguages = listOf(
        LanguageOption("uk", "Ukrainian", "UA", "uk-UA"),
        LanguageOption("en", "English", "EN", "en-US"),
        LanguageOption("ru", "Russian", "RU", "ru-RU"),
        LanguageOption("pl", "Polish", "PL", "pl-PL"),
        LanguageOption("de", "German", "DE", "de-DE"),
        LanguageOption("es", "Spanish", "ES", "es-ES"),
    )

    private val languagesByCode = supportedLanguages.associateBy { language -> language.code }

    override fun topicsForUser(userKey: String): List<DictionaryTopic> = runBlocking(Dispatchers.IO) {
        migrateLegacyLanguageDirection(userKey)
        val topics = vocabularyDao.topicsForUser(userKey)
        if (topics.isEmpty()) return@runBlocking emptyList()

        val wordsByTopicId = vocabularyDao
            .wordsForTopics(
                userKey = userKey,
                topicIds = topics.map { topic -> topic.id },
            )
            .groupBy { word -> word.topicId }

        topics.map { topic ->
            topic.toDomain(wordsByTopicId[topic.id].orEmpty())
        }
    }

    override fun createTopic(
        userKey: String,
        title: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
        coverIndex: Int,
    ): DictionaryTopic = runBlocking(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val topic = TopicEntity(
            id = UUID.randomUUID().toString(),
            userKey = userKey,
            title = title,
            sourceLanguageCode = sourceLanguage.code,
            targetLanguageCode = targetLanguage.code,
            coverIndex = coverIndex,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncStatus = SyncStatus.PendingCreate,
        )

        vocabularyDao.insertTopic(topic)
        topic.toDomain(words = emptyList())
    }

    override fun removeTopic(
        userKey: String,
        topicId: String,
    ): Boolean = runBlocking(Dispatchers.IO) {
        var deleted = false
        database.runInTransaction {
            vocabularyDao.topicById(userKey, topicId) ?: return@runInTransaction
            val affected = if (userKey == DEFAULT_LOCAL_USER_KEY) {
                vocabularyDao.deleteTopic(
                    userKey = userKey,
                    topicId = topicId,
                )
            } else {
                vocabularyDao.markTopicDeleted(
                    userKey = userKey,
                    topicId = topicId,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                    syncStatus = SyncStatus.PendingDelete,
                )
            }
            deleted = affected > 0
        }
        deleted
    }

    override fun addWord(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
        ipa: String?,
        details: WordDetails?,
    ): WordEntry? = runBlocking(Dispatchers.IO) {
        var insertedWord: WordEntity? = null

        database.runInTransaction {
            val topic = vocabularyDao.topicById(userKey, topicId) ?: return@runInTransaction
            val duplicateCount = vocabularyDao.duplicateWordCount(
                userKey = userKey,
                topicId = topicId,
                source = source,
                translation = translation,
            )
            if (duplicateCount > 0) return@runInTransaction

            val now = System.currentTimeMillis()
            val word = WordEntity(
                id = UUID.randomUUID().toString(),
                userKey = userKey,
                topicId = topicId,
                source = source,
                translation = translation,
                ipa = ipa,
                detailsJson = details?.let {
                    detailsCodec.encodeToString(WordDetails.serializer(), it)
                },
                knowledgePercent = 0,
                addedAtEpochMillis = now,
                updatedAtEpochMillis = now,
                syncStatus = SyncStatus.PendingCreate,
            )
            val topicSyncStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            }

            vocabularyDao.insertWord(word)
            vocabularyDao.updateTopicAfterWordInsert(
                userKey = userKey,
                topicId = topicId,
                updatedAtEpochMillis = now,
                syncStatus = topicSyncStatus,
            )
            insertedWord = word
        }

        insertedWord?.toDomain()
    }

    override fun removeWordByTranslation(
        userKey: String,
        topicId: String,
        translation: String,
    ): Boolean = runBlocking(Dispatchers.IO) {
        var deleted = false
        database.runInTransaction {
            val topic = vocabularyDao.topicById(userKey, topicId) ?: return@runInTransaction
            val now = System.currentTimeMillis()
            val affected = if (userKey == DEFAULT_LOCAL_USER_KEY) {
                vocabularyDao.deleteWordByTranslation(
                    userKey = userKey,
                    topicId = topicId,
                    translation = translation,
                )
            } else {
                vocabularyDao.markWordDeletedByTranslation(
                    userKey = userKey,
                    topicId = topicId,
                    translation = translation,
                    updatedAtEpochMillis = now,
                    syncStatus = SyncStatus.PendingDelete,
                )
            }
            if (affected == 0) return@runInTransaction
            val nextStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            }
            vocabularyDao.updateTopicAfterWordInsert(
                userKey = userKey,
                topicId = topicId,
                updatedAtEpochMillis = now,
                syncStatus = nextStatus,
            )
            deleted = true
        }
        deleted
    }

    override fun adjustWordKnowledgePercent(
        userKey: String,
        topicId: String,
        wordId: String,
        deltaPercent: Int,
    ): WordEntry? = runBlocking(Dispatchers.IO) {
        var updatedWord: WordEntity? = null

        database.runInTransaction {
            val topic = vocabularyDao.topicById(userKey, topicId) ?: return@runInTransaction
            val word = vocabularyDao.wordById(
                userKey = userKey,
                topicId = topicId,
                wordId = wordId,
            ) ?: return@runInTransaction
            val now = System.currentTimeMillis()
            val nextWordStatus = if (word.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            }
            val nextTopicStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            }
            val nextKnowledge = (word.knowledgePercent + deltaPercent).coerceIn(0, 100)
            val affected = vocabularyDao.updateWordKnowledgePercent(
                userKey = userKey,
                topicId = topicId,
                wordId = wordId,
                knowledgePercent = nextKnowledge,
                updatedAtEpochMillis = now,
                syncStatus = nextWordStatus,
            )
            if (affected == 0) return@runInTransaction
            vocabularyDao.updateTopicAfterWordInsert(
                userKey = userKey,
                topicId = topicId,
                updatedAtEpochMillis = now,
                syncStatus = nextTopicStatus,
            )
            updatedWord = word.copy(
                knowledgePercent = nextKnowledge,
                updatedAtEpochMillis = now,
                syncStatus = nextWordStatus,
            )
        }

        updatedWord?.toDomain()
    }

    override fun hasVocabulary(userKey: String): Boolean = runBlocking(Dispatchers.IO) {
        vocabularyDao.topicCountForUser(userKey) > 0 || vocabularyDao.wordCountForUser(userKey) > 0
    }

    override fun exportSyncSnapshot(
        userKey: String,
        includeDeleted: Boolean,
    ): VocabularySyncSnapshot = runBlocking(Dispatchers.IO) {
        val topics = if (includeDeleted) {
            vocabularyDao.topicsForUserIncludingDeleted(userKey)
        } else {
            vocabularyDao.topicsForUser(userKey)
        }
        if (topics.isEmpty()) return@runBlocking VocabularySyncSnapshot(emptyList())

        val wordsByTopicId = if (includeDeleted) {
            vocabularyDao.wordsForTopicsIncludingDeleted(
                userKey = userKey,
                topicIds = topics.map { it.id },
            )
        } else {
            vocabularyDao.wordsForTopics(
                userKey = userKey,
                topicIds = topics.map { it.id },
            )
        }.groupBy { it.topicId }

        VocabularySyncSnapshot(
            topics = topics.map { topic -> topic.toDomain(wordsByTopicId[topic.id].orEmpty()) },
        )
    }

    override fun replaceSyncSnapshot(
        userKey: String,
        snapshot: VocabularySyncSnapshot,
    ) = runBlocking(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        database.runInTransaction {
            vocabularyDao.deleteWordsForUser(userKey)
            vocabularyDao.deleteTopicsForUser(userKey)
            val topicEntities = snapshot.topics.map { topic ->
                TopicEntity(
                    id = topic.id,
                    userKey = userKey,
                    title = topic.title,
                    sourceLanguageCode = topic.sourceLanguage.code,
                    targetLanguageCode = topic.targetLanguage.code,
                    coverIndex = topic.coverIndex,
                    createdAtEpochMillis = topic.createdAtEpochMillis.takeIf { it > 0L } ?: now,
                    updatedAtEpochMillis = topic.updatedAtEpochMillis.takeIf { it > 0L } ?: now,
                    syncStatus = SyncStatus.Synced,
                )
            }
            if (topicEntities.isNotEmpty()) {
                vocabularyDao.upsertTopics(topicEntities)
            }
            val wordEntities = snapshot.topics.flatMap { topic ->
                topic.words.map { word ->
                    WordEntity(
                        id = word.id,
                        userKey = userKey,
                        topicId = topic.id,
                        source = word.source,
                        translation = word.translation,
                        ipa = word.ipa,
                        detailsJson = word.details?.let {
                            detailsCodec.encodeToString(WordDetails.serializer(), it)
                        },
                        knowledgePercent = word.knowledgePercent.coerceIn(0, 100),
                        addedAtEpochMillis = word.addedAtEpochMillis.takeIf { it > 0L } ?: now,
                        updatedAtEpochMillis = word.updatedAtEpochMillis.takeIf { it > 0L } ?: now,
                        syncStatus = SyncStatus.Synced,
                    )
                }
            }
            if (wordEntities.isNotEmpty()) {
                vocabularyDao.upsertWords(wordEntities)
            }
        }
    }

    override fun markSynced(userKey: String) = runBlocking(Dispatchers.IO) {
        database.runInTransaction {
            vocabularyDao.purgePendingDeletedWords(userKey)
            vocabularyDao.purgePendingDeletedTopics(userKey)
            vocabularyDao.markWordsSynced(userKey)
            vocabularyDao.markTopicsSynced(userKey)
        }
    }

    override fun moveUserVocabulary(
        fromUserKey: String,
        toUserKey: String,
    ) = runBlocking(Dispatchers.IO) {
        if (fromUserKey == toUserKey) return@runBlocking
        database.runInTransaction {
            vocabularyDao.moveTopicsToUser(fromUserKey, toUserKey)
            vocabularyDao.moveWordsToUser(fromUserKey, toUserKey)
        }
    }

    private fun TopicEntity.toDomain(words: List<WordEntity>): DictionaryTopic {
        return DictionaryTopic(
            id = id,
            userKey = userKey,
            title = title,
            sourceLanguage = languageForCode(sourceLanguageCode),
            targetLanguage = languageForCode(targetLanguageCode),
            updatedLabel = updatedLabelFor(updatedAtEpochMillis),
            coverIndex = coverIndex,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            syncStatus = syncStatus,
            words = words.map { word -> word.toDomain() },
        )
    }

    private fun WordEntity.toDomain(): WordEntry {
        val details = detailsJson?.let { rawJson ->
            runCatching { detailsCodec.decodeFromString(WordDetails.serializer(), rawJson) }
                .getOrNull()
        }
        return WordEntry(
            id = id,
            source = source,
            translation = translation,
            ipa = ipa,
            details = details,
            knowledgePercent = knowledgePercent.coerceIn(0, 100),
            addedAtEpochMillis = addedAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            syncStatus = syncStatus,
        )
    }

    private fun languageForCode(code: String): LanguageOption {
        return languagesByCode[code] ?: LanguageOption(
            code = code,
            name = code.uppercase(),
            shortName = code.uppercase(),
            speechTag = code,
        )
    }

    private fun updatedLabelFor(updatedAtEpochMillis: Long): TopicUpdatedLabel {
        val daysAgo = ((startOfTodayMillis() - startOfDayMillis(updatedAtEpochMillis)) / MILLIS_PER_DAY)
            .coerceAtLeast(0L)
        return when {
            daysAgo == 0L -> TopicUpdatedLabel.Today
            daysAgo == 1L -> TopicUpdatedLabel.Yesterday
            daysAgo < 7L -> TopicUpdatedLabel.DaysAgo(daysAgo.toInt())
            else -> TopicUpdatedLabel.WeeksAgo((daysAgo / 7L).coerceAtLeast(1L).toInt())
        }
    }

    private fun startOfTodayMillis(): Long = startOfDayMillis(System.currentTimeMillis())

    private fun startOfDayMillis(epochMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun migrateLegacyLanguageDirection(userKey: String) {
        val legacyTopicIds = vocabularyDao.legacyUkrainianToEnglishTopicIds(userKey)
        if (legacyTopicIds.isEmpty()) return

        database.runInTransaction {
            vocabularyDao.migrateLegacyWordLanguageDirection(
                userKey = userKey,
                topicIds = legacyTopicIds,
            )
            vocabularyDao.migrateLegacyTopicLanguageDirection(
                userKey = userKey,
                topicIds = legacyTopicIds,
            )
        }
    }
}
