# Dictionary Quality Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the compact dictionary header and prevent incomplete lexicon results from creating empty saved-word detail cards.

**Architecture:** Put completeness and renderability rules in a focused domain-model policy file, and put exact-result recovery in a small use case that accepts a testable search function. Keep the existing `TopicDetail` route and Room model; presentation resolves a candidate before dispatching `AddWord`, while the store applies the same policy defensively and dictionary detail performs one safe legacy enrichment attempt per record.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, coroutines, JUnit 4, existing `RemoteLexiconSearchUseCase`, Room-backed `VocabularyRepository`.

## Global Constraints

- No backend endpoint, database schema, or Room migration changes.
- UI copy remains Ukrainian.
- Save-ready data requires source, translation, IPA, part of speech, a valid `senseIndex`, a non-blank definition, and a non-blank example.
- Forms, synonyms, and antonyms remain optional.
- Exact completion searches do not spend another user coin.
- Existing incomplete user records are never deleted automatically.
- The failure message is exactly `Не вдалося завантажити повну інформацію. Спробуйте ще раз.`
- Use `apply_patch` for all file edits.

## File map

- Create `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/model/TranslationQuality.kt`: the only source of truth for save readiness and details renderability.
- Create `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/usecase/ResolveTranslationForSaveUseCase.kt`: exact-query recovery and selected-translation matching.
- Create `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/DictionaryDetailLayout.kt`: pure compact-header positioning math.
- Create `app/src/test/java/com/vocabee/android/domain/TranslationQualityTest.kt`: policy coverage.
- Create `app/src/test/java/com/vocabee/android/domain/ResolveTranslationForSaveUseCaseTest.kt`: recovery coverage without a real API.
- Create `app/src/test/java/com/vocabee/android/presentation/DictionaryDetailLayoutTest.kt`: compact-title geometry coverage.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/VocabeeStore.kt`: defensive save boundary.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/AddWordOverlay.kt`: grouped-details selection uses renderable content.
- Modify `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt`: resolver wiring, one-shot legacy enrichment, error feedback, and measured compact-title alignment.
- Modify `app/src/test/java/com/vocabee/android/presentation/VocabeeStoreTest.kt`: reject incomplete adds and keep existing store tests on complete fixtures.
- Modify `docs/01-screens.md`, `docs/13-add-word-and-ai-search.md`, and `docs/14-word-details-and-audio.md`: living behavior documentation.

---

### Task 1: Define translation quality policies

**Files:**
- Create: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/model/TranslationQuality.kt`
- Test: `app/src/test/java/com/vocabee/android/domain/TranslationQualityTest.kt`

**Interfaces:**
- Consumes: `WordDetails`, `WordEntry`, and `TranslationOption` from `VocabularyModels.kt`.
- Produces: `WordDetails.hasRenderableContent: Boolean`, `isSaveReadyTranslation(source, translation, ipa, details): Boolean`, `TranslationOption.isSaveReady: Boolean`, and `WordEntry.isSaveReady: Boolean`.

- [ ] **Step 1: Write the failing policy tests**

```kotlin
package com.vocabee.android.domain

