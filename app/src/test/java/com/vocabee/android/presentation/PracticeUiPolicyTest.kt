package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.vocabee.android.feature.vocabulary.presentation.ContextTokenHighlightRadius
import com.vocabee.android.feature.vocabulary.presentation.ContextTokenHighlightPadding
import com.vocabee.android.feature.vocabulary.presentation.contextTokenHighlightColor
import androidx.compose.ui.graphics.isUnspecified
import com.vocabee.android.feature.vocabulary.presentation.contextSentenceAnnotated
import androidx.compose.ui.unit.TextUnit

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

    /**
     * Регресія: `Modifier.weight(1f)` у Row попапа роздував його на всю ширину
     * вікна (Popup вимірює вміст по межах екрана), `maximumX` схлопувався до
     * відступу — і попап назавжди прилипав до лівого краю.
     */
    @Test
    fun theContextPopupIsCenteredOverTheTappedWord() {
        val position = AboveTokenPopupPositionProvider(
            tokenBounds = IntRect(left = 500, top = 44, right = 600, bottom = 90),
            horizontalMarginPx = 22,
            gapPx = 25,
        ).calculatePosition(
            anchorBounds = IntRect(left = 60, top = 1850, right = 1020, bottom = 1960),
            windowSize = IntSize(width = 1080, height = 2400),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 150),
        )

        // Центр токена у вікні = 60 + 500 + 50 = 610 → лівий край = 610 − 160.
        assertEquals(450, position.x)
        // Над токеном: 1850 + 44 − 150 − 25.
        assertEquals(1719, position.y)
    }

    @Test
    fun aFullWidthPopupWouldNoLongerPinItselfToTheLeftEdge() {
        // Та сама геометрія, але попап ширший за екран: тоді центрування
        // неможливе й x схлопується у відступ. Тест фіксує, що це стається
        // ЛИШЕ при завеликому вмісті — саме тому вміст обмежений widthIn.
        val provider = { popupWidth: Int ->
            AboveTokenPopupPositionProvider(
                tokenBounds = IntRect(left = 500, top = 44, right = 600, bottom = 90),
                horizontalMarginPx = 22,
                gapPx = 25,
            ).calculatePosition(
                anchorBounds = IntRect(left = 60, top = 1850, right = 1020, bottom = 1960),
                windowSize = IntSize(width = 1080, height = 2400),
                layoutDirection = LayoutDirection.Ltr,
                popupContentSize = IntSize(width = popupWidth, height = 150),
            ).x
        }

        assertEquals(22, provider(1080))
        assertTrue(provider(320) > 22)
    }

    @Test
    fun theContextPopupStaysInsideBothScreenEdges() {
        fun xFor(tokenLeft: Int): Int = AboveTokenPopupPositionProvider(
            tokenBounds = IntRect(left = tokenLeft, top = 44, right = tokenLeft + 60, bottom = 90),
            horizontalMarginPx = 22,
            gapPx = 25,
        ).calculatePosition(
            anchorBounds = IntRect(left = 0, top = 1850, right = 1080, bottom = 1960),
            windowSize = IntSize(width = 1080, height = 2400),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 150),
        ).x

        assertEquals(22, xFor(tokenLeft = 0))
        assertEquals(1080 - 320 - 22, xFor(tokenLeft = 1020))
        assertEquals(370, xFor(tokenLeft = 500))
    }

    @Test
    fun aPopupWithNoRoomAboveTheTokenFlipsBelowIt() {
        val position = AboveTokenPopupPositionProvider(
            tokenBounds = IntRect(left = 500, top = 10, right = 600, bottom = 56),
            horizontalMarginPx = 22,
            gapPx = 25,
        ).calculatePosition(
            anchorBounds = IntRect(left = 0, top = 40, right = 1080, bottom = 150),
            windowSize = IntSize(width = 1080, height = 2400),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 150),
        )

        // 40 + 10 − 150 − 25 < 22 → падаємо під токен: 40 + 56 + 25.
        assertEquals(121, position.y)
    }

    /**
     * Сам `weight(1f)` ловиться лише UI-тестом (у модулі немає ні Robolectric,
     * ні ui-test-junit4), тому фіксуємо намір: бюджет ширини попапа мусить
     * лишати запас навіть на вузькому 320dp-екрані — інакше центрування над
     * словом неможливе в принципі.
     */
    @Test
    fun theContextPopupBudgetFitsEvenTheNarrowestPhone() {
        val narrowestPhoneWidth = 320.dp

        assertTrue(
            "Попап $ContextPopupMaxWidth не влазить у $narrowestPhoneWidth з відступами",
            ContextPopupMaxWidth <= narrowestPhoneWidth - 32.dp,
        )
    }

    @Test
    fun aRelayoutNeverLosesTheOpenPopupAnchor() {
        val anchor = IntRect(left = 500, top = 44, right = 600, bottom = 90)
        val shifted = IntRect(left = 512, top = 44, right = 618, bottom = 90)

        // Жирний токен після виділення ширший — нові bounds приймаються.
        assertEquals(shifted, contextPopupBoundsAfterRelayout(anchor, shifted))
        // А порожній/нульовий результат не має гасити відкритий попап.
        assertEquals(anchor, contextPopupBoundsAfterRelayout(anchor, null))
        assertEquals(anchor, contextPopupBoundsAfterRelayout(anchor, IntRect(0, 0, 0, 0)))
        assertEquals(anchor, contextPopupBoundsAfterRelayout(anchor, IntRect(500, 44, 500, 90)))
    }

    @Test
    fun theHighlightedTargetWordIsNotInteractive() {
        val glossary = ContextGlossary(
            sentence = "He runs a small bakery downtown.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(
                ContextGlossaryToken("runs", "runs", 3, 7, "керує", "run"),
                ContextGlossaryToken("bakery", "bakery", 16, 22, "пекарня", "bakery"),
                ContextGlossaryToken("downtown", "downtown", 23, 31, "центр міста", "downtown"),
            ),
        )
        val targets = contextTargetTokenIndexes(glossary, "run")

        // Тап по жовтому цільовому слову не відкриває попап…
        assertNull(contextSelectableTokenIndexAtOffset(glossary, offset = 4, targetTokenIndexes = targets))
        // …решта слів лишаються тапабельними.
        assertEquals(1, contextSelectableTokenIndexAtOffset(glossary, offset = 17, targetTokenIndexes = targets))
        assertEquals(2, contextSelectableTokenIndexAtOffset(glossary, offset = 24, targetTokenIndexes = targets))
    }

    @Test
    fun contextTokensNeverCarryUnderlinesAndReadTheirStateFromFillsOnly() {
        val styles = ContextTokenVisual.entries.associateWith(::contextTokenSpanStyle)
        val fills = ContextTokenVisual.entries.associateWith(::contextTokenHighlightColor)

        assertTrue(styles.values.filterNotNull().all { style -> style.textDecoration == null })
        assertNull(styles.getValue(ContextTokenVisual.Plain))
        assertNull(fills.getValue(ContextTokenVisual.Plain))

        // Заливка НЕ живе у SpanStyle: той малює прямокутник впритул до глифів,
        // а борд вимагає відступ + скруглення, тож підсвітку малює drawBehind.
        assertTrue(styles.values.filterNotNull().all { style -> style.background.isUnspecified })

        // Заливку має ЛИШЕ цільове слово раунду; натиснуте й збережене
        // відрізняються самим кольором тексту, щоб у реченні не світилося
        // кілька плашок одночасно.
        assertEquals(PrototypeColor.Yellow, fills.getValue(ContextTokenVisual.Target))
        assertEquals(PrototypeColor.YellowText, styles.getValue(ContextTokenVisual.Target)?.color)
        assertNull(fills.getValue(ContextTokenVisual.Saved))
        assertEquals(PrototypeColor.NoteYellowText, styles.getValue(ContextTokenVisual.Saved)?.color)
        assertNull(fills.getValue(ContextTokenVisual.Selected))
        assertEquals(PrototypeColor.PurpleText, styles.getValue(ContextTokenVisual.Selected)?.color)
    }

    @Test
    fun theHighlightedWordPushesItsNeighboursAway() {
        // Плашка малюється ширшою за глифи, тож сусідні пробіли мають бути
        // розширені — інакше підсвітка торкається «This» і «sells».
        val sentence = "This bakery sells bread."
        val tokens = listOf(
            ContextGlossaryToken("bakery", "bakery", 5, 11, "пекарня"),
            ContextGlossaryToken("bread", "bread", 18, 23, "хліб"),
        )

        val annotated = contextSentenceAnnotated(
            sentence = sentence,
            tokens = tokens,
            targetTokenIndexes = setOf(0),
            savedTokenIndexes = setOf(1),
            selectedIndex = 1,
        )
        val gaps = annotated.spanStyles.filter { it.item.letterSpacing != TextUnit.Unspecified }

        // Пробіли обабіч цільового слова (індекси 4 і 11) розширені…
        assertEquals(setOf(4 to 5, 11 to 12), gaps.map { it.start to it.end }.toSet())
        // …а слово без заливки (збережене+вибране) сусідів не розсуває.
        assertTrue(gaps.none { it.start == 17 })
    }

    @Test
    fun thePopupCaretPointsAtTheTappedWord() {
        var caretCenter = -1
        var popupLeft = -1
        var above = false
        AboveTokenPopupPositionProvider(
            tokenBounds = IntRect(left = 500, top = 44, right = 600, bottom = 90),
            horizontalMarginPx = 22,
            gapPx = 25,
            onPlaced = { x, tokenCenterX, isAbove ->
                popupLeft = x
                caretCenter = tokenCenterX - x
                above = isAbove
            },
        ).calculatePosition(
            anchorBounds = IntRect(left = 60, top = 1850, right = 1020, bottom = 1960),
            windowSize = IntSize(width = 1080, height = 2400),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 150),
        )

        assertEquals(450, popupLeft)
        // Носик стоїть під центром токена: 610 − 450 = середина бульбашки.
        assertEquals(160, caretCenter)
        assertTrue(above)
    }

    @Test
    fun thePopupCaretFollowsTheWordWhenTheBubbleIsClampedToTheEdge() {
        var caretCenter = -1
        AboveTokenPopupPositionProvider(
            tokenBounds = IntRect(left = 0, top = 44, right = 60, bottom = 90),
            horizontalMarginPx = 22,
            gapPx = 25,
            onPlaced = { x, tokenCenterX, _ -> caretCenter = tokenCenterX - x },
        ).calculatePosition(
            anchorBounds = IntRect(left = 0, top = 1850, right = 1080, bottom = 1960),
            windowSize = IntSize(width = 1080, height = 2400),
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = IntSize(width = 320, height = 150),
        )

        // Бульбашку притиснуло до лівого краю (x=22), але носик лишається
        // над словом (центр токена 30) — тобто зліва від центру бульбашки.
        assertEquals(8, caretCenter)
    }

    @Test
    fun theTokenHighlightLeavesBreathingRoomAroundTheWord() {
        // Борд 13: `padding: 0 4px; border-radius: 4–5px` — підсвітка не впритул.
        assertTrue(ContextTokenHighlightPadding > 0.dp)
        assertTrue(ContextTokenHighlightRadius > 0.dp)
    }

    @Test
    fun aBookmarkedTokenKeepsItsYellowFillWhileItsPopupIsOpen() {
        val targets = setOf(0)
        val saved = setOf(2)

        assertEquals(
            ContextTokenVisual.Target,
            contextTokenVisual(0, targets, saved, selectedIndex = 0),
        )
        assertEquals(
            ContextTokenVisual.Selected,
            contextTokenVisual(1, targets, saved, selectedIndex = 1),
        )
        assertEquals(
            ContextTokenVisual.Saved,
            contextTokenVisual(2, targets, saved, selectedIndex = 2),
        )
        assertEquals(
            ContextTokenVisual.Plain,
            contextTokenVisual(3, targets, saved, selectedIndex = 1),
        )
    }

    @Test
    fun aSecondTapInThePopupRemovesTheBookmarkInsteadOfDuplicatingIt() {
        val token = ContextGlossaryToken("downtown", "downtown", 23, 31, "центр міста", "downtown")
        val glossary = ContextGlossary(
            sentence = "He runs a small bakery downtown.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(token),
        )
        val bookmark = practiceBookmark(glossary, token, originTopicId = "travel")

        val added = toggledPracticeBookmarks(emptyList(), bookmark)
        assertEquals(listOf(bookmark), added)
        assertEquals(emptyList<PracticeBookmark>(), toggledPracticeBookmarks(added, bookmark))
    }

    @Test
    fun aTokenWithoutATranslationNeverBecomesABookmark() {
        val token = ContextGlossaryToken("the", "the", 0, 3, "   ", "the")
        val glossary = ContextGlossary(
            sentence = "the bakery",
            sourceLang = "en",
            targetLang = "uk",
            tokens = listOf(token),
        )

        assertEquals(
            emptyList<PracticeBookmark>(),
            toggledPracticeBookmarks(emptyList(), practiceBookmark(glossary, token, "travel")),
        )
    }

    @Test
    fun tappingAnAlreadySavedWordAgainNeverChargesASecondCoin() {
        // У словнику збережене слово віддає ✓ і більше не клікається,
        // у тренуванні той самий тап знімає закладку.
        assertFalse(
            contextPopupActionEnabled(ContextGlossaryTokenAction.AddToDictionary, isSaved = true),
        )
        assertTrue(
            contextPopupActionEnabled(ContextGlossaryTokenAction.AddToDictionary, isSaved = false),
        )
        assertTrue(contextPopupActionEnabled(ContextGlossaryTokenAction.Bookmark, isSaved = true))
    }

    @Test
    fun savedBookmarksKeepTheirRowButLeaveTheBadge() {
        val glossary = ContextGlossary(
            sentence = "He runs a small bakery downtown.",
            sourceLang = "en",
            targetLang = "uk",
            tokens = emptyList(),
        )
        val bakery = practiceBookmark(
            glossary,
            ContextGlossaryToken("bakery", "bakery", 16, 22, "пекарня", "bakery"),
            originTopicId = "travel",
        )
        val downtown = practiceBookmark(
            glossary,
            ContextGlossaryToken("downtown", "downtown", 23, 31, "центр міста", "downtown"),
            originTopicId = "travel",
        )
        val bookmarks = listOf(bakery, downtown)

        assertEquals(bookmarks, pendingPracticeBookmarks(bookmarks, savedKeys = emptySet()))
        assertEquals(
            listOf(downtown),
            pendingPracticeBookmarks(bookmarks, savedKeys = setOf(bakery.key)),
        )
    }

    @Test
    fun theSaveToastNamesTheWordTheDictionaryAndTheCharge() {
        assertEquals(
            "bakery додано в «Подорожі» · −1 монетка",
            bookmarkSavedMessage(listOf("bakery"), topicTitle = "Подорожі", chargedBees = 1),
        )
        assertEquals(
            "2 слова додано в «Подорожі» · −2 монетки",
            bookmarkSavedMessage(listOf("bakery", "downtown"), topicTitle = "Подорожі", chargedBees = 2),
        )
        assertEquals(
            "Ці слова вже є в словнику «Подорожі»",
            bookmarkSavedMessage(emptyList(), topicTitle = "Подорожі", chargedBees = 0),
        )
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
