package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails

internal data class ContextAnalysisRequest(
    val topicId: String,
    val wordId: String,
    val sentence: String,
)

internal fun WordDetails?.hasReadyContextGlossary(sentence: String): Boolean =
    this?.contextGlossary?.let { it.sentence == sentence && it.tokens.isNotEmpty() } == true

/** Confirmation and duplicate-request guard shared by dictionary and practice. */
internal class ContextAnalysisGate {
    var pending by mutableStateOf<ContextAnalysisRequest?>(null)
        private set

    private var runningKeys by mutableStateOf(emptySet<Pair<String, String>>())

    fun isRunning(topicId: String, wordId: String): Boolean =
        (topicId to wordId) in runningKeys

    fun ask(request: ContextAnalysisRequest, alreadyReady: Boolean): Boolean {
        val key = request.topicId to request.wordId
        if (alreadyReady || request.sentence.isBlank() || key in runningKeys || pending != null) return false
        pending = request
        return true
    }

    fun cancel() {
        pending = null
    }

    fun confirm(): ContextAnalysisRequest? {
        val request = pending ?: return null
        pending = null
        val key = request.topicId to request.wordId
        if (key in runningKeys) return null
        runningKeys = runningKeys + key
        return request
    }

    fun finish(request: ContextAnalysisRequest) {
        runningKeys = runningKeys - (request.topicId to request.wordId)
    }
}
