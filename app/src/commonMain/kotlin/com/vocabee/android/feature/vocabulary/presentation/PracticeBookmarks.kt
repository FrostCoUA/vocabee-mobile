package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails

internal data class PracticeBookmark(
    val key: String,
    val source: String,
    val translation: String,
    val sentence: String,
    val sourceLang: String,
    val targetLang: String,
    val originTopicId: String,
    val glossary: ContextGlossary,
) {
    fun toWordDetails(): WordDetails = WordDetails(
        usageExample = sentence,
        contextGlossary = glossary,
    )
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
    )
}

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
