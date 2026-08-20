package com.vocabee.android.domain

import com.vocabee.android.feature.vocabulary.data.api.SearchExample
import com.vocabee.android.feature.vocabulary.data.api.SearchSense
import com.vocabee.android.feature.vocabulary.data.api.SearchVariant
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOptionNote
import com.vocabee.android.feature.vocabulary.domain.model.savedWordKey
import com.vocabee.android.feature.vocabulary.domain.usecase.toSenseGroupedOptions
import com.vocabee.android.feature.vocabulary.presentation.firstSenseLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Пошук показує ОДИН айтем на сенс: головний переклад + «близькі за значенням».
 * Групування йде тим самим правилом атрибуції, що й словник
 * (`senseMergeKeys`): стабільні `senseKeys`, інакше легасі `senseIndex`.
 * Неатрибутовані варіанти НЕ зливаються — кожен лишається окремим рядком.
 */
class SenseGroupedSearchResultsTest {
    private val senseRun = SearchSense(
        senseKey = "k1",
        definition = "рухатися швидко на ногах",
        examples = listOf(SearchExample("He runs every morning.")),
    )
    private val senseJog = SearchSense(
        senseKey = "k2",
        definition = "неспішний біг для здоров'я",
    )

    private fun variant(
        translation: String,
        senseKeys: List<String> = emptyList(),
        senses: List<SearchSense> = emptyList(),
        senseIndex: Int? = null,
        learningWord: String = "run",
        translationId: String = "t-$translation",
    ) = SearchVariant(
        translationId = translationId,
        knownWord = translation,
        learningWord = learningWord,
        senses = senses,
        senseKeys = senseKeys,
        senseIndex = senseIndex,
        source = "dictionary",
        origin = "lexicon-v2",
        isPrimary = false,
        cached = true,
    )

