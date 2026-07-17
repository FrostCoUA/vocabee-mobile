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
