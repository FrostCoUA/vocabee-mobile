package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.domain.model.LexicalUnitKind
import com.vocabee.android.feature.vocabulary.presentation.navigation.VocabeeRoute
import com.vocabee.android.feature.vocabulary.presentation.navigation.shouldShowBottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeUiPolicyTest {
    @Test
    fun practiceSetupKeepsBottomBarVisible() {
        assertTrue(shouldShowBottomBar(VocabeeRoute.Practice, practiceSessionActive = false))
    }

    @Test
    fun activePracticeHidesBottomBar() {
        assertFalse(shouldShowBottomBar(VocabeeRoute.Practice, practiceSessionActive = true))
    }

    @Test
    fun practiceSessionStateDoesNotHideOtherRootTabs() {
        assertTrue(shouldShowBottomBar(VocabeeRoute.DictionaryHome, practiceSessionActive = true))
        assertTrue(shouldShowBottomBar(VocabeeRoute.Settings, practiceSessionActive = true))
    }

    @Test
    fun practiceCardUsesExampleFromItsAttributedSense() {
        val word = WordEntry(
            id = "bank-river",
            source = "bank",
            translation = "берег",
            details = WordDetails(
                senseIndex = 1,
                senses = listOf(
                    WordSense(
                        definition = "a financial institution",
                        examples = listOf("She deposited money at the bank."),
                    ),
                    WordSense(
                        definition = "the land beside a river",
                        examples = listOf("They sat on the river bank."),
                    ),
                ),
            ),
        )

        assertEquals("They sat on the river bank.", word.contextSentence())
    }

    @Test
    fun abbreviationUsesItsOwnUsageExampleWhenItHasNoDictionarySense() {
        val word = WordEntry(
            id = "lol",
            source = "LOL",
            translation = "лол",
            details = WordDetails(
                lexicalUnitKind = LexicalUnitKind.Abbreviation,
                usageExample = "LOL, that was hilarious!",
            ),
        )

        assertEquals("LOL, that was hilarious!", word.contextSentence())
    }

    @Test
    fun practiceUsesTheExactSentenceStoredWithItsContextGlossary() {
        val glossary = ContextGlossary(
            sentence = "I left my keys by the bank.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryToken("bank", "bank", 22, 26, "банк"),
            ),
        )
        val word = WordEntry(
            id = "bank",
            source = "bank",
            translation = "банк",
            details = WordDetails(
                usageExample = "A newer example must not replace the enriched sentence.",
                contextGlossary = glossary,
            ),
        )

        assertEquals(glossary.sentence, word.contextSentence())
    }

    @Test
    fun contextPopupHitTestingSelectsWordsButNotPunctuation() {
        val glossary = ContextGlossary(
            sentence = "Hello, world!",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryToken("Hello", "hello", 0, 5, "привіт"),
                ContextGlossaryToken("world", "world", 7, 12, "світ"),
            ),
        )

        assertEquals(0, contextTokenIndexAtOffset(glossary, 4))
        assertEquals(1, contextTokenIndexAtOffset(glossary, 7))
        assertNull(contextTokenIndexAtOffset(glossary, 5))
        assertNull(contextTokenIndexAtOffset(glossary, 12))
    }

    @Test
    fun aTapConsumedByTheContextSentenceNeverFlipsTheCard() {
        assertFalse(shouldFlipPracticeCardOnTap(downWasConsumedByChild = true))
        assertTrue(shouldFlipPracticeCardOnTap(downWasConsumedByChild = false))
    }

    @Test
    fun legacyContextSentenceRequestsGlossaryEnrichment() {
        val legacy = WordEntry(
            id = "stone",
            source = "stone",
            translation = "камінь",
            details = WordDetails(
                usageExample = "He threw a stone into the river.",
            ),
        )
        val enriched = legacy.copy(
            details = legacy.details?.copy(
                contextGlossary = ContextGlossary(
                    sentence = "He threw a stone into the river.",
                    sourceLang = "en",
                    targetLang = "uk",
                    tokens = listOf(
                        ContextGlossaryToken("stone", "stone", 11, 16, "камінь", "stone"),
                    ),
                ),
            ),
        )

        assertTrue(needsContextGlossaryEnrichment(legacy))
        assertFalse(needsContextGlossaryEnrichment(enriched))
    }

    @Test
    fun everyOccurrenceOfThePracticedWordIsExcludedFromHints() {
        val glossary = ContextGlossary(
            sentence = "A bank differs from other banks near the bank and a banker.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryToken("A", "a", 0, 1, "артикль"),
                ContextGlossaryToken("bank", "bank", 2, 6, "банк", "bank"),
                ContextGlossaryToken("differs", "differs", 7, 14, "відрізняється", "differ"),
                ContextGlossaryToken("from", "from", 15, 19, "від", "from"),
                ContextGlossaryToken("other", "other", 20, 25, "інших", "other"),
                ContextGlossaryToken("banks", "banks", 26, 31, "банків", "bank"),
                ContextGlossaryToken("near", "near", 32, 36, "біля", "near"),
                ContextGlossaryToken("the", "the", 37, 40, "артикль", "the"),
                ContextGlossaryToken("bank", "bank", 41, 45, "банку", "bank"),
                ContextGlossaryToken("banker", "banker", 52, 58, "банкір", "banker"),
            ),
        )

        assertEquals(setOf(1, 5, 8), contextTargetTokenIndexes(glossary, "bank"))
    }

    @Test
    fun bookmarkSavesTheLemmaAndKeepsTheWholeContextSnapshot() {
        val token = ContextGlossaryToken("bakeries", "bakeries", 4, 12, "пекарні", "bakery")
        val glossary = ContextGlossary(
            sentence = "New bakeries opened downtown.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(token),
        )

        val bookmark = practiceBookmark(glossary, token, originTopicId = "travel")

        assertEquals("bakery", bookmark.source)
        assertEquals("пекарні", bookmark.translation)
        assertEquals(glossary, bookmark.toWordDetails().contextGlossary)
    }

    @Test
    fun contextKeysKeepDifferentTranslationsOfTheSameWordSeparate() {
        val glossary = ContextGlossary(
            sentence = "The bank is near the river bank.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = emptyList(),
        )
        val financial = ContextGlossaryToken("bank", "bank", 4, 8, "банк", "bank")
        val riverside = ContextGlossaryToken("bank", "bank", 27, 31, "берег", "bank")

        assertFalse(contextBookmarkKey(glossary, financial) == contextBookmarkKey(glossary, riverside))
        assertEquals(
            contextBookmarkKey(glossary, financial),
            contextTranslationKey("EN", "UK", "Bank", "Банк"),
        )
    }

    @Test
    fun legacySavedUnitsGetConservativeLabelsWithoutRemoteResearch() {
        assertEquals(listOf("Фраза"), lexicalLabelsFor("by the way", details = null))
        assertEquals(listOf("Абревіатура"), lexicalLabelsFor("NATO", details = null))
        assertEquals(emptyList<String>(), lexicalLabelsFor("apple", details = null))
        assertEquals(
            listOf("Вислів"),
            lexicalLabelsFor(
                source = "break a leg",
                details = WordDetails(lexicalUnitKind = LexicalUnitKind.Expression),
            ),
        )
    }

    @Test
    fun practiceDeckOrderDoesNotDependOnRepositoryOrdering() {
        val candidates = listOf(
            "topic:one" to 0,
            "topic:two" to 20,
            "topic:three" to 0,
        )

        assertEquals(
            buildPracticeDeckKeys(candidates, seed = 42),
            buildPracticeDeckKeys(candidates.reversed(), seed = 42),
        )
    }

    @Test
    fun reverseFlipKeepsBackFaceUntilCardCrossesNinetyDegrees() {
        assertTrue(shouldShowPracticeCardBack(180f))
        assertTrue(shouldShowPracticeCardBack(135f))
        assertFalse(shouldShowPracticeCardBack(45f))
        assertTrue(shouldShowPracticeCardBack(-180f))
        assertTrue(shouldShowPracticeCardBack(-135f))
    }

    @Test
    fun horizontalFlingChoosesRotationDirection() {
        assertEquals(
            PracticeFlipDirection.Left,
            practiceFlipDirectionForGesture(-900f, -10f, minimumVelocity = 600f, minimumDistance = 100f),
        )
        assertEquals(
            PracticeFlipDirection.Right,
            practiceFlipDirectionForGesture(900f, 10f, minimumVelocity = 600f, minimumDistance = 100f),
        )
        assertNull(
            practiceFlipDirectionForGesture(200f, 30f, minimumVelocity = 600f, minimumDistance = 100f),
        )
    }

    @Test
    fun practiceContextHighlightsInflectedTargetWordLikeContextMode() {
        val sentence = highlightedSentence(
            sentence = "Children love listening to fables with animals.",
            target = "fable",
        )

        val highlightedRange = sentence.spanStyles.single()
        assertEquals("fables", sentence.text.substring(highlightedRange.start, highlightedRange.end))
        assertEquals(PrototypeColor.Yellow, highlightedRange.item.background)
        assertEquals(PrototypeColor.YellowText, highlightedRange.item.color)
    }

    @Test
    fun manuallyRevealingTranslationRecordsUnknownOnlyOnce() {
        assertTrue(
            shouldRecordUnknownOnManualReveal(
                isFlipped = false,
                waitingForNextAfterMiss = false,
            ),
        )
        assertFalse(
            shouldRecordUnknownOnManualReveal(
                isFlipped = true,
                waitingForNextAfterMiss = false,
            ),
        )
        assertFalse(
            shouldRecordUnknownOnManualReveal(
                isFlipped = false,
                waitingForNextAfterMiss = true,
            ),
        )
    }
}
