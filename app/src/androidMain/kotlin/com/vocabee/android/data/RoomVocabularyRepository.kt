package com.vocabee.android.data

import com.vocabee.android.data.local.VocabeeDatabase
import com.vocabee.android.data.local.VocabularyDao
import com.vocabee.android.data.local.entity.TopicEntity
import com.vocabee.android.data.local.entity.WordEntity
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.SyncStatus
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.domain.model.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject

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

    override fun addWord(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
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

    override fun translationOptionsFor(
        topic: DictionaryTopic,
        input: String,
    ): List<TranslationOption> {
        val existingWord = topic.findExistingWord(input) ?: return emptyList()
        return listOf(
            TranslationOption(
                value = existingWord.translation,
                note = TranslationOptionNote.AlreadyAdded(existingWord.source),
                alreadyAdded = true,
            ),
        )
    }

    private fun TopicEntity.toDomain(words: List<WordEntity>): DictionaryTopic {
        return DictionaryTopic(
            id = id,
            userKey = userKey,
            title = title,
            sourceLanguage = languageForCode(sourceLanguageCode),
            targetLanguage = languageForCode(targetLanguageCode),
            coverIndex = coverIndex,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            syncStatus = syncStatus,
            words = words.map { word -> word.toDomain() },
        )
    }

    private fun WordEntity.toDomain(): WordEntry {
        return WordEntry(
            id = id,
            source = source,
            translation = translation,
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

    private fun DictionaryTopic.findExistingWord(query: String): WordEntry? {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return null

        return words.firstOrNull { word ->
            word.source.equals(normalizedQuery, ignoreCase = true) ||
                word.translation.equals(normalizedQuery, ignoreCase = true)
        } ?: words.firstOrNull { word ->
            word.source.lowercase().contains(normalizedQuery) ||
                word.translation.lowercase().contains(normalizedQuery)
        }
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