import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.domain.model.hasRenderableContent
import com.vocabee.android.feature.vocabulary.domain.model.isSaveReady
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationQualityTest {
    private val completeDetails = WordDetails(
        senseIndex = 0,
        senses = listOf(
            WordSense(
                definition = "to deceive someone",
                partOfSpeech = "verb",
                examples = listOf("They tried to cheat the system."),
            ),
        ),
        partOfSpeech = listOf("verb"),
    )

    private fun option(
        ipa: String? = "/tʃiːt/",
        details: WordDetails? = completeDetails,
    ) = TranslationOption(
        value = "обманювати",
        note = TranslationOptionNote.Primary,
        learningWord = "cheat",
        ipa = ipa,
        details = details,
    )

    @Test fun completeAttributedOptionIsSaveReady() {
        assertTrue(option().isSaveReady)
    }

    @Test fun missingRequiredFieldsAreRejected() {
        assertFalse(option(ipa = null).isSaveReady)
        assertFalse(option(details = completeDetails.copy(senseIndex = null)).isSaveReady)
        assertFalse(option(details = completeDetails.copy(partOfSpeech = emptyList(), senses = completeDetails.senses.map { it.copy(partOfSpeech = null) })).isSaveReady)
        assertFalse(option(details = completeDetails.copy(senses = completeDetails.senses.map { it.copy(definition = "") })).isSaveReady)
        assertFalse(option(details = completeDetails.copy(senses = completeDetails.senses.map { it.copy(examples = emptyList()) })).isSaveReady)
    }

    @Test fun metadataOnlyDetailsAreNotRenderable() {
        assertFalse(WordDetails(partOfSpeech = listOf("verb")).hasRenderableContent)
        assertTrue(completeDetails.hasRenderableContent)
    }
}
```

- [ ] **Step 2: Run the new test and verify the missing-policy failure**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.domain.TranslationQualityTest`

Expected: compilation fails because `hasRenderableContent` and `isSaveReady` do not exist.

- [ ] **Step 3: Add the minimal policy implementation**

```kotlin
package com.vocabee.android.feature.vocabulary.domain.model

val WordDetails.hasRenderableContent: Boolean
    get() = senses.any { sense ->
        sense.definition.isNotBlank() || sense.examples.any(String::isNotBlank)
    } || synonyms.any(String::isNotBlank) ||
        antonyms.any(String::isNotBlank) ||
        forms.any { it.text.isNotBlank() }

fun isSaveReadyTranslation(
    source: String,
    translation: String,
    ipa: String?,
    details: WordDetails?,
): Boolean {
    val rich = details ?: return false
    val ownSense = rich.senseIndex?.let(rich.senses::getOrNull) ?: return false
    val hasPartOfSpeech = rich.partOfSpeech.any(String::isNotBlank) ||
        !ownSense.partOfSpeech.isNullOrBlank()
    return source.isNotBlank() &&
        translation.isNotBlank() &&
        !ipa.isNullOrBlank() &&
        hasPartOfSpeech &&
        ownSense.definition.isNotBlank() &&
        ownSense.examples.any(String::isNotBlank)
}

val TranslationOption.isSaveReady: Boolean
    get() = isSaveReadyTranslation(learningWord, value, ipa, details)

val WordEntry.isSaveReady: Boolean
    get() = isSaveReadyTranslation(source, translation, ipa, details)
```

- [ ] **Step 4: Run the policy tests and verify green**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.domain.TranslationQualityTest`

Expected: `BUILD SUCCESSFUL`, 3 tests pass.

- [ ] **Step 5: Commit the policy**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/model/TranslationQuality.kt app/src/test/java/com/vocabee/android/domain/TranslationQualityTest.kt
git commit -m "feat: define complete translation policy"
```

---

### Task 2: Resolve incomplete selections through an exact query

**Files:**
- Create: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/usecase/ResolveTranslationForSaveUseCase.kt`
- Test: `app/src/test/java/com/vocabee/android/domain/ResolveTranslationForSaveUseCaseTest.kt`

**Interfaces:**
- Consumes: `TranslationOption.isSaveReady` from Task 1 and a suspend search function `(query, speakLang, learnLang) -> List<TranslationOption>?`.
- Produces: `ResolveTranslationForSaveUseCase.invoke(candidate, speakLang, learnLang): TranslationOption?`.

- [ ] **Step 1: Write failing resolver tests**

```kotlin
package com.vocabee.android.domain

