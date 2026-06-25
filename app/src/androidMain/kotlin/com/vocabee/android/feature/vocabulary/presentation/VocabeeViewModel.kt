package com.vocabee.android.feature.vocabulary.presentation

import androidx.lifecycle.ViewModel
import com.vocabee.android.feature.vocabulary.domain.VocabularyRepository
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.domain.manager.UserSessionManager
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoteLexiconSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VocabeeViewModel @Inject constructor(
    repository: VocabularyRepository,
    userSessionManager: UserSessionManager,
    val api: VocabeeApi,
    val preferencesManager: PreferencesManager,
) : ViewModel() {
    val store = VocabeeStore(
        repository = repository,
        userSessionManager = userSessionManager,
        preferencesManager = preferencesManager,
    )
    val remoteLexiconSearch = RemoteLexiconSearchUseCase(api)
}
