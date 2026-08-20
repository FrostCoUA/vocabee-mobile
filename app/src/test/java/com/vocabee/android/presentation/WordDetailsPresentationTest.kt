package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Test

class WordDetailsPresentationTest {
    private val signalSense = WordSense(
        senseKey = "sense_signal",
        definition = "a device used to signal",
        synonyms = listOf("signal light"),
        antonyms = listOf("concealment"),
    )
    private val burstSense = WordSense(
        senseKey = "sense_burst",
        definition = "a sudden burst of light",
        synonyms = listOf("burst", "flash"),
        antonyms = listOf("fade"),
    )

    @Test
    fun attributedTranslationShowsOnlyItsOwnSenseAndRelations() {
        val details = WordDetails(
            senseIndex = 1,
            senses = listOf(signalSense, burstSense),
            synonyms = listOf("signal light", "burst", "flash"),
            antonyms = listOf("concealment", "fade"),
        )

        val content = details.displayContent()

        assertEquals(listOf(1), content.senses.map { it.index })
        assertEquals(listOf(burstSense), content.senses.map { it.value })
        assertEquals(listOf("burst", "flash"), content.synonyms)
        assertEquals(listOf("fade"), content.antonyms)
    }

    @Test
    fun v2TranslationShowsEveryLinkedSenseAndOnlyTheirRelations() {
        val details = WordDetails(
            senseKeys = listOf("sense_signal", "sense_burst"),
            senseIndex = 0,
            senses = listOf(signalSense, burstSense),
            synonyms = listOf("signal light", "burst", "flash"),
            antonyms = listOf("concealment", "fade"),
        )

        val content = details.displayContent()

        assertEquals(listOf(0, 1), content.senses.map { it.index })
        assertEquals(listOf(signalSense, burstSense), content.senses.map { it.value })
        assertEquals(listOf("signal light", "burst", "flash"), content.synonyms)
        assertEquals(listOf("concealment", "fade"), content.antonyms)
    }

    @Test
    fun unattributedTranslationKeepsTheLegacyAllSensesFallback() {
        val details = WordDetails(
            senses = listOf(signalSense, burstSense),
            synonyms = listOf("signal light", "burst", "flash"),
            antonyms = listOf("concealment", "fade"),
        )

        val content = details.displayContent()

        assertEquals(listOf(0, 1), content.senses.map { it.index })
        assertEquals(listOf(signalSense, burstSense), content.senses.map { it.value })
        assertEquals(details.synonyms, content.synonyms)
        assertEquals(details.antonyms, content.antonyms)
    }

    @Test
    fun groupedCardKeepsAllSensesForItsMultipleTranslations() {
        val details = WordDetails(
            senseIndex = 1,
            senses = listOf(signalSense, burstSense),
            synonyms = listOf("signal light", "burst", "flash"),
            antonyms = listOf("concealment", "fade"),
        )

        val content = details.displayContent(scopeToAttributedSense = false)

        assertEquals(listOf(0, 1), content.senses.map { it.index })
        assertEquals(details.synonyms, content.synonyms)
        assertEquals(details.antonyms, content.antonyms)
    }

    @Test
    fun attributedSenseWithoutOwnRelationsFallsBackToTheWholeWordSet() {
        // The gateway keeps whole-word relations separate from sense-scoped ones
        // and unions them into the top-level lists. A provider that returned only
        // whole-word synonyms leaves every sense empty, and scoping must not turn
        // that into an empty chip row.
        val details = WordDetails(
            senseKeys = listOf("sense_signal"),
            senses = listOf(
                WordSense(senseKey = "sense_signal", definition = "a device used to signal"),
                WordSense(senseKey = "sense_burst", definition = "a sudden burst of light"),
            ),
            synonyms = listOf("beacon", "flare"),
            antonyms = listOf("concealment"),
        )

        val content = details.displayContent()

        assertEquals(listOf(0), content.senses.map { it.index })
        assertEquals(listOf("beacon", "flare"), content.synonyms)
        assertEquals(listOf("concealment"), content.antonyms)
    }

    // Підпис сенсу в рядку пошуку: лише АТРИБУТОВАНИЙ сенс, інакше нічого —
    // перший сенс легасі-блоба не має стосунку до конкретного перекладу.
    @Test
    fun firstSenseLineTakesTheAttributedSenseOrNothing() {
        val details = WordDetails(
            senseKeys = listOf("sense_burst"),
            senses = listOf(signalSense, burstSense),
        )

        assertEquals("a sudden burst of light", details.firstSenseLine())
        assertEquals(null, WordDetails(senses = listOf(signalSense, burstSense)).firstSenseLine())
        assertEquals(
            "The sky flashed.",
            WordDetails(
                senseIndex = 0,
                senses = listOf(WordSense(definition = "  ", examples = listOf("The sky flashed."))),
            ).firstSenseLine(),
        )
    }
}
