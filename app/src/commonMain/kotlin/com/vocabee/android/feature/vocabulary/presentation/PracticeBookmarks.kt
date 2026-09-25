package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.conflictsWithCandidate

internal data class PracticeBookmark(
    val key: String,
    val source: String,
    val translation: String,
    val sentence: String,
    val sourceLang: String,
    val targetLang: String,
    val originTopicId: String,
    val glossary: ContextGlossary,
    val translationId: String? = null,
    val senseKey: String? = null,
) {
    fun toWordDetails(): WordDetails = WordDetails(
        translationId = translationId,
        senseKeys = listOfNotNull(senseKey),
        usageExample = sentence,
        contextGlossary = glossary,
    )

    fun savedWordIn(words: List<WordEntry>): WordEntry? =
        words.firstOrNull { it.conflictsWithCandidate(source, translation, toWordDetails()) }
}

internal fun practiceBookmark(
    glossary: ContextGlossary,
    token: ContextGlossaryToken,
    originTopicId: String,
): PracticeBookmark {
    val source = token.lemma?.trim()?.takeIf { it.isNotEmpty() } ?: token.surface
    return PracticeBookmark(
        key = contextBookmarkKey(glossary, token),
        source = source,
        translation = token.translation.trim(),
        sentence = glossary.sentence,
        sourceLang = glossary.sourceLang,
        targetLang = glossary.targetLang,
        originTopicId = originTopicId,
        glossary = glossary,
        translationId = token.translationId,
        senseKey = token.senseKey,
    )
}

/**
 * Тап у попапі перемикає закладку: перший — додає, повторний — знімає
 * (борд 13, «повторний тап у поповері знімає закладку»).
 */
internal fun toggledPracticeBookmarks(
    current: List<PracticeBookmark>,
    bookmark: PracticeBookmark,
): List<PracticeBookmark> = when {
    current.any { it.key == bookmark.key } -> current.filterNot { it.key == bookmark.key }
    bookmark.source.isBlank() || bookmark.translation.isBlank() -> current
    else -> current + bookmark
}

/**
 * Збережені закладки лишаються в картці результату з ✓, але зникають із
 * бейджа й з лічильника «Додати всі (N)» (борд 13, фрейм 10).
 */
internal fun pendingPracticeBookmarks(
    bookmarks: List<PracticeBookmark>,
    savedKeys: Set<String>,
): List<PracticeBookmark> = bookmarks.filterNot { bookmark -> bookmark.key in savedKeys }

internal fun compatibleBookmarkTopics(
    bookmarks: List<PracticeBookmark>,
    topics: List<com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic>,
): List<com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic> {
    if (bookmarks.isEmpty()) return emptyList()
    return topics.filter { topic ->
        bookmarks.all { bookmark ->
            topic.sourceLanguage.code == bookmark.sourceLang &&
                topic.targetLanguage.code == bookmark.targetLang
        }
    }
}
