package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.usecase.SearchHintsUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHintsPolicyTest {
    private val unfinished = SearchHintsUseCase.Hints(lookup = false, words = listOf("there", "therefore"))
    private val knownWord = SearchHintsUseCase.Hints(lookup = true, words = listOf("they", "there"))

    @Test
    fun unfinishedWordOnlyOffersHints() {
        assertFalse(shouldLookUpNow(forced = false, hints = unfinished))
    }

    @Test
    fun knownWordIsLookedUpWithHintsBelow() {
        assertTrue(shouldLookUpNow(forced = false, hints = knownWord))
    }

    @Test
    fun explicitRequestIsAlwaysLookedUp() {
        assertTrue(shouldLookUpNow(forced = true, hints = unfinished))
    }

    @Test
    fun gatewayWithoutHintsKeepsTheOldLookup() {
        assertTrue(shouldLookUpNow(forced = false, hints = null))
    }
}
