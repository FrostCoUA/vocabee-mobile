package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Групування збережених записів ПО СЕНСУ (а не по слову-джерелу): один сенс —
 * одна картка. Ключ включає source, тож `run` і `naturally` ніколи не злипаються
 * навіть за однакового senseKey. Записи без атрибуції (легасі) лишаються в
 * окремій «безсенсовій» групі свого source — точно як у [groupBySourceWord].
 */
class SenseGroupingTest {
    private fun details(
        senseKeys: List<String> = emptyList(),
        senseIndex: Int? = null,
        senses: List<WordSense> = emptyList(),
    ) = WordDetails(senseKeys = senseKeys, senseIndex = senseIndex, senses = senses)

    private fun entry(
        id: String,
        source: String,
        translation: String,
        senseKeys: List<String> = emptyList(),
        knowledgePercent: Int = 0,
        details: WordDetails? = if (senseKeys.isEmpty()) null else details(senseKeys = senseKeys),
    ) = WordEntry(
        id = id,
        source = source,
        translation = translation,
        details = details,
        knowledgePercent = knowledgePercent,
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
    // тримаються однією легасі-групою (стара поведінка groupBySourceWord).
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

    @Test
    fun emptyListProducesNoGroups() {
        assertEquals(emptyList<WordGroup>(), emptyList<WordEntry>().groupBySense())
    }
}
