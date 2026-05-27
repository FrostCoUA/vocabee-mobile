package com.vocabee.android.presentation

import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.platform.MachineTranslationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabeeStoreTest {
    @Test
    fun createTopicUsesCurrentLanguagePair() {
        val store = VocabeeStore()

        store.onEvent(
            VocabeeEvent.CreateTopic(
                title = "Нові слова",
                coverIndex = 3,
            )
        )

        val topic = store.state.topics.last()
        assertEquals("Нові слова", topic.title)
        assertEquals(store.state.userLanguage, topic.sourceLanguage)
        assertEquals(store.state.learningLanguage, topic.targetLanguage)
    }

    @Test
    fun addWordSkipsDuplicateWords() {
        val store = VocabeeStore()
        val topic = store.state.topics.first { it.id == "topic-travel" }
        val initialCount = topic.words.size

        store.onEvent(
            VocabeeEvent.AddWord(
                topicId = topic.id,
                source = "багаж",
                translation = "baggage",
            )
        )

        val updatedTopic = store.state.topics.first { it.id == topic.id }
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
    fun translationOptionsPutExistingWordFirst() {
        val store = VocabeeStore()
        val cookingTopic = store.state.topics.first { it.id == "topic-cooking" }

        val options = store.translationOptionsFor(cookingTopic, "recipe")

        assertTrue(options.first().alreadyAdded)
        assertEquals("recipe", options.first().value)
    }

    @Test
    fun machineTranslationIsMergedAfterRequest() {
        val store = VocabeeStore(
            machineTranslationProvider = ImmediateMachineTranslationProvider("machine translated"),
        )
        val topic = store.state.topics.first { it.id == "topic-travel" }

        store.onEvent(
            VocabeeEvent.RequestMachineTranslation(
                topicId = topic.id,
                input = "нове слово",
            )
        )

        val options = store.translationOptionsFor(topic, "нове слово")
        assertEquals("machine translated", options.first().value)
        assertEquals(TranslationOptionNote.MlKitOnDevice, options.first().note)
    }

    @Test
    fun existingWordStaysFirstWhenMachineTranslationReturns() {
        val store = VocabeeStore(
            machineTranslationProvider = ImmediateMachineTranslationProvider("ml recipe"),
        )
        val topic = store.state.topics.first { it.id == "topic-cooking" }

        store.onEvent(
            VocabeeEvent.RequestMachineTranslation(
                topicId = topic.id,
                input = "recipe",
            )
        )

        val options = store.translationOptionsFor(topic, "recipe")
        assertTrue(options.first().alreadyAdded)
        assertEquals("recipe", options.first().value)
        assertEquals("ml recipe", options[1].value)
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
