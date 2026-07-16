# Practice Session Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make active practice full-screen and exit-safe, correct classic card/CTA interaction, and simplify context practice to readable option-based recognition plus matching.

**Architecture:** Keep `VocabeeRoute.Practice` as the single route and hoist only an `isPracticeFullScreen` chrome signal to `MainApp`. `PracticeScreen` remains the owner of setup selections, session completion, and interruption confirmation; context deck eligibility stays pure and testable in `ContextPractice.kt`, while UI layout uses Compose intrinsic sizing and flow layout.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose Material 3, Navigation3, `BackHandler`, JUnit 4, existing `PrototypeBottomSheet`.

## Global Constraints

- Setup keeps the bottom tab bar; active and completed sessions hide it.
- Close and system Back show `Перервати тренування?` only for an unfinished round.
- `Продовжити` preserves the exact current deck/card; `Перервати` returns to setup with mode and selected dictionaries preserved.
- The interruption body states that already recorded answers stay saved.
- Completed-result close/back returns to setup without interruption confirmation.
- Classic `Далі` sits above the system navigation inset and its chevron follows the text.
- Ripple is clipped by the same rounded `Surface` that paints each interactive card/button.
- Context answer text is never ellipsized.
- Context recall/flip and `Знаю` / `Не знаю` do not exist after this change.
- Context pairs without an honest wrong option are neither counted nor placed in the deck.
- UI copy remains Ukrainian; D10 knowledge increments stay at ±20.
- Use `apply_patch` for all file edits.

## File map

- Create `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PracticeSessionPolicy.kt`: pure app-chrome and exit decisions.
- Create `app/src/test/java/com/vocabee/android/presentation/PracticeSessionPolicyTest.kt`: setup/session/result policy coverage.
- Create `app/src/test/java/com/vocabee/android/presentation/ContextPracticeDeckTest.kt`: eligibility, no-recall, matching, and anti-repeat coverage.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt`: parent chrome signal, practice Back/close state, classic full-screen layout, trailing `Далі` icon, and clipped ripple.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt`: exact deck eligibility, no recall, no hints/format labels, equal mode cards, and adaptive answer flow.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PrototypeBottomSheets.kt`: practice interruption sheet.
- Modify `docs/01-screens.md`, `docs/11-practice-training.md`, and `docs/12-motion-and-interaction-brief.md`: living behavior and motion documentation.

---

### Task 1: Define full-screen chrome and exit policies

**Files:**
- Create: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PracticeSessionPolicy.kt`
- Test: `app/src/test/java/com/vocabee/android/presentation/PracticeSessionPolicyTest.kt`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt:424-455,700-710,805-855,3621-3685`

**Interfaces:**
- Produces: `shouldShowBottomBar(route, practiceFullScreen): Boolean`, `PracticeExitAction`, and `practiceExitAction(roundCompleted): PracticeExitAction`.
- Produces: `PracticeScreen(..., onFullScreenChanged: (Boolean) -> Unit)` for `MainApp`.

- [ ] **Step 1: Write failing policy tests**

```kotlin
package com.vocabee.android.presentation

import com.vocabee.android.feature.vocabulary.presentation.PracticeExitAction
import com.vocabee.android.feature.vocabulary.presentation.practiceExitAction
import com.vocabee.android.feature.vocabulary.presentation.shouldShowBottomBar
import com.vocabee.android.feature.vocabulary.presentation.navigation.VocabeeRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeSessionPolicyTest {
    @Test fun setupShowsPracticeTabBar() {
        assertTrue(shouldShowBottomBar(VocabeeRoute.Practice, practiceFullScreen = false))
    }

    @Test fun activeAndResultPracticeHideTabBar() {
        assertFalse(shouldShowBottomBar(VocabeeRoute.Practice, practiceFullScreen = true))
    }

    @Test fun nonRootRoutesDoNotGainATabBar() {
        assertFalse(shouldShowBottomBar(VocabeeRoute.TopicDetail("topic"), practiceFullScreen = false))
    }

    @Test fun unfinishedRoundConfirmsButCompletedRoundReturnsToSetup() {
        assertEquals(PracticeExitAction.ConfirmInterruption, practiceExitAction(roundCompleted = false))
        assertEquals(PracticeExitAction.ReturnToSetup, practiceExitAction(roundCompleted = true))
    }
}
```

- [ ] **Step 2: Run the policy test and verify missing symbols**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.PracticeSessionPolicyTest`

