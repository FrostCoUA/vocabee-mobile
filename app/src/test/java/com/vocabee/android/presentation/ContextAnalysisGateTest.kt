package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextAnalysisGateTest {
    private val request = ContextAnalysisRequest("topic", "word", "I run every morning.")

    @Test
    fun cancelDoesNotStartAnalysis() {
        val gate = ContextAnalysisGate()
        assertTrue(gate.ask(request, alreadyReady = false))
        gate.cancel()

        assertNull(gate.confirm())
        assertFalse(gate.isRunning("topic", "word"))
    }

    @Test
    fun confirmStartsExactlyOneRequestAndFailureCanBeRetried() {
        val gate = ContextAnalysisGate()
        assertTrue(gate.ask(request, alreadyReady = false))
        assertEquals(request, gate.confirm())
        assertNull(gate.confirm())
        assertTrue(gate.isRunning("topic", "word"))
        assertFalse(gate.ask(request, alreadyReady = false))

        gate.finish(request)
        assertTrue(gate.ask(request, alreadyReady = false))
        assertEquals(request, gate.confirm())
    }

    @Test
    fun existingGlossaryNeverOpensConfirmation() {
        val glossary = ContextGlossary(
            sentence = request.sentence,
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(ContextGlossaryToken("run", "run", 2, 5, "бігати")),
        )
        val gate = ContextAnalysisGate()
        assertTrue(WordDetails(contextGlossary = glossary).hasReadyContextGlossary(request.sentence))
        assertFalse(gate.ask(request, alreadyReady = true))
        assertNull(gate.pending)
        assertNull(gate.confirm())
    }

    @Test
    fun anotherOwnedExampleCanBeSelectedWhileUnrelatedExampleCannot() {
        val selectedGlossary = ContextGlossary(
            sentence = "I run every morning.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(ContextGlossaryToken("run", "run", 2, 5, "бігаю")),
        )
        val details = WordDetails(
            senseKeys = listOf("running"),
            senses = listOf(
                WordSense(definition = "Move by foot", examples = listOf("I run.", "I run every morning."), senseKey = "running"),
                WordSense(definition = "Operate", examples = listOf("I run the program."), senseKey = "operate"),
            ),
            contextGlossary = selectedGlossary,
        )

        assertEquals(listOf("I run.", "I run every morning."), details.contextSentences())
        assertEquals("I run every morning.", details.contextSentence())
        assertFalse("I run the program." in details.contextSentences())
    }

    @Test
    fun confirmedOwnGlossaryRemainsAvailableAfterFullSenseEnrichment() {
        val details = WordDetails(
            translationId = "translation-run-move",
            senseKeys = listOf("sense-run-move"),
            senses = listOf(
                WordSense(senseKey = "sense-run-move", definition = "Move on foot", examples = listOf("He runs fast.")),
                WordSense(senseKey = "sense-run-operate", definition = "Operate", examples = listOf("I run the program.")),
            ),
            contextGlossary = ContextGlossary(
                sentence = "I run every morning.", sourceLang = "en", targetLang = "uk",
                tokens = listOf(ContextGlossaryToken(
                    "run", "run", 2, 5, "бігати",
                    translationId = "translation-run-move", senseKey = "sense-run-move",
                )),
            ),
        )

        assertEquals(listOf("He runs fast.", "I run every morning."), details.contextSentences())
        assertEquals("I run every morning.", details.contextSentence())
    }
}
