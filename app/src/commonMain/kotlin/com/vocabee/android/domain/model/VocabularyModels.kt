package com.vocabee.android.domain.model

const val DEFAULT_LOCAL_USER_KEY = "local-user"

data class LanguageOption(
    val code: String,
    val name: String,
    val shortName: String,
    val speechTag: String,
)

enum class SyncStatus {
    PendingCreate,
    PendingUpdate,
    Synced,
    PendingDelete,
}

data class WordEntry(
    val id: String,
    val source: String,
    val translation: String,
    val addedAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = addedAtEpochMillis,
    val syncStatus: SyncStatus = SyncStatus.PendingCreate,
)

data class DictionaryTopic(
    val id: String,
    val userKey: String = DEFAULT_LOCAL_USER_KEY,
    val title: String,
    val sourceLanguage: LanguageOption,
    val targetLanguage: LanguageOption,
    val updatedLabel: TopicUpdatedLabel = TopicUpdatedLabel.Today,
    val coverIndex: Int = 0,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val syncStatus: SyncStatus = SyncStatus.PendingCreate,
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
    data class AlreadyAdded(val source: String) : TranslationOptionNote
}
