package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Колода класичного тренування: одиниця — СЕНС-ГРУПА, а не рядок словника.
 * Три збережені переклади одного значення дають одну картку (інакше юзер тричі
 * поспіль відповідає на те саме), відбір іде по найслабшому члену групи, а
 * дельта знань за відповідь роздається всім її записам.
 */
class PracticeSenseDeckTest {
    private fun entry(
        id: String,
        source: String,
        translation: String,
        senseKeys: List<String> = emptyList(),
        knowledgePercent: Int = 0,
    ) = WordEntry(
        id = id,
        source = source,
        translation = translation,
        details = if (senseKeys.isEmpty()) null else WordDetails(senseKeys = senseKeys),
        knowledgePercent = knowledgePercent,
    )

    /** Ключ картки колоди: словник + ідентичність СЕНСУ ([WordGroup.stableKey]). */
    private fun cardKey(source: String, sense: String): String = "topic:$source\u0000$sense"

    private fun testTopic(words: List<WordEntry>): DictionaryTopic = DictionaryTopic(
        id = "topic",
        title = "Test",
        sourceLanguage = LanguageOption(code = "en", name = "English", shortName = "EN", speechTag = "en-US"),
        targetLanguage = LanguageOption(code = "uk", name = "Українська", shortName = "UK", speechTag = "uk-UA"),
        words = words,
    )

    /** Той самий словник, що й у [SenseGroupingTest]: 9 рядків `run` + окремі слова. */
    private fun nineRunRows(): List<WordEntry> = listOf(
        entry("run-1", "run", "бігти", senseKeys = listOf("k1")),
        entry("run-2", "run", "мчати", senseKeys = listOf("k1")),
        entry("run-3", "run", "гнати", senseKeys = listOf("k1")),
        entry("run-4", "run", "серія", senseKeys = listOf("k2")),
        entry("run-5", "run", "сезон", senseKeys = listOf("k2")),
        entry("run-6", "run", "керувати"),
        entry("run-7", "run", "запуск"),
        entry("run-8", "run", "пробіжка"),
        entry("run-9", "run", "тираж"),
    )

    // (а)
    @Test
    fun deckHoldsOneCardPerSenseGroupInsteadOfOnePerRow() {
        val topic = testTopic(
            words = nineRunRows() + listOf(
                entry("nat-1", "naturally", "природно", senseKeys = listOf("k9")),
                entry("bee-1", "bee", "бджола"),
                entry("hive-1", "hive", "вулик"),
            ),
        )

        val cards = buildPracticeDeckCards(listOf(topic))

        assertEquals(12, topic.words.size)
        assertEquals(
            listOf(
                cardKey("run", "k1"),
                cardKey("run", "k2"),
                cardKey("run", "legacy"),
                cardKey("naturally", "k9"),
                cardKey("bee", "legacy"),
                cardKey("hive", "legacy"),
            ),
            cards.map { it.key },
        )
        val k1 = cards.single { it.key == cardKey("run", "k1") }
        assertEquals(listOf("run-1", "run-2", "run-3"), k1.memberWordIds)
        assertEquals("бігти", k1.word.translation)
        assertEquals(listOf("мчати", "гнати"), k1.extraTranslations)

        val deckKeys = buildPracticeDeckKeys(
            candidates = cards.map { card -> card.key to card.knowledgePercent },
            seed = 11,
        )
        assertTrue(deckKeys.size <= 10)
        // Двох карток одного сенсу в раунді бути не може.
        assertEquals(deckKeys.size, deckKeys.toSet().size)
        assertEquals(1, deckKeys.count { it == k1.key })
    }

    // (б)
    @Test
    fun deckRanksSenseGroupByItsWeakestMember() {
        val topic = testTopic(
            words = listOf(
                entry("run-1", "run", "бігти", senseKeys = listOf("k1"), knowledgePercent = 80),
                entry("run-2", "run", "мчати", senseKeys = listOf("k1"), knowledgePercent = 0),
                entry("run-3", "run", "гнати", senseKeys = listOf("k1"), knowledgePercent = 60),
                entry("bee-1", "bee", "бджола", knowledgePercent = 40),
            ),
        )

        val cards = buildPracticeDeckCards(listOf(topic))

        val group = cards.single { it.key == cardKey("run", "k1") }
        // Мінімум, а не середнє (47): слабкий синонім не ховається за сильним.
        assertEquals(0, group.knowledgePercent)
        assertEquals(
            listOf(cardKey("run", "k1"), cardKey("bee", "legacy")),
            buildPracticeDeckKeys(
                candidates = cards.map { card -> card.key to card.knowledgePercent },
                seed = 3,
            ),
        )
    }

    // (в)
    @Test
    fun knownAnswerRaisesEveryMemberOfTheSenseGroup() {
        val store = VocabeeStore()
        store.replaceSyncSnapshot(
            userKey = DEFAULT_LOCAL_USER_KEY,
            snapshot = VocabularySyncSnapshot(
                topics = listOf(
                    testTopic(
                        words = listOf(
                            entry("run-1", "run", "бігти", senseKeys = listOf("k1")),
                            entry("run-2", "run", "мчати", senseKeys = listOf("k1")),
                            entry("run-3", "run", "гнати", senseKeys = listOf("k1")),
                        ),
                    ),
                ),
            ),
        )
        val card = buildPracticeDeckCards(store.state.topics).single()
        assertEquals(listOf("run-1", "run-2", "run-3"), card.memberWordIds)

        applyPracticeAnswer(card, KnowledgeStepPercent) { topicId, wordId, deltaPercent ->
            store.onEvent(VocabeeEvent.AdjustWordKnowledge(topicId, wordId, deltaPercent))
        }

        assertEquals(
            List(3) { KnowledgeStepPercent },
            store.state.topics.single().words.map { it.knowledgePercent },
        )
    }

    @Test
    fun unknownAnswerLowersEveryMemberOfTheSenseGroup() {
        val store = VocabeeStore()
        store.replaceSyncSnapshot(
            userKey = DEFAULT_LOCAL_USER_KEY,
            snapshot = VocabularySyncSnapshot(
                topics = listOf(
                    testTopic(
                        words = listOf(
                            entry("run-1", "run", "бігти", senseKeys = listOf("k1"), knowledgePercent = 60),
                            entry("run-2", "run", "мчати", senseKeys = listOf("k1"), knowledgePercent = 60),
                        ),
                    ),
                ),
            ),
        )
        val card = buildPracticeDeckCards(store.state.topics).single()

        applyPracticeAnswer(card, -KnowledgeStepPercent) { topicId, wordId, deltaPercent ->
            store.onEvent(VocabeeEvent.AdjustWordKnowledge(topicId, wordId, deltaPercent))
        }

        assertEquals(
            listOf(40, 40),
            store.state.topics.single().words.map { it.knowledgePercent },
        )
    }

    @Test
    fun backSideListsGroupTranslationsOnlyWhenThereAreOthers() {
        assertEquals("також: мчати, гнати", practiceAlsoTranslationsLabel(listOf("мчати", "гнати")))
        assertNull(practiceAlsoTranslationsLabel(emptyList()))
    }
}
