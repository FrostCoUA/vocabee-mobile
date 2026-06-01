package com.vocabee.android.presentation

import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.manager.StaticUserSessionManager
import com.vocabee.android.data.FakeVocabularyRepository
import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.platform.MachineTranslationProvider
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
            )
        )

        val topic = store.state.topics.last()
        assertEquals("Нові слова", topic.title)
        assertEquals(store.state.learningLanguage, topic.sourceLanguage)
        assertEquals(store.state.userLanguage, topic.targetLanguage)
    }

    @Test
    fun addWordSkipsDuplicateWords() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "привіт",
            )
        )
        val initialCount = store.topicForTest(topic.id).words.size

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "hi",
            )
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
    fun translationOptionsReturnExistingWord() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()
        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "привіт",
            )
        )

        val options = store.translationOptionsFor(store.topicForTest(topic.id), "hello")

        assertTrue(options.first().alreadyAdded)
        assertEquals("привіт", options.first().value)
    }

    @Test
    fun machineTranslationIsMergedAfterRequest() {
        val store = VocabeeStore(
            machineTranslationProvider = ImmediateMachineTranslationProvider("machine translated"),
        )
        val topic = store.createTopicForTest()

        store.onEvent(
            VocabeeEvent.RequestMachineTranslation(
                topicId = topic.id,
                input = "new word",
            )
        )

        val options = store.translationOptionsFor(topic, "new word")
        assertEquals("machine translated", options.first().value)
        assertEquals(TranslationOptionNote.MlKitOnDevice, options.first().note)
    }

    @Test
    fun builtInPrototypeWordsAreAvailableForEnglishToUkrainianTopics() {
        val store = VocabeeStore()
        val topic = store.createTopicForTest()

        val options = store.translationOptionsFor(topic, "resilience")

        assertEquals("стійкість", options.first().value)
        assertEquals(TranslationOptionNote.Primary, options.first().note)
    }

    @Test
    fun existingWordStaysFirstWhenMachineTranslationReturns() {
        val store = VocabeeStore(
            machineTranslationProvider = ImmediateMachineTranslationProvider("ml hello"),
        )
        val topic = store.createTopicForTest()
        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "hello",
                translation = "привіт",
            )
        )

        store.onEvent(
            VocabeeEvent.RequestMachineTranslation(
                topicId = topic.id,
                input = "hello",
            )
        )

        val options = store.translationOptionsFor(store.topicForTest(topic.id), "hello")
        assertTrue(options.first().alreadyAdded)
        assertEquals("привіт", options.first().value)
        assertEquals("ml hello", options[1].value)
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
            )
        )

        assertEquals(1, firstUserStore.state.topics.size)
        assertEquals("user-a", firstUserStore.state.topics.single().userKey)
        assertTrue(secondUserStore.state.topics.isEmpty())
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
            )
        )

        val word = store.topicForTest(topic.id).words.first()
        assertTrue(word.addedAtEpochMillis > 0L)
    }

    private fun VocabeeStore.createTopicForTest(): DictionaryTopic {
        onEvent(
            VocabeeEvent.CreateTopic(
                title = "Test topic",
                coverIndex = 0,
            )
        )
        return state.topics.last()
    }

    private fun VocabeeStore.topicForTest(id: String): DictionaryTopic {
        return state.topics.first { it.id == id }
    }

    private class ImmediateMachineTranslationProvider(
        private val translatedText: String,
    ) : MachineTranslationProvider {
        override val isSupported: Boolean = true

        override fun translate(
            sourceLanguageCode: String,
            targetLanguageCode: String,
            text: String,
            onSuccess: (String) -> Unit,
            onError: (String) -> Unit,
        ) {
            onSuccess(translatedText)
        }

        override fun close() = Unit
    }
}
