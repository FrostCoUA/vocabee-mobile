package com.vocabee.android.domain.usecase

import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.domain.manager.UserSessionManager
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.WordDetails
import com.vocabee.android.domain.model.WordEntry

class LoadUserTopicsUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(): List<DictionaryTopic> {
        return repository.topicsForUser(userSessionManager.currentUserKey)
    }
}

class CreateTopicUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(
        title: String,
        sourceLanguage: LanguageOption,
        targetLanguage: LanguageOption,
        coverIndex: Int,
    ): DictionaryTopic {
        return repository.createTopic(
            userKey = userSessionManager.currentUserKey,
            title = title,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            coverIndex = coverIndex,
        )
    }
}

class AddWordUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(
        topicId: String,
        source: String,
        translation: String,
        ipa: String? = null,
        details: WordDetails? = null,
    ): WordEntry? {
        return repository.addWord(
            userKey = userSessionManager.currentUserKey,
            topicId = topicId,
            source = source,
            translation = translation,
            ipa = ipa,
            details = details,
        )
    }
}

