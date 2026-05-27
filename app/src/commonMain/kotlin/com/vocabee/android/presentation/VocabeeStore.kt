package com.vocabee.android.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vocabee.android.data.FakeVocabularyRepository
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.domain.model.WordEntry
import com.vocabee.android.platform.MachineTranslationProvider
import com.vocabee.android.platform.NoMachineTranslationProvider

data class VocabeeState(
    val supportedLanguages: List<LanguageOption>,
    val userLanguage: LanguageOption,
    val learningLanguage: LanguageOption,
    val topics: List<DictionaryTopic>,
    val topicCounter: Int = 10,
    val recentlyAddedWordId: String? = null,
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val machineTranslations: Map<String, MachineTranslationState> = emptyMap(),
)

data class MachineTranslationState(
    val isLoading: Boolean = false,
    val translatedText: String? = null,
    val errorMessage: String? = null,
)

sealed interface VocabeeEvent {
    data class CreateTopic(
        val title: String,
        val coverIndex: Int,
    ) : VocabeeEvent

    data class AddWord(
        val topicId: String,
        val source: String,
        val translation: String,
    ) : VocabeeEvent

    data class SelectSpeakingLanguage(
        val language: LanguageOption,
    ) : VocabeeEvent

    data class SelectLearningLanguage(
        val language: LanguageOption,
    ) : VocabeeEvent

    data class SetNotificationsEnabled(
        val enabled: Boolean,
    ) : VocabeeEvent

    data class SetDarkThemeEnabled(
        val enabled: Boolean,
    ) : VocabeeEvent

    data class RequestMachineTranslation(
        val topicId: String,
        val input: String,
    ) : VocabeeEvent
}

