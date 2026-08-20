package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.core.analytics.NoAnalyticsTracker
import com.vocabee.android.core.platform.currentEpochMillis
import com.vocabee.android.core.platform.debugLog
import com.vocabee.android.feature.vocabulary.data.api.SearchResponse
import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApiException
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.LexicalRegisterTag
import com.vocabee.android.feature.vocabulary.domain.model.LexicalUnitKind
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordForm
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.domain.model.savedWordKey
import com.vocabee.android.feature.vocabulary.domain.model.senseMergeKeys

/** Тег для `adb logcat -s VocabeeSearch` — звідки прийшов кожен переклад. */
internal const val SearchLogTag = "VocabeeSearch"

/**
 * Calls the gateway's `/search` endpoint and adapts the response into the
 * presentation-layer [TranslationOption] list used by the Add Word overlay.
 */
class RemoteLexiconSearchUseCase(
    private val api: VocabeeApi,
    private val analytics: AnalyticsTracker = NoAnalyticsTracker,
) {
    /**
     * @param savedWordKeys ключі [savedWordKey] уже збережених пар (слово,
     * переклад) поточного словника — з них рахується позначка «вже додано».
     */
    suspend operator fun invoke(
        query: String,
        speakLang: String,
        learnLang: String,
        savedWordKeys: Set<String>,
    ): Result {
        if (query.isBlank()) {
            return Result.Ok(
                query = query,
                options = emptyList(),
                tier = "anonymous",
                maxResults = 0,
                beeBalance = null,
            )
        }
        return try {
            val startedAt = currentEpochMillis()
            val response = api.search(
                query = query,
                speakLang = speakLang,
                learnLang = learnLang,
            )
            trackSearchResult(response, currentEpochMillis() - startedAt)
            val options = response.results.toSenseGroupedOptions(savedWordKeys)
            Result.Ok(
                query = query,
                options = options,
                tier = response.tier,
                maxResults = response.maxResults,
                beeBalance = response.meta.beeBalance,
            )
        } catch (cause: VocabeeApiException) {
            analytics.track(
                "translation_search_failed",
                mapOf("query" to query, "status_code" to cause.statusCode, "message" to cause.errorMessage),
            )
            Result.Failure(
                query = query,
                statusCode = cause.statusCode,
                message = cause.errorMessage ?: "Network error",
            )
        } catch (cause: Throwable) {
            analytics.track(
                "translation_search_failed",
                mapOf("query" to query, "status_code" to null, "message" to cause.message),
            )
            Result.Failure(
                query = query,
                statusCode = null,
                message = cause.message ?: "Network error",
            )
        }
    }

    /**
     * Клієнтський зріз відповіді `/search`: сирі `source`/`origin` губляться
     * при мапінгу в [TranslationOption], тож фіксуємо джерело даних (база
     * проти AI) саме тут, поки воно ще в руках.
     */
    private fun trackSearchResult(response: SearchResponse, durationMs: Long) {
        val primary = response.results.firstOrNull { it.isPrimary } ?: response.results.firstOrNull()
        val dataSource = translationDataSource(response, primary)
        val cachedCount = response.results.count { it.cached }
        val origin = primary?.origin ?: response.meta.dictionaryOrigin

        debugLog(
            SearchLogTag,
            "q='${response.query}' source=$dataSource ms=$durationMs n=${response.results.size} " +
                "cached=$cachedCount/${response.results.size} triedProvider=${response.meta.triedProvider} " +
                "reason=${response.meta.providerReason ?: "-"} origin=${origin ?: "-"}",
        )

        analytics.track(
            "translation_search_result",
            mapOf(
                "query" to response.query,
                "detected_lang" to response.detectedLang,
                "known_lang" to response.knownLang,
                "learning_lang" to response.learningLang,
                "tier" to response.tier,
                "results_count" to response.results.size,
                "cached_results_count" to cachedCount,
                "bee_balance" to response.meta.beeBalance,
                "translation_origin" to origin,
                "provider_reason" to response.meta.providerReason,
                "tried_provider" to response.meta.triedProvider,
                "data_source" to dataSource,
                "duration_ms" to durationMs,
            ),
        )
    }

    sealed interface Result {
        val query: String

        data class Ok(
            override val query: String,
            val options: List<TranslationOption>,
            val tier: String,
            val maxResults: Int,
            val beeBalance: Int?,
        ) : Result

        data class Failure(
            override val query: String,
            val statusCode: Int?,
            val message: String,
        ) : Result
    }
}

/**
 * `providerReason` від сервера — авторитетне джерело: воно каже, чи сервер
 * реально ходив до провайдера. Якщо його немає (старий сервер), падаємо
 * назад на ознаку `cached` у першому результаті.
 */
