package com.vocabee.android.feature.vocabulary.data

import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry

class FakeVocabularyRepository : VocabularyRepository {
    override val supportedLanguages = listOf(
        LanguageOption("uk", "Ukrainian", "UA", "uk-UA"),
        LanguageOption("en", "English", "EN", "en-US"),
        LanguageOption("ru", "Russian", "RU", "ru-RU"),
        LanguageOption("pl", "Polish", "PL", "pl-PL"),
        LanguageOption("de", "German", "DE", "de-DE"),
        LanguageOption("es", "Spanish", "ES", "es-ES"),
    )

    private val topicsByUser = mutableMapOf<String, MutableList<DictionaryTopic>>()
    private var topicCounter = 0
    private var wordCounter = 0
    private var timestampCounter = 1_000L

    override fun topicsForUser(userKey: String): List<DictionaryTopic> {
        return topicsByUser[userKey].orEmpty()
    }

    override fun createTopic(
        userKey: String,
        title: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
        coverIndex: Int,
        iconIndex: Int,
    ): DictionaryTopic {
        val now = nextTimestamp()
        val topic = DictionaryTopic(
            id = "topic-${++topicCounter}",
            userKey = userKey,
            title = title,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            coverIndex = coverIndex,
            iconIndex = iconIndex,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncStatus = SyncStatus.PendingCreate,
        )
        topicsByUser.getOrPut(userKey) { mutableListOf() }.add(topic)
        return topic
    }

    override fun removeTopic(
        userKey: String,
        topicId: String,
    ): Boolean {
        val topics = topicsByUser[userKey] ?: return false
        return topics.removeAll { topic -> topic.id == topicId }
    }

    override fun addWord(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
        ipa: String?,
        details: WordDetails?,
    ): WordEntry? {
        val topics = topicsByUser[userKey] ?: return null
        val topicIndex = topics.indexOfFirst { topic -> topic.id == topicId }
        if (topicIndex == -1) return null

        val topic = topics[topicIndex]
        val exists = topic.words.any { word ->
            word.source.equals(source, ignoreCase = true) &&
                word.translation.equals(translation, ignoreCase = true)
        }
        if (exists) return null

        val now = nextTimestamp()
        val word = WordEntry(
            id = "${topic.id}-word-${++wordCounter}",
            source = source,
            translation = translation,
            ipa = ipa,
            details = details,
            addedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncStatus = SyncStatus.PendingCreate,
        )
        topics[topicIndex] = topic.copy(
            words = listOf(word) + topic.words,
            updatedAtEpochMillis = now,
            syncStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            },
        )
        return word
    }

    override fun removeWordByTranslation(
        userKey: String,
        topicId: String,
        translation: String,
    ): Boolean {
        val topics = topicsByUser[userKey] ?: return false
        val topicIndex = topics.indexOfFirst { it.id == topicId }
        if (topicIndex == -1) return false
        val topic = topics[topicIndex]
        val target = translation.trim().lowercase()
        val remaining = topic.words.filterNot { it.translation.trim().lowercase() == target }
        if (remaining.size == topic.words.size) return false
        val now = nextTimestamp()
        topics[topicIndex] = topic.copy(
            words = remaining,
            updatedAtEpochMillis = now,
            syncStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            },
        )
        return true
    }

    override fun adjustWordKnowledgePercent(
        userKey: String,
        topicId: String,
        wordId: String,
        deltaPercent: Int,
    ): WordEntry? {
        val topics = topicsByUser[userKey] ?: return null
        val topicIndex = topics.indexOfFirst { it.id == topicId }
        if (topicIndex == -1) return null

        val topic = topics[topicIndex]
        val wordIndex = topic.words.indexOfFirst { it.id == wordId }
        if (wordIndex == -1) return null

        val now = nextTimestamp()
        val current = topic.words[wordIndex]
        val updatedWord = current.copy(
            knowledgePercent = (current.knowledgePercent + deltaPercent).coerceIn(0, 100),
            updatedAtEpochMillis = now,
            syncStatus = if (current.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            },
        )
        topics[topicIndex] = topic.copy(
            words = topic.words.toMutableList().also { words -> words[wordIndex] = updatedWord },
            updatedAtEpochMillis = now,
            syncStatus = if (topic.syncStatus == SyncStatus.PendingCreate) {
                SyncStatus.PendingCreate
            } else {
                SyncStatus.PendingUpdate
            },
        )
        return updatedWord
    }

    override fun hasVocabulary(userKey: String): Boolean {
        return topicsByUser[userKey].orEmpty().isNotEmpty()
    }

    override fun exportSyncSnapshot(
        userKey: String,
        includeDeleted: Boolean,
    ): VocabularySyncSnapshot {
        return VocabularySyncSnapshot(topics = topicsByUser[userKey].orEmpty())
    }

    override fun replaceSyncSnapshot(
        userKey: String,
        snapshot: VocabularySyncSnapshot,
    ) {
        topicsByUser[userKey] = snapshot.topics.map { topic ->
            topic.copy(userKey = userKey, syncStatus = SyncStatus.Synced)
        }.toMutableList()
    }

    override fun markSynced(userKey: String) {
        topicsByUser[userKey] = topicsByUser[userKey].orEmpty().map { topic ->
            topic.copy(
                syncStatus = SyncStatus.Synced,
                words = topic.words.map { word -> word.copy(syncStatus = SyncStatus.Synced) },
            )
        }.toMutableList()
    }

    override fun moveUserVocabulary(
        fromUserKey: String,
        toUserKey: String,
    ) {
        val moving = topicsByUser.remove(fromUserKey).orEmpty()
        if (moving.isEmpty()) return
        val target = topicsByUser.getOrPut(toUserKey) { mutableListOf() }
        target += moving.map { topic -> topic.copy(userKey = toUserKey) }
    }

    private fun nextTimestamp(): Long = timestampCounter++
}