import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.domain.usecase.ResolveTranslationForSaveUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveTranslationForSaveUseCaseTest {
    private fun complete(value: String = "обманювати") = TranslationOption(
        value = value,
        note = TranslationOptionNote.Primary,
        learningWord = "cheat",
        ipa = "/tʃiːt/",
        details = WordDetails(
            senseIndex = 0,
            senses = listOf(WordSense("to deceive", "verb", examples = listOf("Do not cheat."))),
            partOfSpeech = listOf("verb"),
        ),
    )

    @Test fun incompleteCandidateUsesExactSourceAndSelectedTranslation() = runBlocking {
        var searchedQuery = ""
        val resolver = ResolveTranslationForSaveUseCase { query, _, _ ->
            searchedQuery = query
            listOf(complete("шахраювати"), complete("обманювати"))
        }
        val lean = TranslationOption(
            value = "обманювати",
            note = TranslationOptionNote.Alternative,
            learningWord = "cheat",
            ipa = "/tʃiːt/",
            details = WordDetails(partOfSpeech = listOf("verb")),
        )

        val resolved = resolver(lean, speakLang = "uk", learnLang = "en")

        assertEquals("cheat", searchedQuery)
        assertEquals("обманювати", resolved?.value)
    }

    @Test fun incompleteExactMatchReturnsNull() = runBlocking {
        val resolver = ResolveTranslationForSaveUseCase { _, _, _ -> emptyList() }
        val lean = TranslationOption(
            value = "обманювати",
            note = TranslationOptionNote.Alternative,
            learningWord = "cheat",
        )

        assertNull(resolver(lean, speakLang = "uk", learnLang = "en"))
    }
}
```

- [ ] **Step 2: Run the resolver test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.domain.ResolveTranslationForSaveUseCaseTest`

Expected: compilation fails because `ResolveTranslationForSaveUseCase` does not exist.

- [ ] **Step 3: Implement exact matching**

```kotlin
package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.isSaveReady

class ResolveTranslationForSaveUseCase(
    private val exactSearch: suspend (String, String, String) -> List<TranslationOption>?,
) {
    suspend operator fun invoke(
        candidate: TranslationOption,
        speakLang: String,
        learnLang: String,
    ): TranslationOption? {
        if (candidate.isSaveReady) return candidate
        val source = candidate.learningWord.trim()
        if (source.isEmpty()) return null
        return exactSearch(source, speakLang, learnLang)
            .orEmpty()
            .firstOrNull { option ->
                option.learningWord.trim().equals(source, ignoreCase = true) &&
                    option.value.trim().equals(candidate.value.trim(), ignoreCase = true) &&
                    option.isSaveReady
            }
    }
}
```

- [ ] **Step 4: Run both domain tests**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.vocabee.android.domain.*'`

Expected: `BUILD SUCCESSFUL`, all translation-quality and resolver tests pass.

- [ ] **Step 5: Commit the resolver**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/domain/usecase/ResolveTranslationForSaveUseCase.kt app/src/test/java/com/vocabee/android/domain/ResolveTranslationForSaveUseCaseTest.kt
git commit -m "feat: enrich translations before saving"
```

---

### Task 3: Enforce completeness at the store boundary

**Files:**
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/VocabeeStore.kt:356-378`
- Modify: `app/src/test/java/com/vocabee/android/presentation/VocabeeStoreTest.kt:35-226`

**Interfaces:**
- Consumes: `isSaveReadyTranslation(...)` from Task 1.
- Produces: `VocabeeEvent.AddWord` becomes a no-op for incomplete candidates, irrespective of caller.

- [ ] **Step 1: Add a failing incomplete-add regression test and complete fixture helpers**

Add these helpers inside `VocabeeStoreTest`:

```kotlin
private fun completeDetails() = WordDetails(
    senseIndex = 0,
    senses = listOf(
        WordSense(
            definition = "test definition",
            partOfSpeech = "noun",
            examples = listOf("A complete test example."),
        ),
    ),
    partOfSpeech = listOf("noun"),
)

private fun VocabeeStore.addCompleteWord(
    topicId: String,
    source: String,
    translation: String,
) {
    onEvent(
        VocabeeEvent.AddWord(
            topicId = topicId,
            source = source,
            translation = translation,
            ipa = "/test/",
            details = completeDetails(),
        ),
    )
}
```

Add the regression test:

