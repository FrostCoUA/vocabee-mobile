package com.vocabee.android.presentation

import com.vocabee.android.data.FakeVocabularyRepository
import com.vocabee.android.domain.manager.StaticUserSessionManager
import com.vocabee.android.domain.model.DictionaryTopic
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
    fun addWordSkipsDuplicateWords() {
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
                translation = "hi",
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

    private fun VocabeeStore.createTopicForTest(): DictionaryTopic {
        onEvent(
            VocabeeEvent.CreateTopic(
                title = "Test topic",
                coverIndex = 0,
            ),
        )
        return state.topics.last()
    }

    private fun VocabeeStore.topicForTest(id: String): DictionaryTopic {
        return state.topics.first { it.id == id }
    }
}
