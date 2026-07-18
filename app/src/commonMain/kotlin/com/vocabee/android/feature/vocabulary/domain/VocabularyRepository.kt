package com.vocabee.android.feature.vocabulary.domain

import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry

interface VocabularyRepository {
    val supportedLanguages: List<LanguageOption>

    fun topicsForUser(userKey: String): List<DictionaryTopic>

    fun createTopic(
        userKey: String,
        title: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
        coverIndex: Int,
        iconIndex: Int,
    ): DictionaryTopic

    /**
     * Оновлює назву/колір/іконку наявного словника (шит «Змінити»). Пара мов
     * незмінна — вона фіксується при створенні. Повертає `true`, якщо словник
     * знайдено й оновлено.
     */
    fun updateTopicAppearance(
        userKey: String,
        topicId: String,
        title: String,
        coverIndex: Int,
        iconIndex: Int,
    ): Boolean

    fun removeTopic(
        userKey: String,
        topicId: String,
    ): Boolean

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

    /**
     * Очищення словника (шит «Очистити словник?»): прибирає ВСІ слова разом із
     * прогресом засвоєння, сам словник лишається. Повертає кількість прибраних
     * слів — 0, якщо словника нема або він і так порожній.
     */
    fun clearTopicWords(
        userKey: String,
        topicId: String,
    ): Int

    fun adjustWordKnowledgePercent(
        userKey: String,
        topicId: String,
        wordId: String,
        deltaPercent: Int,
    ): WordEntry?

    /**
     * Оновлює збагачення збереженого слова (details + ipa) — використовується
     * бекфілом sense-атрибуції для контекстного тренування. Слово стає
     * PendingUpdate, щоб збагачення доїхало на сервер звичайним sync'ом.
     */
    fun updateWordEnrichment(
        userKey: String,
        topicId: String,
        wordId: String,
        ipa: String?,
        details: WordDetails?,
    ): WordEntry?

    fun hasVocabulary(userKey: String): Boolean

    fun exportSyncSnapshot(
        userKey: String,
        includeDeleted: Boolean,
    ): VocabularySyncSnapshot

    fun replaceSyncSnapshot(
        userKey: String,
        snapshot: VocabularySyncSnapshot,
    )

    fun markSynced(userKey: String)

    fun moveUserVocabulary(
        fromUserKey: String,
        toUserKey: String,
    )
}
