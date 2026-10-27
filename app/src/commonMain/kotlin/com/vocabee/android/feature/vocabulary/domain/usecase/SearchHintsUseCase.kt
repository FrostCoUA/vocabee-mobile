package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import kotlinx.coroutines.CancellationException

/**
 * Підказки під час набору: слова, що починаються з введеного, і чи вже варто
 * шукати переклад. Безкоштовно й без збору статті — тому їх можна питати на
 * кожній паузі. `null`, коли сервер не відповів (старий gateway, офлайн):
 * тоді екран шукає введене, як і раніше.
 */
class SearchHintsUseCase(
    private val api: VocabeeApi,
) {
    data class Hints(
        /** Введене — відоме слово або фраза: перекладаємо одразу. */
        val lookup: Boolean,
        /** Інші слова на введений префікс, частотніші першими. */
        val words: List<String>,
    )

    suspend operator fun invoke(
        query: String,
        speakLang: String,
        learnLang: String,
    ): Hints? = try {
        val response = api.searchHints(query, speakLang, learnLang)
        Hints(
            lookup = response.lookup,
            words = response.hints.map { it.text.trim() }.filter(String::isNotEmpty).distinct(),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        null
    }
}
