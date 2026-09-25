package com.vocabee.android.data

import com.vocabee.android.feature.vocabulary.data.WordDetailsJsonCodec
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.WordForm
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDetailsJsonCodecTest {
    @Test
    fun codecUsedByRoomRepositoryRoundTripsTheCompleteSearchSnapshot() {
        val details = WordDetails(
            translationId = "translation-circumstance",
            lexiconSchemaVersion = 1,
            lexiconRevision = "sha256:revision-1",
            senseKeys = listOf("sense_circumstance_noun_situation"),
            senses = listOf(
                WordSense(
                    senseKey = "sense_circumstance_noun_situation",
                    definition = "a fact or condition connected with an event",
                    partOfSpeech = "noun",
                    examples = listOf("We met under unusual circumstances."),
                    synonyms = listOf("condition", "situation"),
                    antonyms = listOf("certainty"),
                ),
            ),
            synonyms = listOf("condition"),
            antonyms = listOf("certainty"),
            forms = listOf(WordForm("circumstances", listOf("plural"))),
            partOfSpeech = listOf("noun"),
        )

        val detailsJson = WordDetailsJsonCodec.encode(details)
        val restored = WordDetailsJsonCodec.decode(detailsJson)

        assertEquals(details, restored)
        assertTrue(detailsJson.contains("senseKeys"))
        assertTrue(detailsJson.contains("examples"))
        assertTrue(detailsJson.contains("forms"))
        assertTrue(detailsJson.contains("translationId"))
        assertTrue(detailsJson.contains("lexiconSchemaVersion"))
        assertTrue(detailsJson.contains("lexiconRevision"))
    }

    @Test
    fun controlOnlySnapshotRemainsPersistableWithoutBecomingVisibleDetails() {
        val details = WordDetails(
            translationId = "translation-obvious",
            lexiconSchemaVersion = 1,
            lexiconRevision = "sha256:obvious-v2",
        )

        val restored = WordDetailsJsonCodec.decode(WordDetailsJsonCodec.encode(details))

        assertTrue(details.isEmpty)
        assertTrue(details.shouldPersist)
        assertEquals(details, restored)
    }

    @Test
    fun contextTokenSenseIdentitySurvivesRoomSnapshot() {
        val details = WordDetails(
            translationId = "translation-work-verb",
            senseKeys = listOf("sense-work-verb"),
            usageExample = "I work from home.",
            contextGlossary = ContextGlossary(
                sentence = "I work from home.", sourceLang = "en", targetLang = "uk",
                tokens = listOf(ContextGlossaryToken(
                    "work", "work", 2, 6, "працювати", "work",
                    translationId = "translation-work-verb", senseKey = "sense-work-verb",
                )),
            ),
        )

        assertEquals(details, WordDetailsJsonCodec.decode(WordDetailsJsonCodec.encode(details)))
    }
}
