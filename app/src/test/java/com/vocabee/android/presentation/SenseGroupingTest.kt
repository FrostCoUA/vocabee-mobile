package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import com.vocabee.android.feature.vocabulary.domain.model.senseMergeKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Групування збережених записів ПО СЕНСУ (а не по слову-джерелу): один сенс —
 * одна картка. Ключ включає source, тож `run` і `naturally` ніколи не злипаються
 * навіть за однакового senseKey. Записи без атрибуції (легасі) лишаються в
 * окремій «безсенсовій» групі свого source — тобто в старій, по-словній групі.
 */
class SenseGroupingTest {
    /**
     * Склад групи в порядку записів — читабельний вигляд для ассертів. Живе в
     * тесті, а не на [WordGroup]: продакшн рендерить пару представника й
     * `nearbyTranslations`, «плаский» список перекладів там нікому не потрібен.
     */
    private val WordGroup.translations: List<String> get() = entries.map { it.translation }

    private fun details(
        senseKeys: List<String> = emptyList(),
        senseIndex: Int? = null,
        senses: List<WordSense> = emptyList(),
        senseGroupTranslations: List<String> = emptyList(),
    ) = WordDetails(
        senseKeys = senseKeys,
        senseIndex = senseIndex,
        senses = senses,
        senseGroupTranslations = senseGroupTranslations,
    )

    private fun entry(
        id: String,
        source: String,
        translation: String,
        senseKeys: List<String> = emptyList(),
        knowledgePercent: Int = 0,
        addedAt: Long = 0L,
        details: WordDetails? = if (senseKeys.isEmpty()) null else details(senseKeys = senseKeys),
    ) = WordEntry(
        id = id,
        source = source,
        translation = translation,
        details = details,
        knowledgePercent = knowledgePercent,
        addedAtEpochMillis = addedAt,
    )

