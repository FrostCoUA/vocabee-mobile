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
    /** Stable backend identity; null for legacy backend rows or pre-V2 local snapshots. */
    val senseKey: String? = null,
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

/** One server-tokenized word occurrence inside the exact saved example sentence. */
@Serializable
data class ContextGlossaryToken(
    val surface: String,
    val normalized: String,
    /** UTF-16 offsets; Kotlin String and the gateway use the same indexing. */
    val start: Int,
    val endExclusive: Int,
    val translation: String,
    val lemma: String? = null,
)

/** Offline snapshot used by the clickable practice sentence. */
@Serializable
data class ContextGlossary(
    val sentence: String,
    val sourceLang: String,
    val targetLang: String,
    val tokens: List<ContextGlossaryToken>,
)

/**
 * Rich enrichment for a saved word — populated from the gateway's search response
 * at the moment the user taps "+" and persisted to Room as a single JSON blob by
 * [com.vocabee.android.feature.vocabulary.data.RoomVocabularyRepository]. Stays read-only
 * on mobile: the server is the source of truth for everything in here.
 */
@Serializable
data class WordDetails(
    /**
     * Opaque identity/version fields used by server-authoritative lexicon refresh.
     * They are persisted and synced, but deliberately do not make an otherwise
     * empty details payload expandable in the UI.
     */
    val translationId: String? = null,
    val lexiconSchemaVersion: Int? = null,
    val lexiconRevision: String? = null,
    /** Stable V2 meanings rendered by this translation. */
    val senseKeys: List<String> = emptyList(),
    /**
     * Індекс sense'а (в [senses]), який рендерить переклад цієї пари — з
     * бекендової атрибуції. Null — ще не атрибутовано (старі збереження);
     * контекстне тренування тоді відступає до першого приклада слова.
     */
    val senseIndex: Int? = null,
    val senses: List<WordSense> = emptyList(),
    /**
     * Усі переклади СЕНС-ГРУПИ, до якої належить ця пара (представник першим) —
     * знімок із пошуку: `бігти` і `гнати` для сенсу «рухатися швидко». Порожній
     * для одинарних груп і для збережень до цієї фічі; споживач, що рендерить
     * «близькі за значенням», сам відкидає власний переклад запису.
     */
    val senseGroupTranslations: List<String> = emptyList(),
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
    /** Background batch enrichment for the exact sentence used in practice. */
    val contextGlossary: ContextGlossary? = null,
) {
    val isEmpty: Boolean
        get() = senses.isEmpty() && synonyms.isEmpty() && antonyms.isEmpty() &&
            forms.isEmpty() && partOfSpeech.isEmpty() &&
            lexicalUnitKind == LexicalUnitKind.Word && registerTags.isEmpty() &&
            expansion.isNullOrBlank() && translatedExpansion.isNullOrBlank() &&
            meaning.isNullOrBlank() && literalTranslation.isNullOrBlank() &&
            usageExample.isNullOrBlank() && usageExampleTranslation.isNullOrBlank() &&
            contextGlossary == null

    /** True when this snapshot must survive storage/sync despite having no visible details. */
    val hasLexiconSnapshot: Boolean
        get() = !translationId.isNullOrBlank() ||
            lexiconSchemaVersion != null ||
            !lexiconRevision.isNullOrBlank()

    /**
     * [senseGroupTranslations] окремим доданком: перелік «близьких за значенням»
     * — це вже вміст, вартий збереження, навіть коли решта деталей порожня
     * (варіант без сенсів і без id). У [isEmpty] він свідомо НЕ входить: той
     * прапорець керує розгортанням деталей у UI.
     */
    val shouldPersist: Boolean
        get() = !isEmpty || hasLexiconSnapshot || senseGroupTranslations.isNotEmpty()
}

/**
 * Підпис атрибуції перекладу: відсортовані стабільні `senseKeys`, інакше легасі
 * `legacy:$senseIndex` (лише якщо індекс справді вказує на наявний sense).
 * Null — атрибуції немає взагалі (збереження до V2).
 */
internal fun WordDetails.attributionSignature(): String? {
    val stableKeys = senseKeys.filter(String::isNotBlank).distinct().sorted()
    if (stableKeys.isNotEmpty()) return stableKeys.joinToString(separator = "\u0000")
    return senseIndex?.takeIf { it in senses.indices }?.let { "legacy:$it" }
}

/**
 * Множина сенсів, за якою переклад зливається з групою. Стабільні `senseKeys`
 * мають пріоритет; легасі-атрибуція представлена одним псевдоключем
 * `legacy:$senseIndex` (те саме правило, що й у [attributionSignature]).
 * Порожня множина = атрибуції немає взагалі (легасі-запис).
 *
 * Живе в домені навмисно: це правило спільне для збережених слів
 * (`groupBySense`, `overlapsSenseGroup`) і для групування відповіді пошуку
 * (`toSenseGroupedOptions`) — копії правила розійшлися б.
 */
internal fun WordDetails?.senseMergeKeys(): Set<String> {
    val details = this ?: return emptySet()
    val stableKeys = details.senseKeys.filter(String::isNotBlank).toSet()
    if (stableKeys.isNotEmpty()) return stableKeys
    return setOfNotNull(details.attributionSignature())
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

/**
 * Ключ збереженого слова — саме ПАРА (слово, переклад), а не самий переклад.
 * У словнику законно співіснують `run→серія` і `series→серія`, тож і позначка
 * «вже додано», і видалення мусять розрізняти їх. Регістр і пробіли по краях
 * не значущі — так само, як у SQL-запитах DAO (`LOWER(...)`).
 */
fun savedWordKey(source: String, translation: String): String =
    "${source.trim().lowercase()}\u0000${translation.trim().lowercase()}"

/** Набір ключів [savedWordKey] для всіх слів словника. */
fun List<WordEntry>.savedWordKeys(): Set<String> =
    mapTo(mutableSetOf()) { savedWordKey(it.source, it.translation) }

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
    /** Opaque dictionary translation id used only for quality feedback. */
    val translationId: String = "",
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
    /**
     * «Близькі за значенням» — решта перекладів ТОГО САМОГО сенсу, кожен
     * повноцінною опцією (свій `translationId` і свої `details`, у яких той
     * самий `senseGroupTranslations`). Тому ✓ на альтернативі зберігається
     * звичайним `onAdd(альтернатива)`, без переписування головної опції.
     * Порожній у самих альтернатив — вкладеність на один рівень.
     */
    val alternatives: List<TranslationOption> = emptyList(),
)

sealed interface TranslationOptionNote {
    data object Primary : TranslationOptionNote
    data object Alternative : TranslationOptionNote
    data object Additional : TranslationOptionNote
    data class AlreadyAdded(val source: String) : TranslationOptionNote
}