class VocabeeStore(
    private val repository: VocabularyRepository = FakeVocabularyRepository(),
    private val machineTranslationProvider: MachineTranslationProvider = NoMachineTranslationProvider,
) {
    var state by mutableStateOf(initialState())
        private set

    fun onEvent(event: VocabeeEvent) {
        when (event) {
            is VocabeeEvent.CreateTopic -> createTopic(event.title, event.coverIndex)
            is VocabeeEvent.AddWord -> addWord(event.topicId, event.source, event.translation)
            is VocabeeEvent.SelectSpeakingLanguage -> selectSpeakingLanguage(event.language)
            is VocabeeEvent.SelectLearningLanguage -> selectLearningLanguage(event.language)
            is VocabeeEvent.SetNotificationsEnabled -> {
                state = state.copy(notificationsEnabled = event.enabled)
            }
            is VocabeeEvent.SetDarkThemeEnabled -> {
                state = state.copy(darkThemeEnabled = event.enabled)
            }
            is VocabeeEvent.RequestMachineTranslation -> {
                requestMachineTranslation(event.topicId, event.input)
            }
        }
    }

    fun translationOptionsFor(
        topic: DictionaryTopic,
        input: String,
    ): List<TranslationOption> {
        val baseOptions = repository.translationOptionsFor(topic, input)
        val machineTranslatedText = state.machineTranslations[translationKey(topic, input)]
            ?.translatedText
            ?.takeIf { it.isNotBlank() }
        val machineOption = machineTranslatedText?.let { translatedText ->
            TranslationOption(
                value = translatedText,
                note = TranslationOptionNote.MlKitOnDevice,
            )
        }

        return mergeTranslationOptions(
            baseOptions = baseOptions,
            machineOption = machineOption,
        )
    }

    private fun initialState(): VocabeeState {
        val supportedLanguages = repository.supportedLanguages
        return VocabeeState(
            supportedLanguages = supportedLanguages,
            userLanguage = supportedLanguages.first { it.code == "uk" },
            learningLanguage = supportedLanguages.first { it.code == "en" },
            topics = repository.initialTopics(),
        )
    }

    private fun createTopic(
        title: String,
        coverIndex: Int,
    ) {
        val cleanedTitle = title.trim()
        if (cleanedTitle.isBlank()) return

        val nextCounter = state.topicCounter + 1
        val newTopic = DictionaryTopic(
            id = "topic-$nextCounter",
            title = cleanedTitle,
            sourceLanguage = state.userLanguage,
            targetLanguage = state.learningLanguage,
            coverIndex = coverIndex,
        )
        state = state.copy(
            topicCounter = nextCounter,
            topics = state.topics + newTopic,
        )
    }

    private fun addWord(
        topicId: String,
        source: String,
        translation: String,
    ) {
        val topic = state.topics.firstOrNull { it.id == topicId } ?: return
        val cleanedSource = source.trim()
        val cleanedTranslation = translation.trim()
        if (cleanedSource.isBlank() || cleanedTranslation.isBlank()) return

        val exists = topic.words.any { word ->
            word.source.equals(cleanedSource, ignoreCase = true) ||
                word.translation.equals(cleanedSource, ignoreCase = true) ||
                word.source.equals(cleanedTranslation, ignoreCase = true) ||
                word.translation.equals(cleanedTranslation, ignoreCase = true)
        }
        if (exists) return

        val wordId = "${topic.id}-word-${topic.words.size + 1}"
        val updatedTopic = topic.copy(
            words = listOf(
                WordEntry(
                    id = wordId,
                    source = cleanedSource,
                    translation = cleanedTranslation,
                )
            ) + topic.words,
        )

        state = state.copy(
            topics = state.topics.map { currentTopic ->
                if (currentTopic.id == topic.id) updatedTopic else currentTopic
            },
            recentlyAddedWordId = wordId,
        )
    }

    private fun selectSpeakingLanguage(language: LanguageOption) {
        val adjustedLearningLanguage = if (state.learningLanguage.code == language.code) {
            state.supportedLanguages.first { it.code != language.code }
        } else {
            state.learningLanguage
        }

        state = state.copy(
            userLanguage = language,
            learningLanguage = adjustedLearningLanguage,
        )
    }

    private fun selectLearningLanguage(language: LanguageOption) {
        val adjustedUserLanguage = if (state.userLanguage.code == language.code) {
            state.supportedLanguages.first { it.code != language.code }
        } else {
            state.userLanguage
        }

        state = state.copy(
            userLanguage = adjustedUserLanguage,
            learningLanguage = language,
        )
    }

    private fun requestMachineTranslation(
        topicId: String,
        input: String,
    ) {
        val topic = state.topics.firstOrNull { it.id == topicId } ?: return
        val cleanedInput = input.trim()
        if (cleanedInput.isBlank() || !machineTranslationProvider.isSupported) return

        val key = translationKey(topic, cleanedInput)
        val currentRequest = state.machineTranslations[key]
        if (currentRequest?.isLoading == true || currentRequest?.translatedText?.isNotBlank() == true) {
            return
        }

        state = state.copy(
            machineTranslations = state.machineTranslations + (
                key to MachineTranslationState(isLoading = true)
                ),
        )

        machineTranslationProvider.translate(
            sourceLanguageCode = topic.sourceLanguage.code,
            targetLanguageCode = topic.targetLanguage.code,
            text = cleanedInput,
            onSuccess = { translatedText ->
                applyMachineTranslationResult(
                    key = key,
                    translatedText = translatedText,
                    errorMessage = null,
                )
            },
            onError = { message ->
                applyMachineTranslationResult(
                    key = key,
                    translatedText = null,
                    errorMessage = message,
                )
            },
        )
    }

    private fun applyMachineTranslationResult(
        key: String,
        translatedText: String?,
        errorMessage: String?,
    ) {
        state = state.copy(
            machineTranslations = state.machineTranslations + (
                key to MachineTranslationState(
                    isLoading = false,
                    translatedText = translatedText,
                    errorMessage = errorMessage,
                )
                ),
        )
    }

    private fun translationKey(
        topic: DictionaryTopic,
        input: String,
    ): String {
        return listOf(
            topic.id,
            topic.sourceLanguage.code,
            topic.targetLanguage.code,
            input.trim().lowercase(),
        ).joinToString(separator = "|")
    }

    private fun mergeTranslationOptions(
        baseOptions: List<TranslationOption>,
        machineOption: TranslationOption?,
    ): List<TranslationOption> {
        if (machineOption == null) return baseOptions

        val existingWordOption = baseOptions.firstOrNull()?.takeIf { it.alreadyAdded }
        val otherOptions = if (existingWordOption == null) baseOptions else baseOptions.drop(1)

        return listOfNotNull(existingWordOption, machineOption)
            .plus(otherOptions)
            .distinctBy { option -> option.value.lowercase() }
            .take(3)
    }
}