internal fun translationDataSource(response: SearchResponse, primary: SearchVariant?): String =
    when (response.meta.providerReason) {
        "exact_cached" -> "database"
        "translated" -> if (isAiOrigin(primary?.origin)) "ai" else "provider"
        "not_a_word", "echo", "no_provider_data" -> "none"
        else -> when {
            primary == null -> "none"
            primary.cached -> "database"
            isAiOrigin(primary.origin) -> "ai"
            else -> "provider"
        }
    }

private fun isAiOrigin(origin: String?): Boolean =
    origin?.startsWith("openai-") == true || origin?.startsWith("ai-") == true

/**
 * Один айтем на СЕНС: варіанти, що рендерять те саме значення, зливаються в
 * одну опцію — головний переклад плюс [TranslationOption.alternatives]
 * («близькі за значенням»).
 *
 * **Мердж «якір на головного»:** група тримає множину сенсів свого ПЕРШОГО
 * (головного) варіанта; кандидат вливається, якщо його ключі
 * ([senseMergeKeys] — стабільні `senseKeys`, інакше легасі `legacy:$senseIndex`)
 * ПЕРЕТИНАЮТЬСЯ з ключами якоря. Рівності вимагати не можна: у V2-даних
 * ~1,6% головних слів мають ширший переклад із зайвим сенсом
 * (`strange` → `дивний[k1]` + `чудернацький[k1,k2]`), і два рядки з тим самим
 * підписом виглядали б як баг.
 *
 * Якір НЕ розширюється прийнятими кандидатами, тобто перетин **не
 * транзитивний**: `[k2]` не приклеїться до групи `[k1]` через місток
 * `[k1,k2]`. Інакше ланцюжок сенсів злипався б в одну «мега-групу».
 *
 * Слово-джерело входить у порівняння обов'язково: однаковий senseKey у різних
 * слів — різні сенси. Неатрибутовані варіанти НЕ зливаються навіть між собою:
 * без атрибуції невідомо, чи це той самий сенс, тож кожен лишається окремим
 * рядком (стара поведінка списку).
 *
 * Головний у групі — перший за порядком сервера: сервер уже сортує за
 * `isPrimary`/`confidence`, тож клієнт не перевпорядковує.
 *
 * @param savedWordKeys ключі [savedWordKey] збережених пар (слово, переклад).
 */
internal fun List<SearchVariant>.toSenseGroupedOptions(savedWordKeys: Set<String>): List<TranslationOption> {
    class SenseBucket(val sourceKey: String, val anchorKeys: Set<String>) {
        val members = mutableListOf<SearchVariant>()
    }

    val buckets = mutableListOf<SenseBucket>()
    for (variant in this) {
        val sourceKey = variant.learningWord.trim().lowercase()
        val keys = variant.attributionKeys()
        // Порожні ключі не перетинаються ні з чим — неатрибутований варіант
        // завжди відкриває власну групу (і в неї теж ніхто не вливається).
        val matched = if (keys.isEmpty()) null else buckets.firstOrNull { bucket ->
            bucket.sourceKey == sourceKey && bucket.anchorKeys.any(keys::contains)
        }
        val target = matched ?: SenseBucket(sourceKey, keys).also(buckets::add)
        target.members += variant
    }
    return buckets.map { bucket -> bucket.members.toSenseGroupOption(savedWordKeys) }
}

/**
 * Атрибуція варіанта — тим самим доменним правилом [senseMergeKeys], але на
 * ЛЕГКОМУ зліпку: правило читає лише `senseKeys`, `senseIndex` і межі `senses`,
 * тож повний маппінг деталей (приклади, форми, синоніми) тут зайвий — його
 * робить [toOption] рівно один раз на варіант.
 */
private fun SearchVariant.attributionKeys(): Set<String> = WordDetails(
    senseKeys = senseKeys.distinct(),
    senseIndex = senseIndex,
    senses = senses.map { sense -> WordSense(senseKey = sense.senseKey, definition = sense.definition) },
).senseMergeKeys()

/**
 * Опція одного сенсу. `value` — головний переклад, `alternatives` — решта
 * групи, `details.senseGroupTranslations` (у головного і в кожної
 * альтернативи) — увесь список групи, представник першим; тому ✓ на
 * альтернативі зберігає її переклад із тим самим сенс-контекстом.
 *
 * `alreadyAdded` рахується по ВСІЙ групі: збережений `run→гнати` означає, що
 * сенс уже у словнику, хай навіть головний переклад — `бігти`. Живу позначку
 * рядка UI все одно бере з `savedWordKeys` по конкретній парі (`isSavedIn`).
 */
