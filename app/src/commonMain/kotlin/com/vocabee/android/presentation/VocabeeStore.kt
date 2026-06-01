package com.vocabee.android.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vocabee.android.data.FakeVocabularyRepository
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.domain.manager.StaticUserSessionManager
import com.vocabee.android.domain.manager.UserSessionManager
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.usecase.AddWordUseCase
import com.vocabee.android.domain.usecase.CreateTopicUseCase
import com.vocabee.android.domain.usecase.LoadUserTopicsUseCase

data class VocabeeState(
    val supportedLanguages: List<LanguageOption>,
    val userLanguage: LanguageOption,
    val learningLanguage: LanguageOption,
    val topics: List<DictionaryTopic>,
    val topicCounter: Int = 0,
    val recentlyAddedWordId: String? = null,
    val notificationsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false,
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
}

class VocabeeStore(
    private val repository: VocabularyRepository = FakeVocabularyRepository(),
    private val userSessionManager: UserSessionManager = StaticUserSessionManager(),
) {
    private val loadUserTopicsUseCase = LoadUserTopicsUseCase(repository, userSessionManager)
    private val createTopicUseCase = CreateTopicUseCase(repository, userSessionManager)
    private val addWordUseCase = AddWordUseCase(repository, userSessionManager)

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
        }
    }

    private fun initialState(): VocabeeState {
        val supportedLanguages = repository.supportedLanguages
        return VocabeeState(
            supportedLanguages = supportedLanguages,
            userLanguage = supportedLanguages.first { it.code == "uk" },
            learningLanguage = supportedLanguages.first { it.code == "en" },
            topics = loadUserTopicsUseCase(),
        )
    }

    private fun createTopic(
        title: String,
        coverIndex: Int,
    ) {
        val cleanedTitle = title.trim()
        if (cleanedTitle.isBlank()) return

        createTopicUseCase(
            title = cleanedTitle,
            sourceLanguage = state.learningLanguage,
            targetLanguage = state.userLanguage,
            coverIndex = coverIndex,
        )
        state = state.copy(
            topics = loadUserTopicsUseCase(),
        )
    }

    private fun addWord(
        topicId: String,
        source: String,
        translation: String,
    ) {
        val cleanedSource = source.trim()
        val cleanedTranslation = translation.trim()
        if (cleanedSource.isBlank() || cleanedTranslation.isBlank()) return

        val word = addWordUseCase(
            topicId = topicId,
            source = cleanedSource,
            translation = cleanedTranslation,
        )
        if (word == null) return

        state = state.copy(
            topics = loadUserTopicsUseCase(),
            recentlyAddedWordId = word.id,
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
}