Expected: compilation fails because the policy file does not exist.

- [ ] **Step 3: Implement the pure policies**

```kotlin
package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.presentation.navigation.AppTab
import com.vocabee.android.feature.vocabulary.presentation.navigation.VocabeeRoute

internal fun shouldShowBottomBar(
    route: VocabeeRoute?,
    practiceFullScreen: Boolean,
): Boolean = AppTab.entries.any { it.route == route } &&
    !(route == VocabeeRoute.Practice && practiceFullScreen)

internal enum class PracticeExitAction {
    ConfirmInterruption,
    ReturnToSetup,
}

internal fun practiceExitAction(roundCompleted: Boolean): PracticeExitAction =
    if (roundCompleted) PracticeExitAction.ReturnToSetup else PracticeExitAction.ConfirmInterruption
```

- [ ] **Step 4: Wire `MainApp` chrome state**

Replace the direct route check with:

```kotlin
var practiceFullScreen by remember { mutableStateOf(false) }
val showBottomBar = shouldShowBottomBar(currentRoute, practiceFullScreen)
```

Pass `onFullScreenChanged = { practiceFullScreen = it }` to `PracticeScreen`. In `PracticeScreen`, report the local phase and clear it on disposal:

```kotlin
LaunchedEffect(practiceStarted) {
    onFullScreenChanged(practiceStarted)
}
DisposableEffect(Unit) {
    onDispose { onFullScreenChanged(false) }
}
```

Setup remains `practiceStarted == false`; both running and done remain `true`.

- [ ] **Step 5: Run policy tests and compile**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.PracticeSessionPolicyTest`

Expected: 4 tests pass.

Run: `./gradlew :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL` with the new callback signature.

- [ ] **Step 6: Commit chrome policy and wiring**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PracticeSessionPolicy.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt app/src/test/java/com/vocabee/android/presentation/PracticeSessionPolicyTest.kt
git commit -m "feat: hide app chrome during practice"
```

---

### Task 2: Add interruption confirmation and correct classic layout

