package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
import com.vocabee.android.feature.vocabulary.data.api.SearchExample
import com.vocabee.android.feature.vocabulary.data.api.SearchForm
import com.vocabee.android.feature.vocabulary.data.api.SearchSense
import com.vocabee.android.feature.vocabulary.domain.model.LexicalRegisterTag
import com.vocabee.android.feature.vocabulary.domain.model.LexicalUnitKind
import com.vocabee.android.feature.vocabulary.domain.usecase.toOption
import org.junit.Assert.assertEquals
import org.junit.Test

class LexicalUnitMappingTest {
    @Test
    fun mapsAbbreviationAndSlangMetadataIntoSavedWordDetails() {
        val option = SearchVariant(
            knownWord = "лол",
            learningWord = "LOL",
            lexicalUnitKind = "abbreviation",
            registerTags = listOf("slang", "internet"),
            expansion = "laughing out loud",
            meaning = "реакція на щось дуже смішне",
            literalTranslation = "сміючись уголос",
            usageExample = "LOL, that was hilarious!",
            usageExampleTranslation = "Лол, це було дуже смішно!",
            source = "ai",
            origin = "test",
            isPrimary = true,
            cached = false,
        ).toOption(emptySet())

        val details = requireNotNull(option.details)
        assertEquals(LexicalUnitKind.Abbreviation, details.lexicalUnitKind)
        assertEquals(
            listOf(LexicalRegisterTag.Slang, LexicalRegisterTag.Internet),
            details.registerTags,
        )
        assertEquals("laughing out loud", details.expansion)
        assertEquals("реакція на щось дуже смішне", details.meaning)
        assertEquals("сміючись уголос", details.literalTranslation)
        assertEquals("LOL, that was hilarious!", details.usageExample)
    }

    @Test
    fun mapsPersistedPrefixSuggestionIntoNonNullWordDetailsSnapshot() {
        val senseKey = "sense_circumstance_noun_situation"
        val option = SearchVariant(
            translationId = "translation-circumstance",
            knownWord = "обставина",
            learningWord = "circumstance",
            ipa = "/ˈsɜːkəmstəns/",
            partOfSpeech = listOf("noun"),
            senses = listOf(
                SearchSense(
                    senseKey = senseKey,
                    definition = "a fact or condition connected with an event",
                    partOfSpeech = "noun",
                    examples = listOf(
                        SearchExample("We met under unusual circumstances."),
                    ),
                    synonyms = listOf("condition", "situation"),
                    antonyms = listOf("certainty"),
                ),
            ),
            synonyms = listOf("condition"),
            antonyms = listOf("certainty"),
            forms = listOf(
                SearchForm("circumstances", listOf("plural")),
            ),
            senseKeys = listOf(senseKey),
            source = "seed",
            origin = "vocabee-translate/en_uk/primary",
            isPrimary = true,
            cached = true,
        ).toOption(emptySet())

        assertEquals("circumstance", option.learningWord)
        assertEquals("/ˈsɜːkəmstəns/", option.ipa)
        val details = requireNotNull(option.details)
        assertEquals("translation-circumstance", details.translationId)
        assertEquals(listOf(senseKey), details.senseKeys)
        assertEquals(listOf("noun"), details.partOfSpeech)
        assertEquals(
            listOf("We met under unusual circumstances."),
            details.senses.single().examples,
        )
        assertEquals(listOf("condition", "situation"), details.senses.single().synonyms)
        assertEquals("circumstances", details.forms.single().text)
        assertEquals(listOf("plural"), details.forms.single().tags)
    }

    @Test
    fun keepsTranslationIdentityWhenSearchResultHasNoVisibleEnrichment() {
        val option = SearchVariant(
            translationId = "translation-obvious",
            knownWord = "очевидний",
            learningWord = "obvious",
            source = "seed",
            origin = "vocabee-translate/en_uk/primary",
            isPrimary = true,
            cached = true,
        ).toOption(emptySet())

        val details = requireNotNull(option.details)
        assertEquals("translation-obvious", details.translationId)
        assertEquals(true, details.isEmpty)
        assertEquals(true, details.shouldPersist)
    }

    @Test
    fun keepsRepeatedExamplesExactlyAsReturnedByServer() {
        val repeated = "The answer seems obvious now."
        val option = SearchVariant(
            translationId = "translation-obvious",
            knownWord = "очевидний",
            learningWord = "obvious",
            senses = listOf(
                SearchSense(
                    definition = "Easy to see, understand, or recognize.",
                    examples = listOf(SearchExample(repeated), SearchExample(repeated)),
                ),
            ),
            source = "seed",
            origin = "vocabee-translate/en_uk/primary",
            isPrimary = true,
            cached = true,
        ).toOption(emptySet())

        assertEquals(listOf(repeated, repeated), requireNotNull(option.details).senses.single().examples)
    }
}
