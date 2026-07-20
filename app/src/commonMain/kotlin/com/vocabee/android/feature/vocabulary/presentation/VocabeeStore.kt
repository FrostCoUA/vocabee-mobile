package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.core.analytics.NoAnalyticsTracker
import com.vocabee.android.core.platform.currentEpochMillis
import com.vocabee.android.core.platform.startOfDayEpochMillis
import com.vocabee.android.feature.vocabulary.data.FakeVocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.domain.manager.StaticUserSessionManager
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.usecase.AdjustWordKnowledgeUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.AddWordUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.ClearTopicWordsUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoveWordUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.CreateTopicUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.LoadUserTopicsUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoveTopicUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.UpdateTopicAppearanceUseCase
import com.vocabee.android.feature.vocabulary.domain.usecase.UpdateWordEnrichmentUseCase
import kotlin.math.roundToInt

internal const val InitialBeeBalance = 50
private const val DayMillis = 86_400_000L
internal const val RewardBeeAmount = 10
internal const val FreeDictionaryLimit = 2
internal const val AnonymousFreeWordLimit = 50
internal const val DictionaryCreationBeeCost = 10
internal const val TranslationSearchBeeCost = 1
internal const val CriticalBeeThreshold = 3

data class VocabeeState(
    val supportedLanguages: List<LanguageOption>,
    val userLanguage: LanguageOption,
    val learningLanguage: LanguageOption,
    val topics: List<DictionaryTopic>,
    val account: VocabeeAccountState = VocabeeAccountState.Anonymous,
    val topicCounter: Int = 0,
    val recentlyAddedWordId: String? = null,
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val beeBalance: Int = InitialBeeBalance,
    val streakDays: Int = 0,
    val practiceRounds: Int = 0,
)

/**
 * Ключ групування словників на Головній і в сетапі тренування: пара
 * «мова, яку вивчаю» → «мова, якою розмовляю». Робоча пара береться з профілю
 * ([VocabeeState.learningLanguage] → [VocabeeState.userLanguage]).
 */
data class TopicLanguagePair(
    val sourceCode: String,
    val targetCode: String,
)

/**
 * Група словників однієї мовної пари. Робоча пара ([isWorkingPair]) рендериться
 * першою і БЕЗ заголовка — це поточні словники; решта груп ідуть нижче з
 * розділювачем «ТЕМИ [прапор] → [прапор]».
 */
data class TopicPairGroup(
    val pair: TopicLanguagePair,
    val topics: List<DictionaryTopic>,
    val isWorkingPair: Boolean,
)

/** Мовна пара словника — ключ його групи на Головній і в сетапі тренування. */
internal fun DictionaryTopic.languagePair(): TopicLanguagePair =
    TopicLanguagePair(sourceLanguage.code, targetLanguage.code)

/**
 * Розкладає словники по мовних парах для Головної та сетапу тренування.
 *
 * Порядок: робоча пара (learning → user) завжди перша; решта — за спаданням
 * кількості словників, а за рівної кількості — стабільно за кодом мови-джерела,
 * потім за кодом мови-перекладу. Порожня робоча пара групи не створює.
 * Порядок словників УСЕРЕДИНІ групи зберігається таким, як його передав
 * викликач (Головна віддає найновіші першими, сетап — у природному порядку).
 */
internal fun groupTopicsByLanguagePair(
    topics: List<DictionaryTopic>,
    learningLanguageCode: String,
    userLanguageCode: String,
): List<TopicPairGroup> {
    if (topics.isEmpty()) return emptyList()
    val workingPair = TopicLanguagePair(learningLanguageCode, userLanguageCode)
    val grouped = topics.groupBy { topic ->
        TopicLanguagePair(topic.sourceLanguage.code, topic.targetLanguage.code)
    }
    val workingGroup = grouped[workingPair]?.let { pairTopics ->
        TopicPairGroup(pair = workingPair, topics = pairTopics, isWorkingPair = true)
    }
    val otherGroups = grouped
        .filterKeys { pair -> pair != workingPair }
        .map { (pair, pairTopics) ->
            TopicPairGroup(pair = pair, topics = pairTopics, isWorkingPair = false)
        }
        .sortedWith(
            compareByDescending<TopicPairGroup> { group -> group.topics.size }
                .thenBy { group -> group.pair.sourceCode }
                .thenBy { group -> group.pair.targetCode },
        )
    return listOfNotNull(workingGroup) + otherGroups
}

