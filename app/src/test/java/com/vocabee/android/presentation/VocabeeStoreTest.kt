package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.FakeVocabularyRepository
import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import com.vocabee.android.feature.vocabulary.domain.manager.StaticUserSessionManager
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabeeStoreTest {
    @Test
    fun createTopicUsesLearningToSpeakingLanguagePair() {
        val store = VocabeeStore()

        store.onEvent(
            VocabeeEvent.CreateTopic(
                title = "Нові слова",
                coverIndex = 3,
            ),
        )

        val topic = store.state.topics.last()
        assertEquals("Нові слова", topic.title)
        assertEquals(store.state.learningLanguage, topic.sourceLanguage)
        assertEquals(store.state.userLanguage, topic.targetLanguage)
    }

    @Test
    fun addWordSkipsDuplicateWordPairs() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "привіт",
            ),
        )
        val initialCount = store.topicForTest(topic.id).words.size

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "привіт",
            ),
        )

        val updatedTopic = store.topicForTest(topic.id)
        assertEquals(initialCount, updatedTopic.words.size)
    }

    @Test
    fun selectingSameSpeakingLanguageAdjustsLearningLanguage() {
        val store = VocabeeStore()
        val english = store.state.supportedLanguages.first { it.code == "en" }

        store.onEvent(VocabeeEvent.SelectSpeakingLanguage(english))

        assertEquals("en", store.state.userLanguage.code)
        assertFalse(store.state.learningLanguage.code == "en")
    }

    @Test
    fun topicsAreScopedByUserKey() {
        val repository = FakeVocabularyRepository()
        val firstUserStore = VocabeeStore(
            repository = repository,
            userSessionManager = StaticUserSessionManager("user-a"),
        )
        val secondUserStore = VocabeeStore(
            repository = repository,
            userSessionManager = StaticUserSessionManager("user-b"),
        )

        firstUserStore.onEvent(
            VocabeeEvent.CreateTopic(
                title = "User A topic",
                coverIndex = 0,
            ),
        )

        assertEquals(1, firstUserStore.state.topics.size)
        assertEquals("user-a", firstUserStore.state.topics.single().userKey)
        assertTrue(secondUserStore.state.topics.isEmpty())
    }

    @Test
    fun removeTopicDeletesDictionaryFromState() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        store.onEvent(VocabeeEvent.RemoveTopic(topic.id))

        assertTrue(store.state.topics.none { it.id == topic.id })
    }

    @Test
    fun addedWordsKeepLocalSyncMetadata() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "walk",
                translation = "ходити",
            ),
        )

        val word = store.topicForTest(topic.id).words.first()
        assertTrue(word.addedAtEpochMillis > 0L)
    }

    @Test
    fun contextGlossaryEnrichmentIsPersistedAndMarksLocalRevision() {
        val preferences = InMemoryPreferencesManager()
        val store = VocabeeStore(preferencesManager = preferences)
        val topic = store.createTopicForTest()
        store.onEvent(VocabeeEvent.AddWord(topic.id, "world", "світ"))
        val word = store.topicForTest(topic.id).words.single()
        val beforeRevision = preferences.localRevisionEpochMillis
        val glossary = ContextGlossary(
            sentence = "Hello world!",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryToken("Hello", "hello", 0, 5, "привіт"),
                ContextGlossaryToken("world", "world", 6, 11, "світ"),
            ),
        )

        store.updateWordEnrichment(
            topicId = topic.id,
            wordId = word.id,
            ipa = null,
            details = WordDetails(contextGlossary = glossary),
        )

        assertEquals(
            glossary,
            store.topicForTest(topic.id).words.single().details?.contextGlossary,
        )
        assertTrue(preferences.localRevisionEpochMillis > beforeRevision)
    }

    @Test
    fun wordKnowledgeProgressIsClampedBetweenZeroAndHundred() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()
        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "walk",
                translation = "ходити",
            ),
        )
        val word = store.topicForTest(topic.id).words.first()
        assertEquals(0, word.knowledgePercent)

        store.onEvent(VocabeeEvent.AdjustWordKnowledge(topic.id, word.id, -20))
        assertEquals(0, store.topicForTest(topic.id).words.first().knowledgePercent)

        repeat(6) {
            store.onEvent(VocabeeEvent.AdjustWordKnowledge(topic.id, word.id, 20))
        }
        assertEquals(100, store.topicForTest(topic.id).words.first().knowledgePercent)
    }

    @Test
    fun firstTwoDictionariesAreFreeAndNextOneCostsBees() {
        val prefs = InMemoryPreferencesManager().apply { beeBalance = 50 }
        val store = VocabeeStore(preferencesManager = prefs)
        store.authenticateForTest()

        store.createTopicForTest("One")
        store.createTopicForTest("Two")
        assertEquals(50, store.state.beeBalance)

        store.createTopicForTest("Three")
        assertEquals(40, store.state.beeBalance)
        assertEquals(3, store.state.topics.size)
    }

    @Test
    fun paidDictionaryCreationIsBlockedWhenBeeBalanceIsTooLow() {
        val prefs = InMemoryPreferencesManager().apply { beeBalance = 0 }
        val store = VocabeeStore(preferencesManager = prefs)
        store.authenticateForTest()

        store.createTopicForTest("One")
        store.createTopicForTest("Two")
        store.onEvent(VocabeeEvent.CreateTopic(title = "Blocked", coverIndex = 0))

        assertEquals(2, store.state.topics.size)
        assertEquals(0, store.state.beeBalance)
    }

    @Test
    fun translationSearchSpendsOneBee() {
        val prefs = InMemoryPreferencesManager().apply { beeBalance = 2 }
        val store = VocabeeStore(preferencesManager = prefs)
        store.authenticateForTest()

        assertTrue(store.spendTranslationBee())
        assertEquals(1, store.state.beeBalance)
        assertTrue(store.spendTranslationBee())
        assertEquals(0, store.state.beeBalance)
        assertFalse(store.spendTranslationBee())
    }

    @Test
    fun anonymousDictionaryLimitBlocksThirdDictionaryWithoutSpendingBees() {
        val prefs = InMemoryPreferencesManager().apply { beeBalance = 50 }
        val store = VocabeeStore(preferencesManager = prefs)

        store.createTopicForTest("One")
        store.createTopicForTest("Two")
        store.onEvent(VocabeeEvent.CreateTopic(title = "Blocked", coverIndex = 0))

        assertEquals(2, store.state.topics.size)
        assertEquals(50, store.state.beeBalance)
        assertTrue(store.anonymousDictionaryLimitReached())
    }

    @Test
    fun anonymousWordLimitBlocksSearchAndExtraWords() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        repeat(AnonymousFreeWordLimit) { index ->
            store.onEvent(
                VocabeeEvent.AddWord(
                    topicId = topic.id,
                    source = "word-$index",
                    translation = "translation-$index",
                ),
            )
        }

        assertEquals(AnonymousFreeWordLimit, store.topicForTest(topic.id).words.size)
        assertFalse(store.canSearchTranslation())
        assertFalse(store.canAddWordToDictionary())

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "extra",
                translation = "extra",
            ),
        )

        assertEquals(AnonymousFreeWordLimit, store.topicForTest(topic.id).words.size)
    }

    @Test
    fun contextPracticeSkipsAmbiguousSameSenseTranslations() {
        val details = WordDetails(
            senseIndex = 0,
            senses = listOf(
                WordSense(
                    definition = "a bright device or light used to signal",
                    examples = listOf("He used a flare to signal for help."),
                ),
            ),
        )
        val topic = testTopic(
            words = listOf(
                testWord(id = "flare-signal", source = "flare", translation = "сигнальна ракета", details = details),
                testWord(id = "flare-flash", source = "flare", translation = "спалах", details = details),
            ),
        )

        assertEquals(0, topic.contextPairCount())
    }

    @Test
    fun contextPracticeCountsSeparatedSenseTranslations() {
        val signalDetails = WordDetails(
            senseIndex = 0,
            senses = listOf(
                WordSense(
                    definition = "a bright device or light used to signal",
                    examples = listOf("He used a flare to signal for help."),
                ),
                WordSense(
                    definition = "a sudden burst of bright light",
                    examples = listOf("A flare lit the night sky for a moment."),
                ),
            ),
        )
        val flashDetails = signalDetails.copy(senseIndex = 1)
        val topic = testTopic(
            words = listOf(
                testWord(id = "flare-signal", source = "flare", translation = "сигнальна ракета", details = signalDetails),
                testWord(id = "flare-flash", source = "flare", translation = "спалах", details = flashDetails),
            ),
        )

        assertEquals(2, topic.contextPairCount())
    }

    @Test
    fun topicsAreGroupedWithWorkingLanguagePairFirst() {
        val store = VocabeeStore()
        store.authenticateForTest()
        store.createTopicInPairForTest(learnCode = "en", title = "Подорожі")
        store.createTopicInPairForTest(learnCode = "de", title = "Німецька база")
        store.createTopicInPairForTest(learnCode = "de", title = "Берлін · побут")
        store.createTopicInPairForTest(learnCode = "pl", title = "Польська")
        store.selectLearningLanguageForTest("en")

        val groups = groupTopicsByLanguagePair(
            topics = store.state.topics,
            learningLanguageCode = "en",
            userLanguageCode = "uk",
        )

        assertEquals(3, groups.size)
        // Робоча пара — перша й без заголовка.
        assertEquals(TopicLanguagePair("en", "uk"), groups[0].pair)
        assertTrue(groups[0].isWorkingPair)
        assertEquals(listOf("Подорожі"), groups[0].topics.map { it.title })
        // Решта — за спаданням кількості словників.
        assertFalse(groups[1].isWorkingPair)
        assertEquals(TopicLanguagePair("de", "uk"), groups[1].pair)
        assertEquals(listOf("Німецька база", "Берлін · побут"), groups[1].topics.map { it.title })
        assertEquals(TopicLanguagePair("pl", "uk"), groups[2].pair)
    }

    @Test
    fun changingLearningLanguageMovesItsGroupToTheTop() {
        val store = VocabeeStore()
        store.createTopicInPairForTest(learnCode = "en", title = "Подорожі")
        store.createTopicInPairForTest(learnCode = "de", title = "Німецька база")
        store.selectLearningLanguageForTest("de")

        val groups = groupTopicsByLanguagePair(
            topics = store.state.topics,
            learningLanguageCode = store.state.learningLanguage.code,
            userLanguageCode = store.state.userLanguage.code,
        )

        assertEquals(TopicLanguagePair("de", "uk"), groups.first().pair)
        assertTrue(groups.first().isWorkingPair)
        assertEquals(listOf("Німецька база"), groups.first().topics.map { it.title })
        assertEquals(TopicLanguagePair("en", "uk"), groups[1].pair)
    }

    @Test
    fun groupingIsStableWhenPairsHaveEqualTopicCounts() {
        val store = VocabeeStore()
        store.createTopicInPairForTest(learnCode = "pl", title = "Польська")
        store.createTopicInPairForTest(learnCode = "de", title = "Німецька")
        store.selectLearningLanguageForTest("en")

        val groups = groupTopicsByLanguagePair(
            topics = store.state.topics,
            learningLanguageCode = "en",
            userLanguageCode = "uk",
        )

        // Робочої пари немає жодного словника → групи лише «інші», за кодом мови.
        assertEquals(listOf("de", "pl"), groups.map { it.pair.sourceCode })
        assertTrue(groups.none { it.isWorkingPair })
    }

    @Test
    fun learningLanguageCountsOnlyCountPairsWithTheNativeLanguage() {
        val store = VocabeeStore()
        store.authenticateForTest()
        store.createTopicInPairForTest(learnCode = "en", title = "Подорожі")
        store.createTopicInPairForTest(learnCode = "en", title = "Робота")
        store.createTopicInPairForTest(learnCode = "de", title = "Німецька база")

        val counts = learningLanguageTopicCounts(
            topics = store.state.topics,
            userLanguageCode = "uk",
        )

        assertEquals(2, counts["en"])
        assertEquals(1, counts["de"])
        // Мови без словників у парі з рідною — неактивні в шиті «Я вивчаю».
        assertEquals(null, counts["pl"])
        assertEquals(
            emptyMap<String, Int>(),
            learningLanguageTopicCounts(topics = store.state.topics, userLanguageCode = "pl"),
        )
    }

    @Test
    fun updateTopicAppearanceRenamesAndRecolorsWithoutSpendingBees() {
        val store = VocabeeStore()
        store.authenticateForTest()
        val topic = store.createTopicForTest(title = "Стара назва")
        val balanceBefore = store.state.beeBalance

        store.onEvent(
            VocabeeEvent.UpdateTopicAppearance(
                topicId = topic.id,
                title = "  Нова назва  ",
                coverIndex = 5,
                iconIndex = 3,
            ),
        )

        val updated = store.topicForTest(topic.id)
        assertEquals("Нова назва", updated.title)
        assertEquals(5, updated.coverIndex)
        assertEquals(3, updated.iconIndex)
        assertEquals(balanceBefore, store.state.beeBalance)
        // Пара мов редагуванням не змінюється (D6).
        assertEquals(topic.sourceLanguage, updated.sourceLanguage)
        assertEquals(topic.targetLanguage, updated.targetLanguage)
    }

    @Test
    fun updateTopicAppearanceIgnoresBlankTitleAndUnknownTopic() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest(title = "Назва")

        store.onEvent(
            VocabeeEvent.UpdateTopicAppearance(topic.id, title = "   ", coverIndex = 2, iconIndex = 2),
        )
        store.onEvent(
            VocabeeEvent.UpdateTopicAppearance("no-such-topic", title = "X", coverIndex = 2, iconIndex = 2),
        )

        val untouched = store.topicForTest(topic.id)
        assertEquals("Назва", untouched.title)
        assertEquals(0, untouched.coverIndex)
        assertEquals(1, store.state.topics.size)
    }

    @Test
    fun clearTopicWordsEmptiesDictionaryButKeepsItAndDoesNotRefundBees() {
        val prefs = InMemoryPreferencesManager().apply { beeBalance = 50 }
        val store = VocabeeStore(preferencesManager = prefs)
        store.authenticateForTest()
        val topic = store.createTopicForTest(title = "Подорожі")
        val other = store.createTopicForTest(title = "Робота")
        store.onEvent(VocabeeEvent.AddWord(topic.id, "trip", "подорож"))
        store.onEvent(VocabeeEvent.AddWord(topic.id, "flight", "рейс"))
        store.onEvent(VocabeeEvent.AddWord(other.id, "job", "робота"))
        val balanceBefore = store.state.beeBalance
        val revisionBefore = prefs.localRevisionEpochMillis

        store.onEvent(VocabeeEvent.ClearTopicWords(topic.id))

        val cleared = store.topicForTest(topic.id)
        assertTrue(cleared.words.isEmpty())
        assertEquals("Подорожі", cleared.title)
        assertEquals(2, store.state.topics.size)
        // Сусідні словники не зачеплені.
        assertEquals(1, store.topicForTest(other.id).words.size)
        // Монетки за видалення не повертаються (D3), але зміну треба синхронізувати.
        assertEquals(balanceBefore, store.state.beeBalance)
        assertTrue(prefs.localRevisionEpochMillis > revisionBefore)
    }

    @Test
    fun clearTopicWordsIsANoOpForEmptyOrUnknownDictionary() {
        val prefs = InMemoryPreferencesManager()
        val store = VocabeeStore(preferencesManager = prefs)
        val topic = store.createTopicForTest(title = "Порожній")
        val revisionBefore = prefs.localRevisionEpochMillis

        store.onEvent(VocabeeEvent.ClearTopicWords(topic.id))
        store.onEvent(VocabeeEvent.ClearTopicWords("no-such-topic"))
        store.onEvent(VocabeeEvent.ClearTopicWords(""))

        assertEquals(1, store.state.topics.size)
        assertTrue(store.topicForTest(topic.id).words.isEmpty())
        assertEquals(revisionBefore, prefs.localRevisionEpochMillis)
    }

    @Test
    fun clearButtonUnlocksOnlyForTheExactControlPhrase() {
        assertTrue(isClearDictionaryConfirmed(ClearDictionaryConfirmationPhrase))
        // Пробіли по краях прощаємо — їх додає автопідстановка клавіатури.
        assertTrue(isClearDictionaryConfirmed("  $ClearDictionaryConfirmationPhrase  "))
        // Регістр і часткове введення — ні.
        assertFalse(isClearDictionaryConfirmed(""))
        assertFalse(isClearDictionaryConfirmed("очистити"))
        assertFalse(isClearDictionaryConfirmed("Очистити"))
        assertFalse(isClearDictionaryConfirmed("ОЧИСТ"))
        assertFalse(isClearDictionaryConfirmed("ОЧИСТИТИ ВСЕ"))
    }

    @Test
    fun profileAvatarInitialsComeFromTheAccountNotFromAHardcodedLiteral() {
        assertEquals("НК", profileInitials("Надія Кобилінська", "nadiia@vocabee.app"))
        // Одне слово — одна літера; зайві частини імені ігноруються.
        assertEquals("Н", profileInitials("надія", "nadiia@vocabee.app"))
        // Більше двох слів — беремо лише перші дві літери.
        assertEquals("НМ", profileInitials("Надія Марія Кобилінська", "n@vocabee.app"))
        // Порожнє імʼя → перша літера пошти, порожні дані → «V».
        assertEquals("N", profileInitials("   ", "nadiia@vocabee.app"))
        assertEquals("V", profileInitials("", ""))
    }

    /** Створює словник у парі «learnCode → рідна», не змінюючи підсумкову мову вивчення. */
    private fun VocabeeStore.createTopicInPairForTest(learnCode: String, title: String) {
        selectLearningLanguageForTest(learnCode)
        createTopicForTest(title)
    }

    private fun VocabeeStore.selectLearningLanguageForTest(code: String) {
        onEvent(
            VocabeeEvent.SelectLearningLanguage(
                state.supportedLanguages.first { it.code == code },
            ),
        )
    }

    private fun VocabeeStore.authenticateForTest() {
        onEvent(
            VocabeeEvent.ApplyAuthenticatedAccount(
                userId = "test-user",
                displayName = "Test User",
                email = "test@example.com",
                speakLang = state.userLanguage.code,
                learnLang = state.learningLanguage.code,
                notificationsEnabled = true,
                darkThemeEnabled = state.darkThemeEnabled,
                beeBalance = state.beeBalance,
            ),
        )
    }

    private fun VocabeeStore.createTopicForTest(title: String = "Test topic"): DictionaryTopic {
        onEvent(
            VocabeeEvent.CreateTopic(
                title = title,
                coverIndex = 0,
            ),
        )
        return state.topics.last()
    }

    private fun VocabeeStore.topicForTest(id: String): DictionaryTopic {
        return state.topics.first { it.id == id }
    }

    private fun testTopic(words: List<WordEntry>): DictionaryTopic {
        val english = LanguageOption(code = "en", name = "English", shortName = "EN", speechTag = "en-US")
        val ukrainian = LanguageOption(code = "uk", name = "Українська", shortName = "UK", speechTag = "uk-UA")
        return DictionaryTopic(
            id = "topic",
            title = "Test",
            sourceLanguage = english,
            targetLanguage = ukrainian,
            words = words,
        )
    }

    private fun testWord(
        id: String,
        source: String,
        translation: String,
        details: WordDetails,
    ): WordEntry = WordEntry(
        id = id,
        source = source,
        translation = translation,
        details = details,
    )
}