```kotlin
@Test
fun incompleteTranslationIsNotPersisted() {
    val store = VocabeeStore()
    val topic = store.createTopicForTest()

    store.onEvent(
        VocabeeEvent.AddWord(
            topicId = topic.id,
            source = "cheat",
            translation = "обманювати",
            ipa = "/tʃiːt/",
            details = WordDetails(partOfSpeech = listOf("verb")),
        ),
    )

    assertTrue(store.topicForTest(topic.id).words.isEmpty())
}
```

Replace the six existing minimal `VocabeeEvent.AddWord` fixtures at lines 40, 49, 111, 127, 208, and 221 with `addCompleteWord(topic.id, source, translation)`. Keep their original source and translation values unchanged; for the anonymous limit loop call `store.addCompleteWord(topic.id, "word-$index", "translation-$index")`.

- [ ] **Step 2: Run the store test and verify the regression is red**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.VocabeeStoreTest.incompleteTranslationIsNotPersisted`

Expected: FAIL because the incomplete word is currently stored.

- [ ] **Step 3: Add the guard before `AddWordUseCase`**

Import `isSaveReadyTranslation`, then add this immediately after trimmed source/translation validation:

```kotlin
if (!isSaveReadyTranslation(cleanedSource, cleanedTranslation, ipa, details)) return
```

Pass the already validated details without the old `takeUnless { it.isEmpty }` fallback:

```kotlin
val word = addWordUseCase(
    topicId = topicId,
    source = cleanedSource,
    translation = cleanedTranslation,
    ipa = ipa?.trim(),
    details = details,
)
```

- [ ] **Step 4: Run the complete store suite**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.VocabeeStoreTest`

Expected: `BUILD SUCCESSFUL`; duplicate, limits, knowledge, context, and new completeness tests all pass.

- [ ] **Step 5: Commit the boundary enforcement**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/VocabeeStore.kt app/src/test/java/com/vocabee/android/presentation/VocabeeStoreTest.kt
git commit -m "fix: reject incomplete saved translations"
```

---

### Task 4: Center the compact header using measured title height

**Files:**
- Create: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/DictionaryDetailLayout.kt`
- Test: `app/src/test/java/com/vocabee/android/presentation/DictionaryDetailLayoutTest.kt`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt:2940-3045`

**Interfaces:**
- Produces: `detailHeaderTitleTopPx(expandedTopPx, controlsTopPx, controlsHeightPx, titleHeightPx, collapseFraction): Int`.
- Consumes: the measured Compose `Text` height and current collapse fraction.

- [ ] **Step 1: Write the failing geometry test**

```kotlin
package com.vocabee.android.presentation

import com.vocabee.android.feature.vocabulary.presentation.detailHeaderTitleTopPx
import org.junit.Assert.assertEquals
import org.junit.Test

class DictionaryDetailLayoutTest {
    @Test fun expandedTitleKeepsExpandedAnchor() {
        assertEquals(100, detailHeaderTitleTopPx(100, 20, 40, 24, 0f))
    }

    @Test fun compactTitleCentersItsMeasuredHeightInControlsRow() {
        assertEquals(28, detailHeaderTitleTopPx(100, 20, 40, 24, 1f))
    }

    @Test fun titleInterpolatesBetweenAnchors() {
        assertEquals(64, detailHeaderTitleTopPx(100, 20, 40, 24, 0.5f))
    }
}
```

- [ ] **Step 2: Run the geometry test and verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.DictionaryDetailLayoutTest`

Expected: compilation fails because `detailHeaderTitleTopPx` is missing.

- [ ] **Step 3: Implement the pure geometry helper**

```kotlin
package com.vocabee.android.feature.vocabulary.presentation

import kotlin.math.roundToInt

internal fun detailHeaderTitleTopPx(
    expandedTopPx: Int,
    controlsTopPx: Int,
    controlsHeightPx: Int,
    titleHeightPx: Int,
    collapseFraction: Float,
): Int {
    val progress = collapseFraction.coerceIn(0f, 1f)
    val compactTop = controlsTopPx + ((controlsHeightPx - titleHeightPx) / 2f)
    return (expandedTopPx + (compactTop - expandedTopPx) * progress).roundToInt()
}
```

