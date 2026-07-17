package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
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
}