private fun List<SearchVariant>.toSenseGroupOption(savedWordKeys: Set<String>): TranslationOption {
    val members = distinctByTranslation()
    // Список із самого себе не несе інформації — «близькі» лишаються порожніми.
    val groupTranslations = if (members.size > 1) members.map(SearchVariant::knownWord) else emptyList()
    val head = members.first().toOption(savedWordKeys, groupTranslations)
    val alternatives = members.drop(1).map { member -> member.toOption(savedWordKeys, groupTranslations) }
    val groupSaved = head.alreadyAdded || alternatives.any(TranslationOption::alreadyAdded)
    return head.copy(
        alreadyAdded = groupSaved,
        note = if (groupSaved) TranslationOptionNote.AlreadyAdded(source = members.first().origin) else head.note,
        alternatives = alternatives,
    )
}

/**
 * Однаковий переклад у межах групи — один рядок. Позиція визначається першим
 * входженням, але з дублів лишається екземпляр із НЕПОРОЖНІМ `translationId`:
 * саме він дає скаргу «неякісний переклад» і зв'язок із лексиконом.
 */
private fun List<SearchVariant>.distinctByTranslation(): List<SearchVariant> {
    val kept = LinkedHashMap<String, SearchVariant>()
    for (member in this) {
        val key = member.knownWord.trim().lowercase()
        val previous = kept[key]
        if (previous == null || (previous.translationId.isBlank() && member.translationId.isNotBlank())) {
            kept[key] = member
        }
    }
    return kept.values.toList()
}

/**
 * @param savedWordKeys ключі [savedWordKey] збережених пар (слово, переклад).
 * Позначка «вже додано» звіряє саме пару: збережений `run→серія` не робить
 * «доданим» варіант `series→серія`.
 * @param senseGroupTranslations усі переклади сенс-групи (представник першим);
 * порожній для одинарної групи.
 */
internal fun SearchVariant.toOption(
    savedWordKeys: Set<String>,
    senseGroupTranslations: List<String> = emptyList(),
): TranslationOption {
    val translation = knownWord
    val alreadySaved = savedWordKeys.contains(
        savedWordKey(source = learningWord, translation = translation),
    )
    val note = when {
        alreadySaved -> TranslationOptionNote.AlreadyAdded(source = origin)
        source == "dictionary" -> TranslationOptionNote.Primary
        source == "translator" -> TranslationOptionNote.Primary
        source == "ai" -> TranslationOptionNote.Additional
        else -> TranslationOptionNote.Alternative
    }
    return TranslationOption(
        translationId = translationId,
        value = translation,
        note = note,
        alreadyAdded = alreadySaved,
        learningWord = learningWord,
        ipa = ipa,
        details = toWordDetails(senseGroupTranslations).takeIf { it.shouldPersist },
    )
}

/**
 * Повний зліпок деталей варіанта — БЕЗ фільтра `shouldPersist`: групування
 * читає з нього `senseKeys`/`senseIndex` ще до того, як порожній зліпок буде
 * відкинуто.
 */
private fun SearchVariant.toWordDetails(
    senseGroupTranslations: List<String> = emptyList(),
): WordDetails = WordDetails(
    translationId = translationId.takeIf { it.isNotBlank() },
    lexiconSchemaVersion = lexiconSchemaVersion,
    lexiconRevision = lexiconRevision,
    senseKeys = senseKeys.distinct(),
    senseIndex = senseIndex,
    senses = senses.map { sense ->
        WordSense(
            senseKey = sense.senseKey,
            definition = sense.definition,
            partOfSpeech = sense.partOfSpeech,
            tags = sense.tags,
            examples = sense.examples.map { it.text },
            synonyms = sense.synonyms,
            antonyms = sense.antonyms,
        )
    },
    senseGroupTranslations = senseGroupTranslations,
    synonyms = synonyms,
    antonyms = antonyms,
    forms = forms.map { WordForm(text = it.text, tags = it.tags) },
    partOfSpeech = partOfSpeech,
    lexicalUnitKind = lexicalUnitKind.toLexicalUnitKind(),
    registerTags = registerTags.mapNotNull(String::toLexicalRegisterTag).distinct(),
    expansion = expansion,
    translatedExpansion = translatedExpansion,
    meaning = meaning,
    literalTranslation = literalTranslation,
    usageExample = usageExample,
    usageExampleTranslation = usageExampleTranslation,
)

private fun String.toLexicalUnitKind(): LexicalUnitKind = when (lowercase()) {
    "phrase" -> LexicalUnitKind.Phrase
    "expression" -> LexicalUnitKind.Expression
    "abbreviation" -> LexicalUnitKind.Abbreviation
    else -> LexicalUnitKind.Word
}

private fun String.toLexicalRegisterTag(): LexicalRegisterTag? = when (lowercase()) {
    "slang" -> LexicalRegisterTag.Slang
    "informal" -> LexicalRegisterTag.Informal
    "formal" -> LexicalRegisterTag.Formal
    "technical" -> LexicalRegisterTag.Technical
    "offensive" -> LexicalRegisterTag.Offensive
    "humorous" -> LexicalRegisterTag.Humorous
    "internet" -> LexicalRegisterTag.Internet
    else -> null
}