- [ ] **Step 4: Replace the magic `titleTop` offset in `DetailHeader`**

Add `var titleHeightPx by remember(topic.id) { mutableIntStateOf(0) }`. Convert the expanded title top, compact controls top, and 40 dp controls height with `LocalDensity`. Replace `.offset(y = titleTop)` with:

```kotlin
.onSizeChanged { titleHeightPx = it.height }
.offset {
    IntOffset(
        x = 0,
        y = detailHeaderTitleTopPx(
            expandedTopPx = with(density) { (statusBarTop + 62.dp).roundToPx() },
            controlsTopPx = with(density) { (statusBarTop + 2.dp).roundToPx() },
            controlsHeightPx = with(density) { 40.dp.roundToPx() },
            titleHeightPx = titleHeightPx,
            collapseFraction = progress,
        ),
    )
}
```

Remove the old `val titleTop = ...`. Import `IntOffset` and `onSizeChanged`. Preserve title horizontal interpolation, font interpolation, max lines, artwork, and subtitle behavior.

- [ ] **Step 5: Run the geometry test and compile the app**

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.DictionaryDetailLayoutTest`

Expected: 3 tests pass.

Run: `./gradlew :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the header fix**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/DictionaryDetailLayout.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt app/src/test/java/com/vocabee/android/presentation/DictionaryDetailLayoutTest.kt
git commit -m "fix: center collapsed dictionary title"
```

---

### Task 5: Wire exact recovery, legacy enrichment, and renderable details

**Files:**
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt:170-275,424-850,2310-2590,3060-3330`
- Modify: `app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/AddWordOverlay.kt:94-108,844-958`

**Interfaces:**
- Consumes: `ResolveTranslationForSaveUseCase`, `TranslationOption.isSaveReady`, `WordEntry.isSaveReady`, and `WordDetails.hasRenderableContent`.
- Produces: `DictionaryDetailScreen` parameters `resolveTranslationForSave: suspend (TranslationOption) -> TranslationOption?` and `onUpdateWordEnrichment: (wordId, ipa, details) -> Unit`.

- [ ] **Step 1: Add a resolver instance in `MainApp`**

Create it once per remote use case:

```kotlin
val translationForSaveResolver = remember(remoteLexiconSearch) {
    remoteLexiconSearch?.let { search ->
        ResolveTranslationForSaveUseCase { query, speakLang, learnLang ->
            (search(query, speakLang, learnLang, emptySet()) as? RemoteLexiconSearchUseCase.Result.Ok)
                ?.options
        }
    }
}
```

For `TopicDetail`, pass:

```kotlin
resolveTranslationForSave = { option ->
    translationForSaveResolver?.invoke(
        candidate = option,
        speakLang = topic.targetLanguage.code,
        learnLang = topic.sourceLanguage.code,
    ) ?: option.takeIf { it.isSaveReady }
},
onUpdateWordEnrichment = { wordId, ipa, details ->
    store.updateWordEnrichment(topic.id, wordId, ipa, details)
},
```

- [ ] **Step 2: Resolve every `+` action before persistence**

Extend `DictionaryDetailScreen` with the two parameters above. Replace its inline `onAdd` body with:

```kotlin
onAdd = { option ->
    scope.launch {
        val ready = resolveTranslationForSave(option)
        if (ready == null) {
            voiceSnackbarHostState.currentSnackbarData?.dismiss()
            voiceSnackbarHostState.showSnackbar(
                "Не вдалося завантажити повну інформацію. Спробуйте ще раз.",
            )
        } else {
            onAddWord(ready.learningWord, ready.value, ready.ipa, ready.details)
        }
    }
},
```

Do not call `onSpendSearchBee` from this recovery path: the initial debounced search already owns charging/gating.