    // (а)
    @Test
    fun entriesSharingSenseKeyLandInOneGroup() {
        val groups = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "run", "мчати", senseKeys = listOf("k1")),
        ).groupBySense()

        assertEquals(1, groups.size)
        assertEquals("run", groups.single().sourceWord)
        assertEquals(listOf("бігти", "мчати"), groups.single().translations)
    }

    // (б)
    @Test
    fun differentSenseKeysStayInDifferentGroups() {
        val groups = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "run", "серія", senseKeys = listOf("k2")),
        ).groupBySense()

        assertEquals(listOf(listOf("бігти"), listOf("серія")), groups.map { it.translations })
    }

    // (в) — безатрибутивні записи не зливаються з атрибутованим, але між собою
    // тримаються однією легасі-групою (стара, по-словна поведінка).
    @Test
    fun unattributedEntriesFormOneLegacyGroupApartFromAttributedOne() {
        val groups = listOf(
            entry("1", "run", "серія"),
            entry("2", "run", "керувати"),
            entry("3", "run", "бігти", senseKeys = listOf("k1")),
        ).groupBySense()

        assertEquals(2, groups.size)
        assertEquals(listOf("серія", "керувати"), groups[0].translations)
        assertEquals(listOf("бігти"), groups[1].translations)
    }

    @Test
    fun translationIdOnlyRowsRemainSeparateFromEachOtherAndLegacyBucket() {
        val groups = listOf(
            entry("id-1", "run", "бігти", details = WordDetails(translationId = "translation-1")),
            entry("id-2", "run", "бігти", details = WordDetails(translationId = "translation-2")),
            entry("legacy", "run", "керувати"),
        ).groupBySense()

        assertEquals(listOf(listOf("id-1"), listOf("id-2"), listOf("legacy")),
            groups.map { group -> group.entries.map(WordEntry::id) })
        assertEquals(3, groups.map(WordGroup::stableKey).distinct().size)
    }

    // (г) — частковий перетин ключів: ревізія лексикону могла додати сенс,
    // збережені раніше записи мусять лишитись у тій самій групі.
    @Test
    fun partiallyOverlappingSenseKeysMergeIntoOneGroup() {
        val groups = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "run", "мчати", senseKeys = listOf("k1", "k2")),
        ).groupBySense()

        assertEquals(1, groups.size)
        assertEquals(listOf("бігти", "мчати"), groups.single().translations)
    }

    // (г²) — перетин транзитивний: [k1] і [k2] окремі, доки не прийде [k1,k2].
    @Test
    fun bridgingEntryUnitesPreviouslySeparateGroups() {
        val groups = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "run", "серія", senseKeys = listOf("k2")),
            entry("3", "run", "мчати", senseKeys = listOf("k1", "k2")),
        ).groupBySense()

        assertEquals(1, groups.size)
        assertEquals(listOf("бігти", "серія", "мчати"), groups.single().translations)
    }

    // (д)
    @Test
    fun groupOrderFollowsFirstOccurrence() {
        val groups = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "naturally", "природно", senseKeys = listOf("k9")),
            entry("3", "run", "серія", senseKeys = listOf("k2")),
            entry("4", "run", "мчати", senseKeys = listOf("k1")),
        ).groupBySense()

        assertEquals(listOf("run", "naturally", "run"), groups.map { it.sourceWord })
        assertEquals(listOf("1", "4"), groups[0].entries.map { it.id })
        assertEquals(listOf("3"), groups[2].entries.map { it.id })
    }

    // (е)
    @Test
    fun sameSenseKeyUnderDifferentSourceWordsNeverMixes() {
        val entries = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "naturally", "природно", senseKeys = listOf("k1")),
        )

        assertNotEquals(entries[0].senseGroupKey(), entries[1].senseGroupKey())
        assertEquals(2, entries.groupBySense().size)
    }

    @Test
    fun senseGroupKeyIgnoresCaseAndPaddingAndFallsBackToSourceWord() {
        assertEquals(entry("1", "  Run ", "бігти").senseGroupKey(), entry("2", "run", "серія").senseGroupKey())
        assertEquals("run", entry("1", " RUN ", "бігти").senseGroupKey())
        assertEquals(
            entry("1", "Run", "бігти", senseKeys = listOf("k1", "k2")).senseGroupKey(),
            entry("2", "run", "мчати", senseKeys = listOf("k2", "k1")).senseGroupKey(),
        )
        assertNotEquals(
            entry("1", "run", "бігти", senseKeys = listOf("k1")).senseGroupKey(),
            entry("2", "run", "серія").senseGroupKey(),
        )
    }

    // Легасі-атрибуція без стабільних ключів — по senseIndex.
    @Test
    fun legacySenseIndexAttributionGroupsPerIndex() {
        val senses = listOf(
            WordSense(definition = "to move fast"),
            WordSense(definition = "a series of episodes"),
        )
        val groups = listOf(
            entry("1", "run", "бігти", details = details(senseIndex = 0, senses = senses)),
            entry("2", "run", "мчати", details = details(senseIndex = 0, senses = senses)),
            entry("3", "run", "серія", details = details(senseIndex = 1, senses = senses)),
        ).groupBySense()

        assertEquals(listOf(listOf("бігти", "мчати"), listOf("серія")), groups.map { it.translations })
    }

    @Test
    fun groupExposesWeakestKnowledgeAndNewestRepresentative() {
        val newest = entry("1", "run", "бігти", senseKeys = listOf("k1"), knowledgePercent = 80)
        val older = entry("2", "run", "мчати", senseKeys = listOf("k1"), knowledgePercent = 20)

        val group = listOf(newest, older).groupBySense().single()

        assertEquals(20, group.minKnowledgePercent)
        assertEquals(50, group.knowledgePercent)
        assertSame(newest, group.representative)
    }

    // M3 — представник не покладається на порядок входу.
    @Test
    fun representativeIsTheNewestEntryRegardlessOfInputOrder() {
        val older = entry("1", "run", "мчати", senseKeys = listOf("k1"), addedAt = 100L)
        val newest = entry("2", "run", "бігти", senseKeys = listOf("k1"), addedAt = 900L)

        assertSame(newest, listOf(older, newest).groupBySense().single().representative)
        assertSame(newest, listOf(newest, older).groupBySense().single().representative)
    }

    // M4 (а) — атрибутований senseIndex=0 і повна відсутність атрибуції — різні речі.
    @Test
    fun legacySenseIndexZeroDoesNotMergeWithUnattributedEntry() {
        val senses = listOf(WordSense(definition = "to move fast"))
        val groups = listOf(
            entry("1", "run", "бігти", details = details(senseIndex = 0, senses = senses)),
            entry("2", "run", "серія"),
        ).groupBySense()

        assertEquals(listOf(listOf("бігти"), listOf("серія")), groups.map { it.translations })
    }

    // M4 (б) — senseIndex поза межами senses: атрибуції фактично немає → легасі-група.
    @Test
    fun senseIndexOutOfRangeFallsBackToTheLegacyBucket() {
        val groups = listOf(
            entry("1", "run", "серія"),
            entry("2", "run", "бігти", details = details(senseIndex = 7, senses = listOf(WordSense(definition = "to move fast")))),
        ).groupBySense()

        assertEquals(1, groups.size)
        assertEquals(listOf("серія", "бігти"), groups.single().translations)
    }

    // I1 — дедуп збереження (Task 4) мусить іти через ПЕРЕТИН, не через рівність ключів.
    @Test
    fun overlapsSenseGroupMatchesRevisedCandidateWithExtraSenseKey() {
        val saved = entry("1", "run", "бігти", senseKeys = listOf("k1"))
        val candidate = details(senseKeys = listOf("k1", "k2"))

        // саме та пастка, від якої страхує хелпер: ключі різні, сенс спільний
        assertNotEquals(saved.senseGroupKey(), entry("2", "run", "мчати", details = candidate).senseGroupKey())
        assertTrue(saved.overlapsSenseGroup("run", candidate))
    }

    @Test
    fun overlapsSenseGroupSeparatesDifferentSenseKeysAndSourceWords() {
        val saved = entry("1", "run", "бігти", senseKeys = listOf("k1"))

        assertFalse(saved.overlapsSenseGroup("run", details(senseKeys = listOf("k2"))))
        assertFalse(saved.overlapsSenseGroup("naturally", details(senseKeys = listOf("k1"))))
        assertTrue(saved.overlapsSenseGroup(" RUN ", details(senseKeys = listOf("k1"))))
    }

    @Test
    fun overlapsSenseGroupKeepsLegacyApartFromAttributedCandidate() {
        val legacySaved = entry("1", "run", "серія")

        assertFalse(legacySaved.overlapsSenseGroup("run", details(senseKeys = listOf("k1"))))
        assertFalse(entry("2", "run", "бігти", senseKeys = listOf("k1")).overlapsSenseGroup("run", null))
        // обидва без атрибуції — той самий легасі-фолбек
        assertTrue(legacySaved.overlapsSenseGroup("run", null))
        assertTrue(legacySaved.overlapsSenseGroup("run", details()))
    }

    @Test
    fun overlapsSenseGroupUsesTheSameKeysAsGrouping() {
        val saved = entry("1", "run", "бігти", senseKeys = listOf("k1"))

        assertEquals(setOf("k1"), saved.senseMergeKeys())
        assertEquals(setOf("k1", "k2"), details(senseKeys = listOf("k1", "k2")).senseMergeKeys())
        assertEquals(emptySet<String>(), (null as WordDetails?).senseMergeKeys())
    }

    @Test
    fun emptyListProducesNoGroups() {
        assertEquals(emptyList<WordGroup>(), emptyList<WordEntry>().groupBySense())
    }

    // ── Формування списку словника: картка = збережений сенс ─────────────────

    /**
     * Словник, який бачить користувач: дев'ять рядків `run` (три сенсу k1, два
     * сенсу k2, чотири легасі без атрибуції) і одне окреме слово. Порядок —
     * такий, як приходить зі стану словника.
     */
    private fun dictionaryWithNineRunRows(): List<WordEntry> = listOf(
        entry("1", "run", "бігти", senseKeys = listOf("k1")),
        entry("2", "run", "мчати", senseKeys = listOf("k1")),
        entry("3", "run", "гнати", senseKeys = listOf("k1")),
        entry("4", "run", "серія", senseKeys = listOf("k2")),
        entry("5", "run", "сезон", senseKeys = listOf("k2")),
        entry("6", "run", "керувати"),
        entry("7", "run", "запуск"),
        entry("8", "run", "пробіжка"),
        entry("9", "run", "тираж"),
        entry("10", "naturally", "природно", senseKeys = listOf("k9")),
    )

    @Test
    fun dictionaryListCollapsesRunRowsIntoOneCardPerSense() {
        val groups = dictionaryWithNineRunRows().groupBySense()

        assertEquals(listOf("run", "run", "run", "naturally"), groups.map { it.sourceWord })
        assertEquals(
            listOf(
                listOf("бігти", "мчати", "гнати"),
                listOf("серія", "сезон"),
                // Легасі-рядки одного source лишаються ОДНІЄЮ спільною карткою.
                listOf("керувати", "запуск", "пробіжка", "тираж"),
                listOf("природно"),
            ),
            groups.map { it.translations },
        )
        // Заголовок картки — пара представника; решта йде другим рядком.
        assertEquals(listOf("бігти", "серія", "керувати", "природно"), groups.map { it.representative.translation })
        assertEquals(listOf("мчати", "гнати"), groups[0].nearbyTranslations)
        assertEquals(listOf("запуск", "пробіжка", "тираж"), groups[2].nearbyTranslations)
        assertEquals(emptyList<String>(), groups[3].nearbyTranslations)
    }

    // I1 — «близькі за значенням» чесні лише для атрибутованої групи: легасі-бакет
    // тримає РІЗНІ значення одного слова (керувати/запуск/тираж), близькими їх
    // називати не можна.
    @Test
    fun nearbyLabelIsSemanticOnlyForAnAttributedGroup() {
        val groups = dictionaryWithNineRunRows().groupBySense()

        assertEquals("Близькі за значенням", nearbyTranslationsLabel(groups[0]))
        assertEquals("Інші переклади", nearbyTranslationsLabel(groups[2]))
    }

    // Легасі-атрибуція по senseIndex — це все ще ОДИН сенс, мітка семантична.
    @Test
    fun nearbyLabelStaysSemanticForLegacySenseIndexAttribution() {
        val senses = listOf(WordSense(definition = "to move fast"))
        val group = listOf(
            entry("1", "run", "бігти", details = details(senseIndex = 0, senses = senses)),
            entry("2", "run", "мчати", details = details(senseIndex = 0, senses = senses)),
        ).groupBySense().single()

        assertEquals("Близькі за значенням", nearbyTranslationsLabel(group))
    }

    // M4 — ключ рендера тримається на ІДЕНТИЧНОСТІ сенсу, а не на найновішому
    // записі: доданий переклад того самого сенсу не скидає розгорнутий стан.
    @Test
    fun stableKeyIdentifiesTheSenseNotTheNewestMember() {
        val groups = dictionaryWithNineRunRows().groupBySense()

        assertEquals(
            listOf("run\u0000k1", "run\u0000k2", "run\u0000legacy", "naturally\u0000k9"),
            groups.map { it.stableKey },
        )
        assertEquals(4, groups.mapTo(mutableSetOf()) { it.stableKey }.size)

        val before = listOf(entry("1", "run", "бігти", senseKeys = listOf("k1"), addedAt = 100L)).groupBySense().single()
        val after = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1"), addedAt = 100L),
            entry("2", "run", "мчати", senseKeys = listOf("k1"), addedAt = 900L),
        ).groupBySense().single()

        assertEquals(before.stableKey, after.stableKey)
        assertNotEquals(before.representative.id, after.representative.id)
    }

    @Test
    fun stableKeyIgnoresKeyOrderAndSourceCase() {
        assertEquals(
            listOf(entry("1", " Run ", "бігти", senseKeys = listOf("k2", "k1"))).groupBySense().single().stableKey,
            listOf(entry("2", "run", "мчати", senseKeys = listOf("k1", "k2"))).groupBySense().single().stableKey,
        )
    }

    @Test
    fun headerCounterCountsSensesAndTranslations() {
        val words = dictionaryWithNineRunRows()

        assertEquals("4 слова · 10 перекладів", senseCountLabel(words.groupBySense().size, words.size))
    }

    // Немає жодної мультиперекладної групи → друга частина лічильника зайва.
    @Test
    fun headerCounterDropsTranslationsPartWhenEveryCardIsOneRow() {
        val words = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1")),
            entry("2", "naturally", "природно", senseKeys = listOf("k9")),
        )

        assertEquals("2 слова", senseCountLabel(words.groupBySense().size, words.size))
        assertEquals("0 слів", senseCountLabel(0, 0))
    }

    @Test
    fun headerCounterUsesUkrainianPlurals() {
        assertEquals("1 слово · 3 переклади", senseCountLabel(1, 3))
        assertEquals("2 слова · 11 перекладів", senseCountLabel(2, 11))
        assertEquals("5 слів · 21 переклад", senseCountLabel(5, 21))
    }

    // Авторитет «близьких» — збережені записи; підказка лише доповнює їх.
    @Test
    fun nearbyTranslationsPreferSavedEntriesAndOnlySupplementWithTheHint() {
        val group = listOf(
            entry(
                "1",
                "run",
                "бігти",
                details = details(senseKeys = listOf("k1"), senseGroupTranslations = listOf("бігти", "мчати", "гнати")),
            ),
            entry("2", "run", "мчати", senseKeys = listOf("k1")),
        ).groupBySense().single()

        assertEquals("бігти", group.representative.translation)
        assertEquals(listOf("мчати", "гнати"), group.nearbyTranslations)
    }

    // Збережений один переклад сенсу — «близькі» тримаються на підказці, і
    // власний переклад представника з неї викидається (він у ній теж є).
    @Test
    fun singleSavedTranslationStillShowsHintedNeighboursWithoutItself() {
        val group = listOf(
            entry(
                "1",
                "run",
                "бігти",
                details = details(senseKeys = listOf("k1"), senseGroupTranslations = listOf("Бігти ", "гнати")),
            ),
        ).groupBySense().single()

        assertEquals(listOf("гнати"), group.nearbyTranslations)
    }

    // Канонічний синк стер підказку — «близькі» лишаються з самих записів.
    @Test
    fun nearbyTranslationsSurviveWithoutTheHint() {
        val group = listOf(
            entry("1", "run", "бігти", senseKeys = listOf("k1"), addedAt = 900L),
            entry("2", "run", "мчати", senseKeys = listOf("k1"), addedAt = 100L),
        ).groupBySense().single()

        assertEquals(listOf("мчати"), group.nearbyTranslations)
    }

    // I2 — фолбек деталей не сміє тягти блоб ЧУЖОГО сенсу через запис-місток:
    // під заголовком k1 не місце сенсам, які прийшли лише з k2.
    @Test
    fun detailsFallbackSkipsMembersOutsideTheRepresentativeSense() {
        val bridged = details(senseKeys = listOf("k1", "k2"), senses = listOf(WordSense(definition = "to move fast")))
        val foreign = details(senseKeys = listOf("k2"), senses = listOf(WordSense(definition = "a series of episodes")))
        val group = listOf(
            entry("1", "run", "бігти", details = details(senseKeys = listOf("k1")), addedAt = 900L),
            entry("2", "run", "серія", details = foreign, addedAt = 100L),
            entry("3", "run", "мчати", details = bridged, addedAt = 200L),
        ).groupBySense().single()

        assertEquals("бігти", group.representative.translation)
        assertSame(bridged, group.displayDetails)
    }

    // Легасі-представник без атрибуції обмежувати нічим — фолбек як раніше.
    @Test
    fun detailsFallbackStaysUnrestrictedForAnUnattributedRepresentative() {
        val legacyDetails = details(senses = listOf(WordSense(definition = "to move fast")))
        val group = listOf(
            entry("1", "run", "керувати", addedAt = 900L),
            entry("2", "run", "запуск", details = legacyDetails, addedAt = 100L),
        ).groupBySense().single()

        assertSame(legacyDetails, group.displayDetails)
    }

    // Розгорнута картка показує деталі ПРЕДСТАВНИКА (його ж пара в заголовку),
    // а якщо їх нема — перші наявні в групі того самого сенсу.
    @Test
    fun expandedCardTakesRepresentativeDetailsWithGroupFallback() {
        val representativeDetails = details(senseKeys = listOf("k1"), senses = listOf(WordSense(definition = "to move fast")))
        val withRepresentative = listOf(
            entry("1", "run", "бігти", details = representativeDetails, addedAt = 900L),
            entry("2", "run", "мчати", details = details(senseKeys = listOf("k1")), addedAt = 100L),
        ).groupBySense().single()
        assertSame(representativeDetails, withRepresentative.displayDetails)

        val withoutRepresentative = listOf(
            entry("1", "run", "бігти", details = details(senseKeys = listOf("k1")), addedAt = 900L),
            entry("2", "run", "мчати", details = representativeDetails, addedAt = 100L),
        ).groupBySense().single()
        assertSame(representativeDetails, withoutRepresentative.displayDetails)
    }
}
