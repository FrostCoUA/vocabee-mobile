package com.vocabee.android.presentation

import androidx.lifecycle.ViewModel
import com.vocabee.android.data.VocabularyRepository
import com.vocabee.android.domain.manager.UserSessionManager
import com.vocabee.android.platform.MachineTranslationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VocabeeViewModel @Inject constructor(
    repository: VocabularyRepository,
    userSessionManager: UserSessionManager,
    private val machineTranslationProvider: MachineTranslationProvider,
) : ViewModel() {
    val store = VocabeeStore(
        repository = repository,
        userSessionManager = userSessionManager,
        machineTranslationProvider = machineTranslationProvider,
    )

    override fun onCleared() {
        machineTranslationProvider.close()
        super.onCleared()
    }
}