- [ ] **Step 3: Add one legacy enrichment pass per detail-screen lifetime**

Inside `DictionaryDetailScreen`, add:

```kotlin
LaunchedEffect(topic.id) {
    topic.words.filterNot { it.isSaveReady }.forEach { word ->
        val candidate = TranslationOption(
            value = word.translation,
            note = TranslationOptionNote.Primary,
            learningWord = word.source,
            ipa = word.ipa,
            details = word.details,
        )
        resolveTranslationForSave(candidate)?.let { ready ->
            onUpdateWordEnrichment(word.id, ready.ipa, ready.details)
        }
    }
}
```

The `topic.id` key guarantees at most one pass for the screen lifetime; failed records remain visible and are not deleted.

- [ ] **Step 4: Replace metadata-based expansion checks with renderability**

Apply these exact policy replacements:

```kotlin
// AddWordOverlay.kt, WordGroup.details
get() = entries.firstNotNullOfOrNull { it.details?.takeIf(WordDetails::hasRenderableContent) }

// App.kt, WordGroupRow and WordRow
val hasDetails = details?.hasRenderableContent == true
```

Render `WordDetailsBlock` only under `expanded && details?.hasRenderableContent == true`. Make the same `hasDetails` and render guard change in the unused-but-maintained `TranslationOptionCard` in `AddWordOverlay.kt`, so future restoration of that overlay cannot recreate an empty shell.

- [ ] **Step 5: Run domain, store, and compilation checks**

Run: `./gradlew :app:testDebugUnitTest --tests 'com.vocabee.android.domain.*'`

Expected: all resolver/policy tests pass.

Run: `./gradlew :app:testDebugUnitTest --tests com.vocabee.android.presentation.VocabeeStoreTest`

Expected: all store tests pass.

Run: `./gradlew :app:compileDebugKotlin`

Expected: `BUILD SUCCESSFUL` with the new suspend callback wiring.

- [ ] **Step 6: Commit the presentation flow**

```bash
git add app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/App.kt app/src/commonMain/kotlin/com/vocabee/android/feature/vocabulary/presentation/AddWordOverlay.kt
git commit -m "fix: save only enriched dictionary entries"
```

---

### Task 6: Update dictionary docs and verify the deliverable

**Files:**
- Modify: `docs/01-screens.md`
- Modify: `docs/13-add-word-and-ai-search.md`
- Modify: `docs/14-word-details-and-audio.md`

**Interfaces:**
- Documents the behavior produced by Tasks 1-5; produces no code interface.

- [ ] **Step 1: Update living documentation**

Record these exact `[ЗАРАЗ]` behaviors:

```markdown
- Compact TopicDetail titles use the measured text height and center in the same 40 dp row as the controls.
- Prefix suggestions remain lean, but `+` exact-enriches the selected source/translation before add.
- Save-ready requires IPA, part of speech, attributed sense, definition, and example.
- Failed completion leaves the add panel open and stores nothing; completion does not charge again.
- Existing incomplete records receive one silent attempt per TopicDetail screen lifetime, are never auto-deleted, and cannot expand into an empty details block.
```

Update obsolete statements that say any non-empty `WordDetails` enables expansion or that saved details are never automatically enriched.

- [ ] **Step 2: Run fresh full client verification**

Run: `./gradlew :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`, all unit tests pass.

Run: `./gradlew :app:assembleDebug`

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Perform Android interaction checks**

Install the APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, then verify:

1. Collapse a dictionary detail header; title and compact controls share a visual center.
2. Add an exact complete result; it persists and expands with definition/example.
3. Tap `+` on a lean result while the gateway cannot return complete data; the Ukrainian error appears and no row is added.
4. Open a legacy metadata-only row; no blank details shell appears.

Expected: all four observations match the design specification.

- [ ] **Step 4: Commit documentation**

```bash
git add docs/01-screens.md docs/13-add-word-and-ai-search.md docs/14-word-details-and-audio.md
git commit -m "docs: record complete dictionary entry behavior"
```
