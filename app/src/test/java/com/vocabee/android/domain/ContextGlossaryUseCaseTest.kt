package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryResponse
import com.vocabee.android.feature.vocabulary.data.api.ContextGlossaryTokenResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextGlossaryUseCaseTest {
    @Test
    fun mapsValidatedUtf16TokenRangesIntoOfflineGlossary() {
        val response = ContextGlossaryResponse(
            sentence = "I saw a bat.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryTokenResponse("I", "i", 0, 1, "я"),
                ContextGlossaryTokenResponse("saw", "saw", 2, 5, "побачив", "see"),
                ContextGlossaryTokenResponse("a", "a", 6, 7, "неозначений артикль"),
                ContextGlossaryTokenResponse("bat", "bat", 8, 11, "кажана", "bat"),
            ),
        )

        val glossary = requireNotNull(response.toValidatedGlossary())

        assertEquals(response.sentence, glossary.sentence)
        assertEquals("побачив", glossary.tokens[1].translation)
        assertEquals("see", glossary.tokens[1].lemma)
    }

    @Test
    fun rejectsAResponseWhoseOffsetsDoNotMatchTheExactSentence() {
        val response = ContextGlossaryResponse(
            sentence = "Hello world",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryTokenResponse("world", "world", 5, 10, "світ"),
            ),
        )

        assertNull(response.toValidatedGlossary())
    }
}
