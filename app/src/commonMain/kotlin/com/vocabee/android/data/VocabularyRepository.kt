package com.vocabee.android.data

import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.SyncStatus
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote
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
    ): WordEntry?

    fun translationOptionsFor(
        topic: DictionaryTopic,
        input: String,
    ): List<TranslationOption>
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
    ): WordEntry? {
        val topics = topicsByUser[userKey] ?: return null
        val topicIndex = topics.indexOfFirst { topic -> topic.id == topicId }
        if (topicIndex == -1) return null

        val topic = topics[topicIndex]
        val exists = topic.words.any { word ->
            word.source.equals(source, ignoreCase = true) ||
                word.translation.equals(source, ignoreCase = true) ||
                word.source.equals(translation, ignoreCase = true) ||
                word.translation.equals(translation, ignoreCase = true)
        }
        if (exists) return null

        val now = nextTimestamp()
        val word = WordEntry(
            id = "${topic.id}-word-${++wordCounter}",
            source = source,
            translation = translation,
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

    private fun nextTimestamp(): Long = timestampCounter++
}
