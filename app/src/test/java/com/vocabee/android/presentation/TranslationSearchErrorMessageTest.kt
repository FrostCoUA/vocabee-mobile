package com.vocabee.android.feature.vocabulary.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationSearchErrorMessageTest {
    @Test
    fun releaseBuildShowsNeutralErrorMessage() {
        assertEquals(
            "Something went wrong",
            translationSearchFailureMessage(
                rawMessage = "Сервіс не відповів вчасно. Спробуйте пізніше.",
                showRawError = false,
            ),
        )
    }

    @Test
    fun debugBuildShowsRawErrorMessage() {
        assertEquals(
            "Сервіс не відповів вчасно. Спробуйте пізніше.",
            translationSearchFailureMessage(
                rawMessage = "Сервіс не відповів вчасно. Спробуйте пізніше.",
                showRawError = true,
            ),
        )
    }
}
