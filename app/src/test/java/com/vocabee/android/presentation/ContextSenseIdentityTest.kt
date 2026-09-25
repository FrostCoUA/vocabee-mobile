package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryResponse
import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryTokenResponse
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.usecase.toValidatedGlossary
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextSenseIdentityTest {
    @Test
    fun contextWordKeepsIdentityAfterSaveAndAnotherSameTextMeaningStaysSeparate() {
        val glossary = requireNotNull(ContextGlossaryResponse(
            sentence = "I work from home.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(ContextGlossaryTokenResponse(
                surface = "work", normalized = "work", start = 2, endExclusive = 6,
                translation = "працювати", lemma = "work",
                translationId = "translation-work-verb", senseKey = "sense-work-verb",
            )),
        ).toValidatedGlossary())
        val token = glossary.tokens.single()
        val bookmark = practiceBookmark(glossary, token, "origin")
        val store = VocabeeStore()
        store.onEvent(VocabeeEvent.CreateTopic("Праця", coverIndex = 0))
        val topicId = store.state.topics.last().id

        store.onEvent(VocabeeEvent.AddWord(
            topicId, bookmark.source, bookmark.translation, details = bookmark.toWordDetails(),
        ))
        val saved = store.state.topics.first { it.id == topicId }.words.single()
        assertEquals("translation-work-verb", saved.details?.translationId)
        assertEquals(listOf("sense-work-verb"), saved.details?.senseKeys)
        assertEquals(glossary.sentence, saved.contextSentence())

        val sameMeaning = TranslationOption(
            value = "працювати", note = TranslationOptionNote.Primary,
            learningWord = "work", translationId = "translation-work-verb",
            details = WordDetails(senseKeys = listOf("sense-work-verb")),
        )
        val otherMeaning = sameMeaning.copy(
            translationId = "translation-work-noun",
            details = WordDetails(senseKeys = listOf("sense-work-noun")),
        )
        assertEquals(saved.id, sameMeaning.savedWordIn(listOf(saved))?.id)
        assertNull(otherMeaning.savedWordIn(listOf(saved)))
        assertFalse(contextBookmarkKey(glossary, token) == contextBookmarkKey(
            glossary, token.copy(translationId = "translation-work-noun", senseKey = "sense-work-noun"),
        ))

        store.onEvent(VocabeeEvent.AddWord(
            topicId, "work", "працювати",
            details = WordDetails(
                translationId = "translation-work-noun",
                senseKeys = listOf("sense-work-noun"),
            ),
        ))
        val both = store.state.topics.first { it.id == topicId }.words
        assertEquals(2, both.size)
        assertNotNull(otherMeaning.savedWordIn(both))
        store.onEvent(VocabeeEvent.RemoveWord(topicId, saved.source, saved.translation, saved.id))
        val remaining = store.state.topics.first { it.id == topicId }.words
        assertEquals(1, remaining.size)
        assertNull(sameMeaning.savedWordIn(remaining))
        assertNotNull(otherMeaning.savedWordIn(remaining))
    }

    @Test
    fun legacyContextTokenJsonStillDecodesWithoutIds() {
        val oldJson = """{"surface":"work","normalized":"work","start":2,"endExclusive":6,"translation":"працювати"}"""
        val token = Json.decodeFromString<ContextGlossaryToken>(oldJson)
        assertNull(token.translationId)
        assertNull(token.senseKey)
        assertTrue(token.translation.isNotBlank())
    }

    @Test
    fun legacyPairIsShownAsAlreadySavedWhenIdentifiedCandidateCannotBeInserted() {
        val legacy = WordEntry(id = "legacy", source = "work", translation = "працювати")
        val candidate = TranslationOption(
            value = "працювати", note = TranslationOptionNote.Primary,
            learningWord = "work", translationId = "translation-work-verb",
            details = WordDetails(senseKeys = listOf("sense-work-verb")),
        )

        assertEquals(legacy.id, candidate.savedWordIn(listOf(legacy))?.id)
        assertNull(candidate.copy(value = "робота").savedWordIn(listOf(legacy)))
    }
}
