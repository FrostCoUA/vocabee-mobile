package com.vocabee.android.feature.vocabulary.domain.usecase

import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry

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
        iconIndex: Int,
    ): DictionaryTopic {
        return repository.createTopic(
            userKey = userSessionManager.currentUserKey,
            title = title,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            coverIndex = coverIndex,
            iconIndex = iconIndex,
        )
    }
}

class RemoveTopicUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(topicId: String): Boolean {
        return repository.removeTopic(
            userKey = userSessionManager.currentUserKey,
            topicId = topicId,
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

class RemoveWordUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(topicId: String, translation: String): Boolean {
        return repository.removeWordByTranslation(
            userKey = userSessionManager.currentUserKey,
            topicId = topicId,
            translation = translation,
        )
    }
}

class AdjustWordKnowledgeUseCase(
    private val repository: VocabularyRepository,
    private val userSessionManager: UserSessionManager,
) {
    operator fun invoke(
        topicId: String,
        wordId: String,
        deltaPercent: Int,
    ): WordEntry? {
        return repository.adjustWordKnowledgePercent(
            userKey = userSessionManager.currentUserKey,
            topicId = topicId,
            wordId = wordId,
            deltaPercent = deltaPercent,
        )
    }
}
