package com.vocabee.android.data

import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.domain.model.TopicUpdatedLabel
import com.vocabee.android.domain.model.WordEntry

interface VocabularyRepository {
    val supportedLanguages: List<LanguageOption>

    fun initialTopics(): List<DictionaryTopic>

    fun translationOptionsFor(
        topic: DictionaryTopic,
        input: String,
    ): List<TranslationOption>
}

class FakeVocabularyRepository : VocabularyRepository {
    override val supportedLanguages = listOf(
        LanguageOption("uk", "Ukrainian", "UA", "uk-UA"),
        LanguageOption("en", "English", "EN", "en-US"),
        LanguageOption("ru", "Russian", "RU", "ru-RU"),
        LanguageOption("pl", "Polish", "PL", "pl-PL"),
        LanguageOption("de", "German", "DE", "de-DE"),
        LanguageOption("es", "Spanish", "ES", "es-ES"),
    )

    override fun initialTopics(): List<DictionaryTopic> {
        val ukrainian = supportedLanguages.first { it.code == "uk" }
        val english = supportedLanguages.first { it.code == "en" }

        return listOf(
            DictionaryTopic(
                id = "topic-travel",
                title = "Подорож літаком",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.Yesterday,
                coverIndex = 2,
                words = sampleWords(
                    prefix = "travel",
                    count = 23,
                    seeds = listOf(
                        "посадковий талон" to "boarding pass",
                        "багаж" to "luggage",
                        "аеропорт" to "airport",
                        "рейс" to "flight",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-cooking",
                title = "Кулінарія",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.Today,
                coverIndex = 0,
                words = sampleWords(
                    prefix = "cooking",
                    count = 41,
                    seeds = listOf(
                        "рецепт" to "recipe",
                        "сковорідка" to "pan",
                        "сіль" to "salt",
                        "сніданок" to "breakfast",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-work-it",
                title = "Робота в IT",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.DaysAgo(2),
                coverIndex = 1,
                words = sampleWords(
                    prefix = "work-it",
                    count = 87,
                    seeds = listOf(
                        "завдання" to "task",
                        "зустріч" to "meeting",
                        "реліз" to "release",
                        "помилка" to "bug",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-emotions",
                title = "Емоції та почуття",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.DaysAgo(5),
                coverIndex = 3,
                words = sampleWords(
                    prefix = "emotions",
                    count = 18,
                    seeds = listOf(
                        "радість" to "joy",
                        "сум" to "sadness",
                        "здивування" to "surprise",
                        "спокій" to "calm",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-hotel",
                title = "Готель",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.WeeksAgo(1),
                coverIndex = 4,
                words = sampleWords(
                    prefix = "hotel",
                    count = 32,
                    seeds = listOf(
                        "бронювання" to "reservation",
                        "ключ" to "key",
                        "номер" to "room",
                        "рушник" to "towel",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-city",
                title = "Місто",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.DaysAgo(8),
                coverIndex = 5,
                words = sampleWords(
                    prefix = "city",
                    count = 29,
                    seeds = listOf(
                        "вулиця" to "street",
                        "площа" to "square",
                        "перехрестя" to "intersection",
                        "зупинка" to "stop",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-health",
                title = "Здоров'я",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.DaysAgo(12),
                coverIndex = 6,
                words = sampleWords(
                    prefix = "health",
                    count = 14,
                    seeds = listOf(
                        "аптека" to "pharmacy",
                        "лікар" to "doctor",
                        "симптом" to "symptom",
                        "температура" to "fever",
                    ),
                ),
            ),
            DictionaryTopic(
                id = "topic-shopping",
                title = "Шопінг",
                sourceLanguage = ukrainian,
                targetLanguage = english,
                updatedLabel = TopicUpdatedLabel.WeeksAgo(2),
                coverIndex = 7,
                words = sampleWords(
                    prefix = "shopping",
                    count = 17,
                    seeds = listOf(
                        "знижка" to "discount",
                        "розмір" to "size",
                        "чек" to "receipt",
                        "кошик" to "basket",
                    ),
                ),
            ),
        )
    }

    override fun translationOptionsFor(
        topic: DictionaryTopic,
        input: String,
    ): List<TranslationOption> {
        val existingWord = topic.findExistingWord(input)
        return rawTranslationOptions(
            input = input,
            sourceLanguage = topic.sourceLanguage,
            targetLanguage = topic.targetLanguage,
        ).withExistingFirst(existingWord)
    }

    private fun sampleWords(
        prefix: String,
        count: Int,
        seeds: List<Pair<String, String>>,
    ): List<WordEntry> {
        val seededWords = seeds.mapIndexed { index, pair ->
            WordEntry(
                id = "$prefix-${index + 1}",
                source = pair.first,
                translation = pair.second,
            )
        }
        if (count <= seededWords.size) return seededWords.take(count)

        return seededWords + ((seededWords.size + 1)..count).map { index ->
            WordEntry(
                id = "$prefix-$index",
                source = "слово $index",
                translation = "word $index",
            )
        }
    }

    private fun rawTranslationOptions(
        input: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
    ): List<TranslationOption> {
        if (input.isBlank()) return emptyList()

        val normalized = input.trim().lowercase()
        val known = knownTranslations["${sourceLanguage.code}:${targetLanguage.code}"]
            ?.get(normalized)
            .orEmpty()

        val values = if (known.isNotEmpty()) {
            known
        } else {
            listOf(
                input.trim(),
                "${input.trim()} (${targetLanguage.shortName})",
            )
        }

        return values.distinct().take(3).mapIndexed { index, value ->
            TranslationOption(
                value = value,
                note = when (index) {
                    0 -> TranslationOptionNote.Primary
                    1 -> TranslationOptionNote.Alternative
                    else -> TranslationOptionNote.Additional
                },
            )
        }
    }

    private fun DictionaryTopic.findExistingWord(query: String): WordEntry? {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return null

        return words.firstOrNull { word ->
            word.source.equals(normalizedQuery, ignoreCase = true) ||
                word.translation.equals(normalizedQuery, ignoreCase = true)
        } ?: words.firstOrNull { word ->
            word.source.lowercase().contains(normalizedQuery) ||
                word.translation.lowercase().contains(normalizedQuery)
        }
    }

    private fun List<TranslationOption>.withExistingFirst(existingWord: WordEntry?): List<TranslationOption> {
        if (existingWord == null) return this

        val existingOption = TranslationOption(
            value = existingWord.translation,
            note = TranslationOptionNote.AlreadyAdded(existingWord.source),
            alreadyAdded = true,
        )
        return (listOf(existingOption) + this)
            .distinctBy { it.value.lowercase() }
            .take(3)
    }

    private val knownTranslations = mapOf(
        "en:uk" to mapOf(
            "hello" to listOf("привіт", "вітаю"),
            "book" to listOf("книга", "зошит"),
            "luggage" to listOf("багаж", "валіза"),
            "table" to listOf("столик", "таблиця"),
            "receipt" to listOf("чек", "квитанція"),
            "boarding pass" to listOf("посадковий талон"),
            "airport" to listOf("аеропорт"),
            "coffee" to listOf("кава"),
            "water" to listOf("вода"),
        ),
        "en:ru" to mapOf(
            "hello" to listOf("привет", "здравствуйте"),
            "book" to listOf("книга"),
            "luggage" to listOf("багаж"),
            "boarding pass" to listOf("посадочный талон"),
            "airport" to listOf("аэропорт"),
            "coffee" to listOf("кофе"),
            "water" to listOf("вода"),
            "recipe" to listOf("рецепт"),
        ),
        "uk:en" to mapOf(
            "привіт" to listOf("hello", "hi"),
            "книга" to listOf("book"),
            "багаж" to listOf("luggage", "baggage"),
            "столик" to listOf("table"),
            "чек" to listOf("receipt"),
            "аеропорт" to listOf("airport"),
            "кава" to listOf("coffee"),
            "вода" to listOf("water"),
            "турбулентність" to listOf("turbulence", "turbulent", "disturbance"),
            "турбуленція" to listOf("turbulence", "turbulent", "disturbance"),
            "вирушати" to listOf("depart", "leave", "set off"),
            "літак" to listOf("airplane", "plane", "aircraft"),
            "квиток" to listOf("ticket", "fare", "pass"),
        ),
        "ru:en" to mapOf(
            "привет" to listOf("hello", "hi"),
            "книга" to listOf("book"),
            "багаж" to listOf("luggage", "baggage"),
            "аэропорт" to listOf("airport"),
            "кофе" to listOf("coffee"),
            "вода" to listOf("water"),
            "рецепт" to listOf("recipe"),
        ),
        "pl:uk" to mapOf(
            "dzień dobry" to listOf("добрий день"),
            "dziękuję" to listOf("дякую"),
            "woda" to listOf("вода"),
            "kawa" to listOf("кава"),
        ),
        "de:uk" to mapOf(
            "hallo" to listOf("привіт"),
            "wasser" to listOf("вода"),
            "kaffee" to listOf("кава"),
        ),
        "es:uk" to mapOf(
            "hola" to listOf("привіт"),
            "agua" to listOf("вода"),
            "café" to listOf("кава"),
        ),
    )
}