**Files:**
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt:3621-3865,4189-4420`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt:570-810`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PrototypeBottomSheets.kt:64-130`

**Interfaces:**
- Consumes: `practiceExitAction` from Task 1.
- Produces: `TrainingInterruptionSheet(onContinue, onInterrupt)` and `ContextPracticeSession(..., onCompletionChanged: (Boolean) -> Unit)`.

- [ ] **Step 1: Add the interruption sheet**

```kotlin
@Composable
internal fun TrainingInterruptionSheet(
    onContinue: () -> Unit,
    onInterrupt: () -> Unit,
) {
    PrototypeBottomSheet(
        title = "Перервати тренування?",
        onDismiss = onContinue,
    ) {
        Text(
            text = "Поточний раунд завершиться. Уже зараховані відповіді залишаться збереженими.",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrototypeColor.NeutralSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Продовжити", color = PrototypeColor.Ink, fontWeight = FontWeight.ExtraBold)
                }
            }
            Surface(
                onClick = onInterrupt,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrototypeColor.NotePeach,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Перервати", color = PrototypeColor.OrangeText, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Make `PracticeScreen` own exit state**

Add state and helpers before rendering setup/session:

```kotlin
var exitConfirmationVisible by remember { mutableStateOf(false) }
var roundCompleted by remember { mutableStateOf(false) }

fun returnToSetup() {
    exitConfirmationVisible = false
    roundCompleted = false
    practiceStarted = false
}

fun requestExit() {
    when (practiceExitAction(roundCompleted)) {
        PracticeExitAction.ConfirmInterruption -> exitConfirmationVisible = true
        PracticeExitAction.ReturnToSetup -> returnToSetup()
    }
}

BackHandler(enabled = practiceStarted) { requestExit() }
```

On start and restart set `roundCompleted = false`. When classic `moveNext()` completes the deck, set `roundCompleted = true` before `onRoundCompleted()`. Pass `onClose = ::requestExit` to the classic header and `onExit = ::requestExit` plus `onCompletionChanged = { roundCompleted = it }` to `ContextPracticeSession`. Replace direct `practiceStarted = false` completion actions with `returnToSetup()`.

Render this after the active branch so it also overlays context practice:

```kotlin
if (exitConfirmationVisible) {
    TrainingInterruptionSheet(
        onContinue = { exitConfirmationVisible = false },
        onInterrupt = ::returnToSetup,
    )
}
```

Remove the current early `return` after the context branch so the sheet stays in the same composition.

- [ ] **Step 3: Report context completion without rebuilding the deck**

Extend `ContextPracticeSession` with `onCompletionChanged: (Boolean) -> Unit`. Add:

```kotlin
LaunchedEffect(done) { onCompletionChanged(done) }
```

Its restart action becomes:

```kotlin
onRestart = {
    onCompletionChanged(false)
    shuffleSeed = Random.nextInt()
},
```

The existing remembered `deck`, `index`, answers, peeks, and bookmarks stay untouched when the interruption sheet is merely dismissed.

- [ ] **Step 4: Add the classic close control and full-height structure**

Replace the title-only header with a `Row`: the title column keeps progress text/bar and a 40 dp top-right close control calls `requestExit()`. Keep `statusBarsPadding()` on the session root.

Keep `PracticeFlipCard` on `Modifier.weight(1f)` and change its padding to:

```kotlin
Modifier.weight(1f).padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 12.dp)
```

Change the CTA row bottom padding to 12 dp after `navigationBarsPadding()`, placing it at the bottom of the available full-screen area.

- [ ] **Step 5: Clip ripple and move the `Далі` icon**

Use clickable `Surface` overloads rather than a `clickable` modifier outside the shape:

```kotlin
Surface(
    onClick = onFlip,
    modifier = modifier.fillMaxWidth().graphicsLayer { /* existing rotation */ },
    shape = RoundedCornerShape(28.dp),
    color = if (showBack) card.accent else PrototypeColor.White,
    border = if (showBack) null else BorderStroke(2.dp, card.accent),
    shadowElevation = 14.dp,
) { /* existing content */ }
```

Extend `PracticeAnswerButton` with `trailingIcon: Boolean = false`, use `Surface(onClick = ...)`, and render content in this order:

```kotlin
if (!trailingIcon) PracticeButtonIcon(icon, color, background)
if (!trailingIcon) Spacer(Modifier.width(9.dp))
Text(text = text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.5.sp)
if (trailingIcon) Spacer(Modifier.width(9.dp))
if (trailingIcon) PracticeButtonIcon(icon, color, background)
```

Extract the existing 30 dp icon `Surface` into `PracticeButtonIcon`. Pass `trailingIcon = true` only for `Далі`; `Не знаю` and `Знаю` remain leading-icon buttons.

- [ ] **Step 6: Compile and run the policy test**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.PracticeSessionPolicyTest`

Expected: all policy tests pass.

Run: `./gradlew :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; all new callback and Material `Surface(onClick)` overloads resolve.

- [ ] **Step 7: Commit exit and classic interaction changes**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/PrototypeBottomSheets.kt
git commit -m "feat: confirm practice interruption"
```

---

### Task 3: Remove recall and enforce honest context eligibility

**Files:**
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt:74-350,570-790,1216-1535`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt:3869-4035`
- Test: `app/src/test/java/com/vocabee/android/presentation/ContextPracticeDeckTest.kt`

**Interfaces:**
- Produces: internal `ContextDirection`, internal `ContextCard` with only `Recognition` and `Matching`, `contextPairCounts(topics): Map<String, Int>`, and internal `buildContextDeck(topics, seed): List<ContextCard>`.
- Consumes: selected topics only, so counts and generated cards use the same distractor pool.

- [ ] **Step 1: Write failing deck tests**

```kotlin
package com.vocabee.android.presentation

import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.presentation.ContextCard
import com.vocabee.android.feature.vocabulary.presentation.ContextDirection
import com.vocabee.android.feature.vocabulary.presentation.buildContextDeck
import com.vocabee.android.feature.vocabulary.presentation.contextPairCounts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextPracticeDeckTest {
    private val en = LanguageOption("en", "English", "EN", "en-US")
    private val uk = LanguageOption("uk", "Українська", "UK", "uk-UA")

    private fun details(index: Int, examples: List<String>) = WordDetails(
        senseIndex = index,
        senses = listOf(
            WordSense("sense zero", "noun", examples = if (index == 0) examples else listOf("Sense zero example.")),
            WordSense("sense one", "noun", examples = if (index == 1) examples else listOf("Sense one example.")),
        ),
        partOfSpeech = listOf("noun"),
    )

    private fun word(
        id: String,
        translation: String,
        sense: Int,
        source: String = "flare",
        knowledge: Int = 40,
        examples: List<String> = listOf("Example $id."),
    ) = WordEntry(
        id = id,
        source = source,
        translation = translation,
        ipa = "/fleə/",
        details = details(sense, examples),
        knowledgePercent = knowledge,
    )

    private fun topic(words: List<WordEntry>) = DictionaryTopic(
        id = "topic",
        title = "Test",
        sourceLanguage = en,
        targetLanguage = uk,
        words = words,
    )

    @Test fun formerRecallRangeNowBuildsRecognition() {
        val deck = buildContextDeck(
            listOf(topic(listOf(word("signal", "сигнальний вогонь", 0), word("flash", "спалах", 1)))),
            seed = 7,
        )

        assertTrue(deck.isNotEmpty())
        assertTrue(deck.all { it is ContextCard.Recognition })
        assertTrue(deck.filterIsInstance<ContextCard.Recognition>().all { it.direction == ContextDirection.EnToUk })
    }

    @Test fun pairWithoutHonestWrongOptionIsNotCountedOrBuilt() {
        val onlyTrainable = word("signal", "сигнальний вогонь", 0, knowledge = 20)
        val missingSentence = word("flash", "спалах", 1, knowledge = 20, examples = emptyList())
            .copy(details = WordDetails(senseIndex = 0, senses = emptyList(), partOfSpeech = listOf("noun")))
        val topic = topic(listOf(onlyTrainable, missingSentence))

        assertEquals(0, contextPairCounts(listOf(topic)).values.sum())
        assertTrue(buildContextDeck(listOf(topic), seed = 1).isEmpty())
    }

    @Test fun matureThreeSenseGroupStillBuildsMatching() {
        val senses = listOf(
            WordSense("one", "noun", examples = listOf("One sentence.")),
            WordSense("two", "noun", examples = listOf("Two sentence.")),
            WordSense("three", "noun", examples = listOf("Three sentence.")),
        )
        val words = senses.indices.map { index ->
            WordEntry(
                id = "word-$index",
                source = "bank",
                translation = "translation-$index",
                ipa = "/bæŋk/",
                details = WordDetails(index, senses, partOfSpeech = listOf("noun")),
                knowledgePercent = 60,
            )
        }
        val mature = topic(words).copy(id = "matching")

        assertEquals(3, contextPairCounts(listOf(mature)).values.sum())
        assertTrue(buildContextDeck(listOf(mature), seed = 2).single() is ContextCard.Matching)
    }

    @Test fun deckKeepsDifferentSourcesApartWhenAlternationIsPossible() {
        val mixed = topic(
            listOf(
                word("flare-0", "спалах", 0, source = "flare", knowledge = 20),
                word("flare-1", "сигнальний вогонь", 1, source = "flare", knowledge = 20),
                word("bank-0", "банк", 0, source = "bank", knowledge = 20),
                word("bank-1", "берег", 1, source = "bank", knowledge = 20),
            ),
        )

        val sources = buildContextDeck(listOf(mixed), seed = 3).map { card ->
            when (card) {
                is ContextCard.Recognition -> card.pair.word.source
                is ContextCard.Matching -> card.source
            }
        }

        assertTrue(sources.zipWithNext().none { (left, right) -> left.equals(right, ignoreCase = true) })
    }
}
```

- [ ] **Step 2: Run deck tests and verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.ContextPracticeDeckTest`

Expected: compilation fails because deck types/builders are private and `contextPairCounts` is absent.

- [ ] **Step 3: Remove the recall model and expose testable deck types**

Change `ContextDirection` and `ContextCard` from private to internal. Delete `ContextCard.Recall`, `RecallThreshold`, the `Recall` branch in `sourceKey`, the `RecallCardView` branch in `ContextPracticeSession`, and the entire `RecallCardView` composable.

Remove `sentenceByTranslation` from `ContextPair` and its construction because the post-error hint is removed in Task 4.

- [ ] **Step 4: Centralize eligibility and counts**

Add:

```kotlin
private fun ContextPair.hasRecognitionDistractor(allSources: List<String>): Boolean =
    if (word.knowledgePercent >= ProductionThreshold) {
        allSources.any { !it.equals(word.source, ignoreCase = true) }
    } else {
        senseTranslations.any { !it.equals(word.translation, ignoreCase = true) }
    }

private fun matchingGroups(pairs: List<ContextPair>): List<List<ContextPair>> = pairs
    .groupBy { it.topicId to normalizedSource(it.word) }
    .values
    .filter { group ->
        group.size >= 3 && group.all { it.word.knowledgePercent >= MatchingThreshold }
    }

internal fun contextPairCounts(topics: List<DictionaryTopic>): Map<String, Int> {
    val pairs = buildContextPairs(topics)
    val matchingIds = matchingGroups(pairs).flatten().mapTo(mutableSetOf()) { it.word.id }
    val allSources = pairs.map { it.word.source }.distinct()
    return pairs
        .filter { it.word.id in matchingIds || it.hasRecognitionDistractor(allSources) }
        .groupingBy { it.topicId }
        .eachCount()
}

internal fun DictionaryTopic.contextPairCount(): Int = contextPairCounts(listOf(this))[id] ?: 0
```

Use `contextPairCounts(topics)` for total row counts in `PracticeSetupScreen` and `contextPairCounts(selectedTopics).values.sum()` for the selected footer/start-button gate.

- [ ] **Step 5: Replace recall fallback with nullable recognition**

Make `buildContextDeck` internal. Use `matchingGroups(pairs).shuffled(random).take(1)` for matching, then replace the singles `when` with:

```kotlin
val allSources = pairs.map { it.word.source }.distinct()
singles.forEach { pair ->
    val card = if (pair.word.knowledgePercent >= ProductionThreshold) {
        val distractors = allSources
            .filter { !it.equals(pair.word.source, ignoreCase = true) }
            .shuffled(random)
            .take(3)
        distractors.takeIf { it.isNotEmpty() }?.let {
            ContextCard.Recognition(
                pair = pair,
                options = (it + pair.word.source).shuffled(random),
                direction = ContextDirection.UkToEn,
            )
        }
    } else {
        val distractors = pair.senseTranslations
            .filter { !it.equals(pair.word.translation, ignoreCase = true) }
            .shuffled(random)
            .take(3)
        distractors.takeIf { it.isNotEmpty() }?.let {
            ContextCard.Recognition(
                pair = pair,
                options = (it + pair.word.translation).shuffled(random),
                direction = ContextDirection.EnToUk,
            )
        }
    }
    if (card != null) cards += card
    if (cards.size >= ContextDeckSize) return@forEach
}
```

Keep the existing anti-repeat pass unchanged.

- [ ] **Step 6: Run deck and existing store tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.ContextPracticeDeckTest`

Expected: 4 tests pass.

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.VocabeeStoreTest`

Expected: existing context-pair tests still pass with the stricter count policy.

- [ ] **Step 7: Commit the deck simplification**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt app/src/test/java/com/vocabee/android/presentation/ContextPracticeDeckTest.kt
git commit -m "feat: simplify context practice deck"
```

---

### Task 4: Make setup cards and answer chips adaptive

**Files:**
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt:380-490,920-1010,1216-1385`

**Interfaces:**
- Consumes: recognition-only deck from Task 3.
- Produces: equal-height `PracticeModeCards`, tag row without format label, and fully visible adaptive answer chips.

- [ ] **Step 1: Equalize practice-mode card heights**

Use intrinsic row height and fill both children:

```kotlin
Row(
    modifier = modifier.fillMaxWidth().height(IntrinsicSize.Max),
    horizontalArrangement = Arrangement.spacedBy(11.dp),
) {
    PracticeModeCard(/* classic args */, modifier = Modifier.weight(1f).fillMaxHeight())
    PracticeModeCard(/* context args */, modifier = Modifier.weight(1f).fillMaxHeight())
}
```

Change subtitle text to:

```kotlin
Text(
    text = subtitle,
    modifier = Modifier.padding(top = 4.dp),
    color = PrototypeColor.Muted,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    minLines = 2,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
)
```

Use `Surface(onClick = onClick, ...)` for the card so its ripple is clipped.

- [ ] **Step 2: Remove format labels and all hints**

Replace `CardTagRow(direction, formatLabel)` with:

```kotlin
@Composable
private fun CardTagRow(direction: ContextDirection) {
    Row(modifier = Modifier.fillMaxWidth()) {
        DirectionBadge(direction)
    }
}
```

Update recognition calls to `CardTagRow(card.direction)`. Delete:

- the UK-to-EN `Підказка:` translation row;
- the incorrect-answer `ПІДКАЗКА` surface and confused sentence lookup;
- the unused `answeredCorrectly` local if no remaining branch reads it.

The left direction badge remains unchanged.

- [ ] **Step 3: Make `AnswerChip` wrap content and text**

Replace fixed height/fill constraints with:

```kotlin
Surface(
    onClick = onClick ?: {},
    enabled = onClick != null,
    modifier = modifier.heightIn(min = 48.dp),
    shape = RoundedCornerShape(24.dp),
    color = background,
    border = BorderStroke(1.6.dp, borderColor),
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Keep the existing optional state icon and 7 dp spacer.
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
    }
}
```

There is no `maxLines` and no `TextOverflow.Ellipsis`.

- [ ] **Step 4: Replace the fixed two-column loop with `FlowRow`**

```kotlin
BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        card.options.forEach { option ->
            val state = when {
                !answered -> ChipState.Idle
                option == correctAnswer -> ChipState.Correct
                option == chosen -> ChipState.Wrong
                else -> ChipState.Dim
            }
            AnswerChip(
                text = option,
                state = state,
                modifier = Modifier.widthIn(max = maxWidth),
                onClick = if (!answered) {
                    {
                        chosen = option
                        onAnswered(option == correctAnswer, option.takeIf { it != correctAnswer })
                    }
                } else {
                    null
                },
            )
        }
    }
}
```

Import `BoxWithConstraints`, `IntrinsicSize`, `fillMaxHeight`, `heightIn`, and `widthIn`. The full-row max constraint lets an oversized chip wrap to 2+ lines; FlowRow moves any chip that does not fit intact to the next row.

- [ ] **Step 5: Clip context `Далі` ripple and lower its bottom gap**

Change `NextButton` to `Surface(onClick = onClick, ...)` and remove the outer `clickable`. Keep its existing `Text` then chevron order. Change every context next-button bottom padding from 28 dp to 12 dp after `navigationBarsPadding()`.

- [ ] **Step 6: Compile the adaptive UI**

Run: `./gradlew :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`; experimental FlowRow opt-in remains satisfied by the file's existing annotations/imports.

- [ ] **Step 7: Commit adaptive context UI**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/ContextPractice.kt
git commit -m "fix: adapt context practice content"
```

---

### Task 5: Update practice docs and verify the complete flow

**Files:**
- Modify: `docs/01-screens.md`
- Modify: `docs/11-practice-training.md`
- Modify: `docs/12-motion-and-interaction-brief.md`

**Interfaces:**
- Documents Tasks 1-4; produces no code interface.

- [ ] **Step 1: Update living documentation**

Record these exact `[ЗАРАЗ]` behaviors:

```markdown
- Practice setup is a normal tab; running and result phases are full-screen.
- Close/system Back during an unfinished round opens the interruption sheet; result close returns directly to setup.
- Classic card uses all available content height, `Далі` is bottom-anchored with a trailing arrow, and rounded interactive surfaces clip ripple.
- Mode cards share height and reserve two ellipsized description lines.
- Context practice contains only recognition/UK→EN option selection and matching; recall/flip is removed.
- Context format labels and both hint forms are removed.
- Answer chips use adaptive whole-chip wrapping and never truncate text.
- Pairs without honest distractors are excluded from both counts and deck generation.
```

Delete the obsolete `40–59% — згадування` documentation and change the lower range to EN-to-UK recognition up to 59%. Keep ≥60% as UK-to-EN option selection without a translation hint.

- [ ] **Step 2: Run fresh unit and build verification**

Run: `./gradlew :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`, including policy, deck, and existing store tests.

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Perform Android visual and interaction checks**

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, then verify:

1. Setup shows the tab bar and equal-height mode cards with two-line description regions.
2. Starting classic hides the tab bar, shows the top-right close icon, expands the card, and anchors CTA above system navigation.
3. `Далі` renders text before its arrow; card/button ripple stays inside rounded bounds.
4. Close and system Back during a round show `Перервати тренування?`; `Продовжити` preserves the same card.
5. `Перервати` returns to setup with prior mode and dictionary selection.
6. Context practice never shows recall, `Знаю / Не знаю`, format labels, or hint panels.
7. A long answer moves to the next row and grows to multiple lines with its full text visible.
8. Completed results remain full-screen and close directly to setup.

Expected: all eight observations match the design specification.

- [ ] **Step 4: Commit documentation**

```bash
git add docs/01-screens.md docs/11-practice-training.md docs/12-motion-and-interaction-brief.md
git commit -m "docs: record focused practice sessions"
```