    /** Відповідь фази 0: атрибутований варіант несе ЛИШЕ свої сенси. */
    private fun fourVariants() = listOf(
        variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun)),
        variant("гнати", senseKeys = listOf("k1"), senses = listOf(senseRun)),
        variant("пробіжка", senseKeys = listOf("k2"), senses = listOf(senseJog)),
        variant("тривати", senses = listOf(senseRun, senseJog)),
    )

    @Test
    fun fourVariantsCollapseIntoThreeSenseItems() {
        val options = fourVariants().toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "пробіжка", "тривати"), options.map { it.value })

        val runGroup = options[0]
        assertEquals(listOf("гнати"), runGroup.alternatives.map { it.value })
        val runDetails = requireNotNull(runGroup.details)
        assertEquals(listOf("бігти", "гнати"), runDetails.senseGroupTranslations)
        assertEquals(listOf("k1"), runDetails.senses.mapNotNull { it.senseKey })

        val jogGroup = options[1]
        assertTrue(jogGroup.alternatives.isEmpty())
        // Група з одного перекладу не має «близьких» — список лишається порожнім.
        assertEquals(emptyList<String>(), requireNotNull(jogGroup.details).senseGroupTranslations)

        val legacy = options[2]
        assertTrue(legacy.alternatives.isEmpty())
        assertEquals(listOf("k1", "k2"), requireNotNull(legacy.details).senses.mapNotNull { it.senseKey })
    }

    @Test
    fun alternativeKeepsItsOwnIdentityAndTheGroupTranslations() {
        val alternative = fourVariants().toSenseGroupedOptions(emptySet())[0].alternatives.single()

        assertEquals("гнати", alternative.value)
        assertEquals("run", alternative.learningWord)
        assertEquals("t-гнати", alternative.translationId)
        assertEquals(listOf("бігти", "гнати"), requireNotNull(alternative.details).senseGroupTranslations)
        assertTrue(alternative.alternatives.isEmpty())
    }

    // Збережений `run→гнати` — сенс уже у словнику, тож ✓ отримує ГРУПА,
    // а не лише сам синонім.
    @Test
    fun savedAlternativeMarksTheWholeSenseGroupAsAdded() {
        val options = fourVariants().toSenseGroupedOptions(setOf(savedWordKey("run", "гнати")))

        assertTrue(options[0].alreadyAdded)
        assertTrue(options[0].note is TranslationOptionNote.AlreadyAdded)
        assertTrue(options[0].alternatives.single().alreadyAdded)
        assertFalse(options[1].alreadyAdded)
        assertFalse(options[2].alreadyAdded)
    }

    @Test
    fun savedPairOfAnotherSenseLeavesTheGroupUnmarked() {
        val options = fourVariants().toSenseGroupedOptions(setOf(savedWordKey("run", "пробіжка")))

        assertFalse(options[0].alreadyAdded)
        assertTrue(options[1].alreadyAdded)
    }

    @Test
    fun unattributedVariantsNeverMergeWithEachOther() {
        val options = listOf(variant("тривати"), variant("минати")).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("тривати", "минати"), options.map { it.value })
        assertTrue(options.all { it.alternatives.isEmpty() })
        assertTrue(options.all { it.details?.senseGroupTranslations.orEmpty().isEmpty() })
    }

    @Test
    fun sameSenseKeyUnderDifferentLearningWordsStaysApart() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun)),
            variant("природно", senseKeys = listOf("k1"), senses = listOf(senseRun), learningWord = "naturally"),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "природно"), options.map { it.value })
        assertTrue(options.all { it.alternatives.isEmpty() })
    }

    @Test
    fun legacySenseIndexAttributionGroupsPerIndex() {
        val senses = listOf(
            SearchSense(definition = "рухатися швидко"),
            SearchSense(definition = "серія показів"),
        )
        val options = listOf(
            variant("бігти", senseIndex = 0, senses = senses),
            variant("мчати", senseIndex = 0, senses = senses),
            variant("серія", senseIndex = 1, senses = senses),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "серія"), options.map { it.value })
        assertEquals(listOf("мчати"), options[0].alternatives.map { it.value })
    }

    // senseIndex поза межами senses = атрибуції фактично немає → без злиття.
    @Test
    fun senseIndexOutOfRangeDoesNotMergeAnything() {
        val senses = listOf(SearchSense(definition = "рухатися швидко"))
        val options = listOf(
            variant("бігти", senseIndex = 7, senses = senses),
            variant("мчати", senseIndex = 7, senses = senses),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "мчати"), options.map { it.value })
    }

    // I1 — реальні V2-дані (~1,6% headwords: strange/étrange, chicken, determine…):
    // ширший переклад несе ще один сенс. Якір — ГОЛОВНИЙ варіант групи: кандидат
    // зливається, якщо перетинається саме з ним.
    @Test
    fun candidateWithAnExtraSenseKeyMergesIntoTheAnchorGroup() {
        val options = listOf(
            variant("дивний", senseKeys = listOf("k1"), senses = listOf(senseRun)),
            variant("чудернацький", senseKeys = listOf("k1", "k2"), senses = listOf(senseRun, senseJog)),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("дивний"), options.map { it.value })
        assertEquals(listOf("чудернацький"), options.single().alternatives.map { it.value })
        assertEquals(
            listOf("дивний", "чудернацький"),
            requireNotNull(options.single().details).senseGroupTranslations,
        )
    }

    @Test
    fun disjointSenseKeySetsStayInSeparateGroups() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun)),
            variant("пробіжка", senseKeys = listOf("k2", "k3"), senses = listOf(senseJog)),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "пробіжка"), options.map { it.value })
        assertTrue(options.all { it.alternatives.isEmpty() })
    }

    // Якір не розширюється прийнятими кандидатами: `[k2]` не приклеюється до
    // групи `[k1]` лише тому, що там уже лежить місток `[k1,k2]`.
    @Test
    fun anchorMergeIsNotTransitive() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun)),
            variant("мчати", senseKeys = listOf("k1", "k2"), senses = listOf(senseRun, senseJog)),
            variant("пробіжка", senseKeys = listOf("k2"), senses = listOf(senseJog)),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "пробіжка"), options.map { it.value })
        assertEquals(listOf("мчати"), options[0].alternatives.map { it.value })
    }

    // M3 — префіксна видача: `senseKeys` є, а в самих sense-об'єктах ключів немає
    // (старіший рядок лексикону). Групування працює, підпису сенсу не буде.
    @Test
    fun senseKeysWithoutMatchingSenseObjectsGroupButShowNoSenseLine() {
        val anonymousSense = SearchSense(definition = "рухатися швидко на ногах")
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(anonymousSense)),
            variant("гнати", senseKeys = listOf("k1"), senses = listOf(anonymousSense)),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("гнати"), options.single().alternatives.map { it.value })
        assertEquals(null, requireNotNull(options.single().details).firstSenseLine())
    }

    // M5 — з двох однакових перекладів виграє той, у кого є `translationId`:
    // саме він дає скаргу «неякісний переклад» і зв'язок із лексиконом.
    @Test
    fun duplicateTranslationPrefersTheVariantWithTranslationId() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun), translationId = ""),
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun), translationId = "t-curated"),
        ).toSenseGroupedOptions(emptySet())

        assertEquals("t-curated", options.single().translationId)
        assertTrue(options.single().alternatives.isEmpty())
    }

    @Test
    fun duplicateTranslationsInsideOneSenseCollapse() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), senses = listOf(senseRun)),
            variant(" Бігти ", senseKeys = listOf("k1"), senses = listOf(senseRun)),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти"), options.map { it.value })
        assertTrue(options.single().alternatives.isEmpty())
        assertEquals(emptyList<String>(), requireNotNull(options.single().details).senseGroupTranslations)
    }

    // Голий варіант без сенсів і без id: раніше `details` відкидались як порожні,
    // але список групи — це вже вміст, що вартий збереження.
    @Test
    fun groupTranslationsSurviveOnVariantsWithoutAnyOtherDetails() {
        val options = listOf(
            variant("бігти", senseKeys = listOf("k1"), translationId = ""),
            variant("гнати", senseKeys = listOf("k1"), translationId = ""),
        ).toSenseGroupedOptions(emptySet())

        assertEquals(listOf("бігти", "гнати"), requireNotNull(options.single().details).senseGroupTranslations)
        assertEquals(listOf("k1"), requireNotNull(options.single().details).senseKeys)
    }

}
