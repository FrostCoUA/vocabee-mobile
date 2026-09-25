package com.vocabee.android.feature.vocabulary.presentation

import com.vocabee.android.core.analytics.AnalyticsTracker
import com.vocabee.android.feature.vocabulary.domain.model.DEFAULT_LOCAL_USER_KEY
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Колода класичного тренування: одиниця — СЕНС-ГРУПА, а не рядок словника.
 * Три збережені переклади одного значення дають одну картку (інакше юзер тричі
 * поспіль відповідає на те саме), відбір іде по найслабшому члену групи, а
 * відповідь однією батч-подією рухає знання всіх записів групи.
 */
class PracticeSenseDeckTest {
    private fun entry(
        id: String,
        source: String,
        translation: String,
        senseKeys: List<String> = emptyList(),
        knowledgePercent: Int = 0,
        ipa: String? = null,
        addedAt: Long = 0L,
        details: WordDetails? = if (senseKeys.isEmpty()) null else WordDetails(senseKeys = senseKeys),
    ) = WordEntry(
        id = id,
        source = source,
        translation = translation,
        ipa = ipa,
        details = details,
        knowledgePercent = knowledgePercent,
        addedAtEpochMillis = addedAt,
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

    private fun storeWith(words: List<WordEntry>, analytics: AnalyticsTracker): VocabeeStore {
        val store = VocabeeStore(analytics = analytics)
        store.replaceSyncSnapshot(
            userKey = DEFAULT_LOCAL_USER_KEY,
            snapshot = VocabularySyncSnapshot(topics = listOf(testTopic(words))),
        )
        return store
    }

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
        // Дванадцять рядків дають шість карток — і жодного сенсу двічі.
        assertEquals(6, deckKeys.size)
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

    // (в) + I1/M4: одна подія на відповідь і рівно один трек аналітики на картку.
    @Test
    fun knownAnswerRaisesEveryMemberOfTheSenseGroupInOneEvent() {
        val analytics = RecordingAnalyticsTracker()
        val store = storeWith(
            words = listOf(
                entry("run-1", "run", "бігти", senseKeys = listOf("k1")),
                entry("run-2", "run", "мчати", senseKeys = listOf("k1")),
                entry("run-3", "run", "гнати", senseKeys = listOf("k1")),
            ),
            analytics = analytics,
        )
        val card = buildPracticeDeckCards(store.state.topics).single()
        assertEquals(listOf("run-1", "run-2", "run-3"), card.memberWordIds)

        store.onEvent(
            VocabeeEvent.AdjustSenseGroupKnowledge(
                topicId = card.topicId,
                memberWordIds = card.memberWordIds,
                deltaPercent = KnowledgeStepPercent,
            ),
        )

        assertEquals(
            List(3) { KnowledgeStepPercent },
            store.state.topics.single().words.map { it.knowledgePercent },
        )
        val answers = analytics.events.filter { (name, _) -> name == "practice_answer" }
        assertEquals(1, answers.size)
        assertEquals(
            // Представник — обличчя картки, саме його id тримає подію.
            mapOf(
                "topic_id" to "topic",
                "word_id" to "run-1",
                "sense_group_size" to 3,
                "known" to true,
            ),
            answers.single().second,
        )
    }

    @Test
    fun unknownAnswerLowersEveryMemberOfTheSenseGroup() {
        val analytics = RecordingAnalyticsTracker()
        val store = storeWith(
            words = listOf(
                entry("run-1", "run", "бігти", senseKeys = listOf("k1"), knowledgePercent = 60),
                entry("run-2", "run", "мчати", senseKeys = listOf("k1"), knowledgePercent = 60),
            ),
            analytics = analytics,
        )
        val card = buildPracticeDeckCards(store.state.topics).single()

        store.onEvent(
            VocabeeEvent.AdjustSenseGroupKnowledge(
                topicId = card.topicId,
                memberWordIds = card.memberWordIds,
                deltaPercent = -KnowledgeStepPercent,
            ),
        )

        assertEquals(listOf(40, 40), store.state.topics.single().words.map { it.knowledgePercent })
        assertEquals(
            false,
            analytics.events.single { (name, _) -> name == "practice_answer" }.second["known"],
        )
    }

    // M8 — дельта клампиться ПО КОЖНОМУ члену окремо: сильний упирається в 100,
    // слабкий росте далі, група не вирівнюється штучно.
    @Test
    fun groupDeltaIsClampedPerMember() {
        val store = storeWith(
            words = listOf(
                entry("run-1", "run", "бігти", senseKeys = listOf("k1"), knowledgePercent = 90),
                entry("run-2", "run", "мчати", senseKeys = listOf("k1"), knowledgePercent = 10),
            ),
            analytics = RecordingAnalyticsTracker(),
        )
        val card = buildPracticeDeckCards(store.state.topics).single()

        store.onEvent(
            VocabeeEvent.AdjustSenseGroupKnowledge(
                topicId = card.topicId,
                memberWordIds = card.memberWordIds,
                deltaPercent = KnowledgeStepPercent,
            ),
        )

        assertEquals(listOf(100, 30), store.state.topics.single().words.map { it.knowledgePercent })
    }

    // I2 — картка бере деталі й IPA з ГРУПИ (той самий фолбек, що в списку
    // словника): представник без вмісту не має лишати фронт без речення.
    @Test
    fun cardFallsBackToGroupDetailsWhenRepresentativeHasNoBlob() {
        val topic = testTopic(
            words = listOf(
                entry(
                    id = "run-new",
                    source = "run",
                    translation = "мчати",
                    addedAt = 200L,
                    // Знімок лексикону без вмісту: ключі є, деталей немає.
                    details = WordDetails(translationId = "t-1", senseKeys = listOf("k1")),
                ),
                entry(
                    id = "run-old",
                    source = "run",
                    translation = "бігти",
                    addedAt = 100L,
                    ipa = "/run/",
                    details = WordDetails(
                        senseKeys = listOf("k1"),
                        senses = listOf(
                            WordSense(
                                senseKey = "k1",
                                definition = "to move fast on foot",
                                examples = listOf("He can run fast."),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val card = buildPracticeDeckCards(listOf(topic)).single()
        assertEquals("run-old", card.contextWord.id)

        assertEquals("run-new", card.word.id)
        assertEquals("He can run fast.", card.details.contextSentence())
        assertEquals("/run/", card.ipa)
    }

    // `sense_group_size` — розмір ПО ФАКТУ: колода заморожена на час раунду, тож
    // її `memberWordIds` можуть згадувати рядок, видалений уже після роздачі.
    @Test
    fun senseGroupSizeCountsOnlyTheRowsTheAnswerActuallyMoved() {
        val analytics = RecordingAnalyticsTracker()
        val store = storeWith(
            words = listOf(
                entry("run-1", "run", "бігти", senseKeys = listOf("k1")),
                entry("run-2", "run", "мчати", senseKeys = listOf("k1")),
                entry("run-3", "run", "гнати", senseKeys = listOf("k1")),
            ),
            analytics = analytics,
        )
        val card = buildPracticeDeckCards(store.state.topics).single()
        assertEquals(listOf("run-1", "run-2", "run-3"), card.memberWordIds)

        store.onEvent(VocabeeEvent.RemoveWord(topicId = card.topicId, source = "run", translation = "гнати"))
        store.onEvent(
            VocabeeEvent.AdjustSenseGroupKnowledge(
                topicId = card.topicId,
                memberWordIds = card.memberWordIds,
                deltaPercent = KnowledgeStepPercent,
            ),
        )

        assertEquals(
            mapOf(
                "topic_id" to "topic",
                "word_id" to "run-1",
                "sense_group_size" to 2,
                "known" to true,
            ),
            analytics.events.single { (name, _) -> name == "practice_answer" }.second,
        )
    }

    @Test
    fun backSideListsGroupTranslationsOnlyWhenThereAreOthers() {
        assertEquals("також: мчати, гнати", practiceAlsoTranslationsLabel(listOf("мчати", "гнати")))
        assertNull(practiceAlsoTranslationsLabel(emptyList()))
    }

    private class RecordingAnalyticsTracker : AnalyticsTracker {
        val events = mutableListOf<Pair<String, Map<String, Any?>>>()

        override fun track(event: String, properties: Map<String, Any?>) {
            events += event to properties
        }

        override fun identify(userId: String, properties: Map<String, Any?>) = Unit

        override fun reset() = Unit
    }
}