/**
 * Скільки словників має кожна мова вивчення В ПАРІ з рідною — джерело для
 * лічильника «N словники» та для розділення активних/неактивних мов у шиті
 * «Я вивчаю». Мови без словників у мапі відсутні.
 */
internal fun learningLanguageTopicCounts(
    topics: List<DictionaryTopic>,
    userLanguageCode: String,
): Map<String, Int> {
    return topics
        .filter { topic -> topic.targetLanguage.code == userLanguageCode }
        .groupingBy { topic -> topic.sourceLanguage.code }
        .eachCount()
}

sealed interface VocabeeAccountState {
    data object Anonymous : VocabeeAccountState

    data class Authenticated(
        val userId: String,
        val displayName: String,
        val email: String,
        val isSyncing: Boolean = false,
    ) : VocabeeAccountState
}

sealed interface VocabeeEvent {
    data class CreateTopic(
        val title: String,
        val coverIndex: Int,
        val iconIndex: Int = 0,
    ) : VocabeeEvent

    data class AddWord(
        val topicId: String,
        val source: String,
        val translation: String,
        val ipa: String? = null,
        val details: com.vocabee.android.feature.vocabulary.domain.model.WordDetails? = null,
    ) : VocabeeEvent

    /**
     * Редагування зовнішнього вигляду словника (назва/колір/іконка) з шита
     * «Змінити». Пара мов НЕ редагується — вона зафіксована при створенні (D6).
     */
    data class UpdateTopicAppearance(
        val topicId: String,
        val title: String,
        val coverIndex: Int,
        val iconIndex: Int,
    ) : VocabeeEvent

    data class RemoveTopic(
        val topicId: String,
    ) : VocabeeEvent

    /**
     * Очищення словника з меню хедера деталей: видаляє ВСІ слова разом із
     * прогресом засвоєння, сам словник лишається. Незворотно (шит вимагає
     * контрольної фрази), монетки не повертаються — як і за будь-яке
     * видалення (D3).
     */
    data class ClearTopicWords(
        val topicId: String,
    ) : VocabeeEvent

    data class RemoveWord(
        val topicId: String,
        /** Translation text — keys the row to delete (case-insensitive). */
        val translation: String,
    ) : VocabeeEvent

    data class AdjustWordKnowledge(
        val topicId: String,
        val wordId: String,
        val deltaPercent: Int,
    ) : VocabeeEvent

    data class SelectSpeakingLanguage(
        val language: LanguageOption,
    ) : VocabeeEvent

    data class SelectLearningLanguage(
        val language: LanguageOption,
    ) : VocabeeEvent

    data class SetNotificationsEnabled(
        val enabled: Boolean,
    ) : VocabeeEvent

    data class SetDarkThemeEnabled(
        val enabled: Boolean,
    ) : VocabeeEvent

    data class AddBees(
        val amount: Int = RewardBeeAmount,
    ) : VocabeeEvent

    data class SpendBees(
        val amount: Int,
    ) : VocabeeEvent

    data class SetBeeBalance(
        val amount: Int,
    ) : VocabeeEvent

    data class ApplyAuthenticatedAccount(
        val userId: String,
        val displayName: String,
        val email: String,
        val speakLang: String,
        val learnLang: String,
        val notificationsEnabled: Boolean,
        val darkThemeEnabled: Boolean,
        val beeBalance: Int,
    ) : VocabeeEvent
}

