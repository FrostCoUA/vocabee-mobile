package com.vocabee.android.data

import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.SyncStatus
import com.vocabee.android.domain.model.WordDetails
import com.vocabee.android.domain.model.WordEntry

interface VocabularyRepository {
    val supportedLanguages: List<LanguageOption>

    fun topicsForUser(userKey: String): List<DictionaryTopic>

    fun createTopic(
        userKey: String,
        title: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
        coverIndex: Int,
    ): DictionaryTopic

    fun addWord(
        userKey: String,
        topicId: String,
        source: String,
        translation: String,
        ipa: String? = null,
        details: WordDetails? = null,
    ): WordEntry?

    /**
     * Remove a word identified by its translation text (case-insensitive). Returns
     * `true` when something was actually deleted, `false` when the topic had no
     * matching row.
     */
    fun removeWordByTranslation(
        userKey: String,
        topicId: String,
        translation: String,
    ): Boolean
}

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
    ): DictionaryTopic {
        val now = nextTimestamp()
        val topic = DictionaryTopic(
            id = "topic-${++topicCounter}",
            userKey = userKey,
            title = title,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            coverIndex = coverIndex,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            syncStatus = SyncStatus.PendingCreate,
        )
        topicsByUser.getOrPut(userKey) { mutableListOf() }.add(topic)
        return topic
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
        // Reject only EXACT (source, translation) pair duplicates — the same
        // English source can map to several Ukrainian variants and we want
        // them all. (Matches the Room repository's behaviour.)
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

    private fun nextTimestamp(): Long = timestampCounter++
}
