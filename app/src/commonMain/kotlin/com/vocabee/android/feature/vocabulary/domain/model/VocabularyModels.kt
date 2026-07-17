package com.vocabee.android.feature.vocabulary.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

const val DEFAULT_LOCAL_USER_KEY = "local-user"

@Serializable
enum class LexicalUnitKind {
    @SerialName("word") Word,
    @SerialName("phrase") Phrase,
    @SerialName("expression") Expression,
    @SerialName("abbreviation") Abbreviation,
}

@Serializable
enum class LexicalRegisterTag {
    @SerialName("slang") Slang,
    @SerialName("informal") Informal,
    @SerialName("formal") Formal,
    @SerialName("technical") Technical,
    @SerialName("offensive") Offensive,
    @SerialName("humorous") Humorous,
    @SerialName("internet") Internet,
}

@Serializable
data class WordSense(
    val definition: String,
    val partOfSpeech: String? = null,
    val tags: List<String> = emptyList(),
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
)

@Serializable
data class WordForm(
    val text: String,
    val tags: List<String> = emptyList(),
)

/**
 * Rich enrichment for a saved word — populated from the gateway's search response
 * at the moment the user taps "+" and persisted to Room as a single JSON blob via
 * [com.vocabee.android.feature.vocabulary.data.local.VocabeeTypeConverters]. Stays read-only on the
 * mobile: the server is the source of truth for everything in here.
 */
@Serializable
data class WordDetails(
    /**
     * Індекс sense'а (в [senses]), який рендерить переклад цієї пари — з
     * бекендової атрибуції. Null — ще не атрибутовано (старі збереження);
     * контекстне тренування тоді відступає до першого приклада слова.
     */
    val senseIndex: Int? = null,
    val senses: List<WordSense> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val forms: List<WordForm> = emptyList(),
    val partOfSpeech: List<String> = emptyList(),
    /** Structure and register are independent: e.g. LOL = Abbreviation + Slang. */
    val lexicalUnitKind: LexicalUnitKind = LexicalUnitKind.Word,
    val registerTags: List<LexicalRegisterTag> = emptyList(),
    /** Abbreviation expansion in learning and known languages. */
    val expansion: String? = null,
    val translatedExpansion: String? = null,
    /** Explanation and literal rendering in the user's known language. */
    val meaning: String? = null,
    val literalTranslation: String? = null,
    /** Natural usage example in the learning language and its translation. */
    val usageExample: String? = null,
    val usageExampleTranslation: String? = null,
) {
    val isEmpty: Boolean
        get() = senses.isEmpty() && synonyms.isEmpty() && antonyms.isEmpty() &&
            forms.isEmpty() && partOfSpeech.isEmpty() &&
            lexicalUnitKind == LexicalUnitKind.Word && registerTags.isEmpty() &&
            expansion.isNullOrBlank() && translatedExpansion.isNullOrBlank() &&
            meaning.isNullOrBlank() && literalTranslation.isNullOrBlank() &&
            usageExample.isNullOrBlank() && usageExampleTranslation.isNullOrBlank()
}

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
    /** IPA transcription for [source], when the gateway knew one. Null if not. */
    val ipa: String? = null,
    /**
     * Rich enrichment from the gateway: definitions, examples, synonyms, antonyms,
     * inflected forms. Null when never enriched (e.g. word added before this field
     * shipped).
     */
    val details: WordDetails? = null,
    val knowledgePercent: Int = 0,
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
    val iconIndex: Int = 0,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = createdAtEpochMillis,
    val syncStatus: SyncStatus = SyncStatus.PendingCreate,
    val words: List<WordEntry> = emptyList(),
)

data class VocabularySyncSnapshot(
    val topics: List<DictionaryTopic>,
    val deletedTopicIds: List<String> = emptyList(),
    val deletedWordIds: List<String> = emptyList(),
)

sealed interface TopicUpdatedLabel {
    data object Today : TopicUpdatedLabel
    data object Yesterday : TopicUpdatedLabel
    data class DaysAgo(val count: Int) : TopicUpdatedLabel
    data class WeeksAgo(val count: Int) : TopicUpdatedLabel
}

data class TranslationOption(
    /** Translation text — what gets stored as `WordEntry.translation` when added. */
    val value: String,
    val note: TranslationOptionNote,
    val alreadyAdded: Boolean = false,
    /**
     * Canonical word in the language the user is LEARNING — rendered as the headword
     * in the Add Word result row. Falls back to [value] when the source doesn't have
     * a separate canonical form (e.g. on local options without a backend response).
     */
    val learningWord: String = value,
    /** IPA transcription for [learningWord]. Null when none is known. */
    val ipa: String? = null,
    /** Rich dictionary enrichment (senses, synonyms, antonyms, forms). */
    val details: WordDetails? = null,
)

sealed interface TranslationOptionNote {
    data object Primary : TranslationOptionNote
    data object Alternative : TranslationOptionNote
    data object Additional : TranslationOptionNote
    data class AlreadyAdded(val source: String) : TranslationOptionNote
}