class VocabeeStore(
    private val repository: VocabularyRepository = FakeVocabularyRepository(),
    private val userSessionManager: UserSessionManager = StaticUserSessionManager(),
    private val preferencesManager: PreferencesManager = InMemoryPreferencesManager(),
    private val analytics: AnalyticsTracker = NoAnalyticsTracker,
) {
    private val loadUserTopicsUseCase = LoadUserTopicsUseCase(repository, userSessionManager)
    private val createTopicUseCase = CreateTopicUseCase(repository, userSessionManager)
    private val updateTopicAppearanceUseCase = UpdateTopicAppearanceUseCase(repository, userSessionManager)
    private val removeTopicUseCase = RemoveTopicUseCase(repository, userSessionManager)
    private val clearTopicWordsUseCase = ClearTopicWordsUseCase(repository, userSessionManager)
    private val addWordUseCase = AddWordUseCase(repository, userSessionManager)
    private val removeWordUseCase = RemoveWordUseCase(repository, userSessionManager)
    private val adjustWordKnowledgeUseCase = AdjustWordKnowledgeUseCase(repository, userSessionManager)
    private val updateWordEnrichmentUseCase = UpdateWordEnrichmentUseCase(repository, userSessionManager)

    var state by mutableStateOf(initialState())
        private set

    fun onEvent(event: VocabeeEvent) {
        when (event) {
            is VocabeeEvent.CreateTopic -> createTopic(event.title, event.coverIndex, event.iconIndex)
            is VocabeeEvent.UpdateTopicAppearance ->
                updateTopicAppearance(event.topicId, event.title, event.coverIndex, event.iconIndex)
            is VocabeeEvent.RemoveTopic -> removeTopic(event.topicId)
            is VocabeeEvent.ClearTopicWords -> clearTopicWords(event.topicId)
            is VocabeeEvent.AddWord -> addWord(event.topicId, event.source, event.translation, event.ipa, event.details)
            is VocabeeEvent.RemoveWord -> removeWord(event.topicId, event.translation)
            is VocabeeEvent.AdjustWordKnowledge -> adjustWordKnowledge(event.topicId, event.wordId, event.deltaPercent)
            is VocabeeEvent.SelectSpeakingLanguage -> selectSpeakingLanguage(event.language)
            is VocabeeEvent.SelectLearningLanguage -> selectLearningLanguage(event.language)
            is VocabeeEvent.SetNotificationsEnabled -> {
                state = state.copy(notificationsEnabled = event.enabled)
                touchLocalRevision()
            }
            is VocabeeEvent.SetDarkThemeEnabled -> {
                preferencesManager.darkThemeEnabled = event.enabled
                state = state.copy(darkThemeEnabled = event.enabled)
                touchLocalRevision()
            }
            is VocabeeEvent.AddBees -> addBees(event.amount)
            is VocabeeEvent.SpendBees -> spendBees(event.amount)
            is VocabeeEvent.SetBeeBalance -> setBeeBalance(event.amount)
            is VocabeeEvent.ApplyAuthenticatedAccount -> applyAuthenticatedAccount(event)
        }
    }

    fun isAuthenticated(): Boolean {
        return state.account is VocabeeAccountState.Authenticated
    }

    fun totalWordCount(): Int {
        return state.topics.sumOf { it.words.size }
    }

    fun hasLocalAnonymousVocabulary(): Boolean {
        return repository.hasVocabulary(DEFAULT_LOCAL_USER_KEY)
    }

    fun localAnonymousWordCount(): Int {
        return repository.exportSyncSnapshot(DEFAULT_LOCAL_USER_KEY, includeDeleted = false)
            .topics
            .sumOf { it.words.size }
    }

    /** Скільки локальних (анонімних) словників — друга цифра картки «На телефоні» в шиті конфлікту. */
    fun localAnonymousTopicCount(): Int {
        return repository.exportSyncSnapshot(DEFAULT_LOCAL_USER_KEY, includeDeleted = false)
            .topics
            .size
    }

    fun hasCurrentVocabulary(): Boolean {
        return repository.hasVocabulary(userSessionManager.currentUserKey)
    }

    fun exportCurrentSyncSnapshot(includeDeleted: Boolean): VocabularySyncSnapshot {
        return repository.exportSyncSnapshot(userSessionManager.currentUserKey, includeDeleted)
    }

    fun replaceCurrentSyncSnapshot(snapshot: VocabularySyncSnapshot) {
        repository.replaceSyncSnapshot(userSessionManager.currentUserKey, snapshot)
        state = state.copy(topics = loadUserTopicsUseCase())
    }

    fun markCurrentVocabularySynced(serverTime: String) {
        repository.markSynced(userSessionManager.currentUserKey)
        preferencesManager.lastSyncAt = serverTime
        preferencesManager.localRevisionEpochMillis = 0L
        state = state.copy(topics = loadUserTopicsUseCase())
    }

    fun moveAnonymousVocabularyToCurrentUser() {
        repository.moveUserVocabulary(DEFAULT_LOCAL_USER_KEY, userSessionManager.currentUserKey)
        touchLocalRevision()
        state = state.copy(topics = loadUserTopicsUseCase())
    }

    fun discardAnonymousVocabulary() {
        repository.replaceSyncSnapshot(DEFAULT_LOCAL_USER_KEY, VocabularySyncSnapshot(emptyList()))
    }

    fun anonymousDictionaryLimitReached(): Boolean {
        return !isAuthenticated() && state.topics.size >= FreeDictionaryLimit
    }

    fun anonymousWordLimitReached(): Boolean {
        return !isAuthenticated() && totalWordCount() >= AnonymousFreeWordLimit
    }

    fun canCreateTopic(): Boolean {
        return if (isAuthenticated()) {
            state.topics.size < FreeDictionaryLimit || state.beeBalance >= DictionaryCreationBeeCost
        } else {
            state.topics.size < FreeDictionaryLimit
        }
    }

    fun canSearchTranslation(): Boolean {
        return if (isAuthenticated()) {
            state.beeBalance >= TranslationSearchBeeCost
        } else {
            !anonymousWordLimitReached()
        }
    }

    fun canAddWordToDictionary(): Boolean {
        return isAuthenticated() || !anonymousWordLimitReached()
    }

    fun spendTranslationBee(): Boolean {
        if (!isAuthenticated()) return canSearchTranslation()
        return spendBees(TranslationSearchBeeCost)
    }

    private fun initialState(): VocabeeState {
        val supportedLanguages = repository.supportedLanguages
        // Restore the user's last-picked pair from persistent prefs, falling
        // back to uk → en for first-time users / fresh installs. We then
        // double-check the pair isn't the same language (e.g. someone deleted
        // a code we no longer ship); if it collapses, pick a sensible default.
        val savedUser = preferencesManager.userLanguageCode
        val savedLearning = preferencesManager.learningLanguageCode
        val userLanguage = supportedLanguages.firstOrNull { it.code == savedUser }
            ?: supportedLanguages.first { it.code == "uk" }
        val learningLanguage = supportedLanguages.firstOrNull {
            it.code == savedLearning && it.code != userLanguage.code
        } ?: supportedLanguages.first { it.code != userLanguage.code }

        return VocabeeState(
            supportedLanguages = supportedLanguages,
            userLanguage = userLanguage,
            learningLanguage = learningLanguage,
            topics = loadUserTopicsUseCase(),
            darkThemeEnabled = preferencesManager.darkThemeEnabled,
            beeBalance = preferencesManager.beeBalance.coerceAtLeast(0),
            streakDays = refreshedStreakDays(),
            practiceRounds = preferencesManager.practiceRoundsCompleted,
        )
    }

    /**
     * Бекфіл sense-збагачення: оновлює details/ipa збереженого слова і одразу
     * перечитує топіки, щоб лічильники пар контекстного тренування ожили.
     */
    fun updateWordEnrichment(topicId: String, wordId: String, ipa: String?, details: com.vocabee.android.feature.vocabulary.domain.model.WordDetails?) {
        updateWordEnrichmentUseCase(topicId = topicId, wordId = wordId, ipa = ipa, details = details) ?: return
        state = state.copy(topics = loadUserTopicsUseCase())
        touchLocalRevision()
    }

    /** Завершений раунд тренування (класика або контекст) — рахуємо в профіль. */
    fun recordPracticeRoundCompleted() {
        val rounds = preferencesManager.practiceRoundsCompleted + 1
        preferencesManager.practiceRoundsCompleted = rounds
        state = state.copy(practiceRounds = rounds, streakDays = refreshedStreakDays())
        analytics.track(
            "practice_round_completed",
            mapOf("rounds_total" to rounds, "streak_days" to state.streakDays),
        )
    }

    /**
     * Оновлює стрік «днів поспіль»: активність сьогодні продовжує вчорашній
     * стрік, пропущений день скидає його до 1. Порівнюємо локальні півночі,
     * а відстань у днях округлюємо, щоб перехід на літній/зимовий час
     * (доба ±1 година) не рвав серію.
     */
    private fun refreshedStreakDays(): Int {
        val todayStart = startOfDayEpochMillis(currentEpochMillis())
        val lastStart = preferencesManager.lastActiveDayStartMillis
        val updated = if (lastStart <= 0L) {
            1
        } else {
            val daysBetween = ((todayStart - lastStart).toDouble() / DayMillis).roundToInt()
            when {
                daysBetween <= 0 -> preferencesManager.streakDays.coerceAtLeast(1)
                daysBetween == 1 -> preferencesManager.streakDays.coerceAtLeast(0) + 1
                else -> 1
            }
        }
        preferencesManager.streakDays = updated
        preferencesManager.lastActiveDayStartMillis = todayStart
        return updated
    }

    private fun createTopic(
        title: String,
        coverIndex: Int,
        iconIndex: Int,
    ) {
        val cleanedTitle = title.trim()
        if (cleanedTitle.isBlank()) return
        if (!isAuthenticated() && state.topics.size >= FreeDictionaryLimit) return
        val shouldCharge = isAuthenticated() && state.topics.size >= FreeDictionaryLimit
        if (shouldCharge && !spendBees(DictionaryCreationBeeCost)) return

        createTopicUseCase(
            title = cleanedTitle,
            sourceLanguage = state.learningLanguage,
            targetLanguage = state.userLanguage,
            coverIndex = coverIndex,
            iconIndex = iconIndex,
        )
        state = state.copy(
            topics = loadUserTopicsUseCase(),
        )
        analytics.track(
            "dictionary_created",
            mapOf(
                "title" to cleanedTitle,
                "learning_lang" to state.learningLanguage.code,
                "known_lang" to state.userLanguage.code,
                "charged_beecoins" to shouldCharge,
                "dictionaries_count" to state.topics.size,
            ),
        )
        touchLocalRevision()
    }

    /**
     * Перейменування/перефарбування наявного словника. Безкоштовно — монетки
     * беруться лише за СТВОРЕННЯ понад безкоштовний ліміт (D1).
     */
    private fun updateTopicAppearance(
        topicId: String,
        title: String,
        coverIndex: Int,
        iconIndex: Int,
    ) {
        val cleanedTitle = title.trim()
        if (topicId.isBlank() || cleanedTitle.isBlank()) return
        val updated = updateTopicAppearanceUseCase(
            topicId = topicId,
            title = cleanedTitle,
            coverIndex = coverIndex,
            iconIndex = iconIndex,
        )
        if (!updated) return
        state = state.copy(topics = loadUserTopicsUseCase())
        touchLocalRevision()
    }

    private fun removeTopic(topicId: String) {
        if (topicId.isBlank()) return
        val removed = removeTopicUseCase(topicId = topicId)
        if (!removed) return
        state = state.copy(
            topics = loadUserTopicsUseCase(),
            recentlyAddedWordId = null,
        )
        analytics.track("dictionary_deleted", mapOf("topic_id" to topicId))
        touchLocalRevision()
    }

    /**
     * Очищення словника (шит H). Прогрес засвоєння зникає разом зі словами;
     * монетки за них НЕ повертаються — це те саме правило, що й для видалення
     * словника (D3). Порожній або неіснуючий словник стан не чіпає.
     */
    private fun clearTopicWords(topicId: String) {
        if (topicId.isBlank()) return
        val clearedCount = clearTopicWordsUseCase(topicId = topicId)
        if (clearedCount <= 0) return
        state = state.copy(
            topics = loadUserTopicsUseCase(),
            recentlyAddedWordId = null,
        )
        analytics.track(
            "dictionary_words_cleared",
            mapOf("topic_id" to topicId, "words_removed" to clearedCount),
        )
        touchLocalRevision()
    }

    private fun addWord(
        topicId: String,
        source: String,
        translation: String,
        ipa: String?,
        details: com.vocabee.android.feature.vocabulary.domain.model.WordDetails?,
    ) {
        val cleanedSource = source.trim()
        val cleanedTranslation = translation.trim()
        if (cleanedSource.isBlank() || cleanedTranslation.isBlank()) return
        if (!canAddWordToDictionary()) return

        val word = addWordUseCase(
            topicId = topicId,
            source = cleanedSource,
            translation = cleanedTranslation,
            ipa = ipa?.trim()?.takeIf { it.isNotEmpty() },
            details = details?.takeUnless { it.isEmpty },
        )
        if (word == null) return

        state = state.copy(
            topics = loadUserTopicsUseCase(),
            recentlyAddedWordId = word.id,
        )
        analytics.track(
            "word_added",
            mapOf(
                "topic_id" to topicId,
                "source" to cleanedSource,
                "translation" to cleanedTranslation,
                "has_details" to (details?.takeUnless { it.isEmpty } != null),
            ),
        )
        touchLocalRevision()
    }

    private fun removeWord(topicId: String, translation: String) {
        val cleaned = translation.trim()
        if (cleaned.isBlank()) return
        val removed = removeWordUseCase(topicId = topicId, translation = cleaned)
        if (!removed) return
        state = state.copy(topics = loadUserTopicsUseCase())
        analytics.track("word_deleted", mapOf("topic_id" to topicId))
        touchLocalRevision()
    }

    private fun adjustWordKnowledge(topicId: String, wordId: String, deltaPercent: Int) {
        if (wordId.isBlank() || deltaPercent == 0) return
        val updated = adjustWordKnowledgeUseCase(
            topicId = topicId,
            wordId = wordId,
            deltaPercent = deltaPercent,
        ) ?: return
        state = state.copy(
            topics = loadUserTopicsUseCase(),
            recentlyAddedWordId = state.recentlyAddedWordId.takeIf { it != updated.id },
        )
        analytics.track(
            "practice_answer",
            mapOf("topic_id" to topicId, "word_id" to wordId, "known" to (deltaPercent > 0)),
        )
        touchLocalRevision()
    }

    private fun addBees(amount: Int) {
        if (amount <= 0) return
        if (!isAuthenticated()) return
        val nextBalance = (state.beeBalance + amount).coerceAtLeast(0)
        preferencesManager.beeBalance = nextBalance
        state = state.copy(beeBalance = nextBalance)
        analytics.track("beecoins_added", mapOf("amount" to amount, "balance" to nextBalance))
    }

    private fun spendBees(amount: Int): Boolean {
        if (amount <= 0) return true
        if (!isAuthenticated()) return false
        if (state.beeBalance < amount) return false
        val nextBalance = (state.beeBalance - amount).coerceAtLeast(0)
        preferencesManager.beeBalance = nextBalance
        state = state.copy(beeBalance = nextBalance)
        analytics.track("beecoins_spent", mapOf("amount" to amount, "balance" to nextBalance))
        return true
    }

    private fun setBeeBalance(amount: Int) {
        val nextBalance = amount.coerceAtLeast(0)
        preferencesManager.beeBalance = nextBalance
        state = state.copy(beeBalance = nextBalance)
    }

    private fun selectSpeakingLanguage(language: LanguageOption) {
        val adjustedLearningLanguage = if (state.learningLanguage.code == language.code) {
            state.supportedLanguages.first { it.code != language.code }
        } else {
            state.learningLanguage
        }

        state = state.copy(
            userLanguage = language,
            learningLanguage = adjustedLearningLanguage,
        )
        persistLanguageChoice()
        touchLocalRevision()
    }

    private fun selectLearningLanguage(language: LanguageOption) {
        val adjustedUserLanguage = if (state.userLanguage.code == language.code) {
            state.supportedLanguages.first { it.code != language.code }
        } else {
            state.userLanguage
        }

        state = state.copy(
            userLanguage = adjustedUserLanguage,
            learningLanguage = language,
        )
        persistLanguageChoice()
        touchLocalRevision()
    }

    /**
     * Mirror the in-memory language pair into SharedPreferences so it survives
     * a relaunch. Called after every language change — both events go through
     * here, so the store stays the single source of truth and prefs are just
     * a snapshot.
     */
    private fun persistLanguageChoice() {
        preferencesManager.userLanguageCode = state.userLanguage.code
        preferencesManager.learningLanguageCode = state.learningLanguage.code
    }

    private fun applyAuthenticatedAccount(event: VocabeeEvent.ApplyAuthenticatedAccount) {
        val userLanguage = state.supportedLanguages.firstOrNull { it.code == event.speakLang }
            ?: state.userLanguage
        val learningLanguage = state.supportedLanguages.firstOrNull {
            it.code == event.learnLang && it.code != userLanguage.code
        } ?: state.learningLanguage.takeIf { it.code != userLanguage.code }
        ?: state.supportedLanguages.first { it.code != userLanguage.code }

        preferencesManager.userLanguageCode = userLanguage.code
        preferencesManager.learningLanguageCode = learningLanguage.code
        preferencesManager.darkThemeEnabled = event.darkThemeEnabled
        preferencesManager.beeBalance = event.beeBalance.coerceAtLeast(0)
        preferencesManager.currentUserId = event.userId
        preferencesManager.lastAuthenticatedUserId = event.userId

        state = state.copy(
            account = VocabeeAccountState.Authenticated(
                userId = event.userId,
                displayName = event.displayName,
                email = event.email,
            ),
            userLanguage = userLanguage,
            learningLanguage = learningLanguage,
            notificationsEnabled = event.notificationsEnabled,
            darkThemeEnabled = event.darkThemeEnabled,
            beeBalance = event.beeBalance.coerceAtLeast(0),
            topics = loadUserTopicsUseCase(),
        )
        // Той самий distinct id, що й у серверних подіях (users.id), тому
        // клієнтські та бекендові події зливаються в одну персону PostHog.
        analytics.identify(
            event.userId,
            mapOf(
                "email" to event.email,
                "name" to event.displayName,
                "speak_lang" to userLanguage.code,
                "learn_lang" to learningLanguage.code,
            ),
        )
    }

    fun signOutKeepLastUserState() {
        analytics.track("signed_out")
        analytics.reset()
        preferencesManager.currentUserId = null
        state = state.copy(
            account = VocabeeAccountState.Anonymous,
            topics = loadUserTopicsUseCase(),
        )
    }

    fun touchLocalRevision() {
        preferencesManager.localRevisionEpochMillis = preferencesManager.localRevisionEpochMillis + 1L
    }
}
