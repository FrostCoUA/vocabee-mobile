package com.vocabee.android.domain.model

data class LanguageOption(
    val code: String,
    val name: String,
    val shortName: String,
    val speechTag: String,
)

data class WordEntry(
    val id: String,
    val source: String,
    val translation: String,
)

data class DictionaryTopic(
    val id: String,
    val title: String,
    val sourceLanguage: LanguageOption,
    val targetLanguage: LanguageOption,
    val updatedLabel: TopicUpdatedLabel = TopicUpdatedLabel.Today,
    val coverIndex: Int = 0,
    val words: List<WordEntry> = emptyList(),
)

sealed interface TopicUpdatedLabel {
    data object Today : TopicUpdatedLabel
    data object Yesterday : TopicUpdatedLabel
    data class DaysAgo(val count: Int) : TopicUpdatedLabel
    data class WeeksAgo(val count: Int) : TopicUpdatedLabel
}

data class TranslationOption(
    val value: String,
    val note: TranslationOptionNote,
    val alreadyAdded: Boolean = false,
)

sealed interface TranslationOptionNote {
    data object Primary : TranslationOptionNote
    data object Alternative : TranslationOptionNote
    data object Additional : TranslationOptionNote
    data object MlKitOnDevice : TranslationOptionNote
    data class AlreadyAdded(val source: String) : TranslationOptionNote
}
