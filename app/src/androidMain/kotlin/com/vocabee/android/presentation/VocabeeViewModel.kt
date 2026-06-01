package com.vocabee.android.presentation

import androidx.lifecycle.ViewModel
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.data.api.VocabeeApi
import com.vocabee.android.data.preferences.PreferencesManager
import com.vocabee.android.domain.manager.UserSessionManager
import com.vocabee.android.domain.usecase.RemoteLexiconSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VocabeeViewModel @Inject constructor(
    repository: VocabularyRepository,
    userSessionManager: UserSessionManager,
    api: VocabeeApi,
    val preferencesManager: PreferencesManager,
) : ViewModel() {
    val store = VocabeeStore(
        repository = repository,
        userSessionManager = userSessionManager,
        preferencesManager = preferencesManager,
    )
    val remoteLexiconSearch = RemoteLexiconSearchUseCase(api)
}
