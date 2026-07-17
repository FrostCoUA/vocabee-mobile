package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
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
}
