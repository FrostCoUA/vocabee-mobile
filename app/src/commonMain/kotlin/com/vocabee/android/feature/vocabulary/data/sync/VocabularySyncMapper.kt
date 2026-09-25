package com.vocabee.android.feature.vocabulary.data.sync

import com.vocabee.android.feature.vocabulary.data.api.ApplySyncRequest
import com.vocabee.android.feature.vocabulary.data.api.ClientTopicSync
import com.vocabee.android.feature.vocabulary.data.api.ClientTopicWordSync
import com.vocabee.android.feature.vocabulary.data.api.SyncResponse
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.SyncStatus
import com.vocabee.android.feature.vocabulary.domain.model.TopicUpdatedLabel
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

private const val CoverColorPrefix = "cover-"
private const val DetailsMetadataKey = "details"
private const val LexiconSnapshotMetadataKey = "lexiconSnapshot"

@Serializable
private data class LexiconSnapshotMetadata(
    val translationId: String? = null,
    val lexiconSchemaVersion: Int? = null,
    val lexiconRevision: String? = null,
) {
    val hasContent: Boolean
        get() = !translationId.isNullOrBlank() ||
            lexiconSchemaVersion != null ||
            !lexiconRevision.isNullOrBlank()
}

private val syncJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

fun VocabularySyncSnapshot.toApplySyncRequest(
    expectedUserId: String,
    replaceServerState: Boolean,
): ApplySyncRequest {
    return ApplySyncRequest(
        expectedUserId = expectedUserId,
        topics = topics.map { topic ->
            ClientTopicSync(
                id = topic.id,
                name = topic.title,
                color = "$CoverColorPrefix${topic.coverIndex}",
                icon = "book",
                sourceLang = topic.sourceLanguage.code,
                targetLang = topic.targetLanguage.code,
                position = topic.coverIndex,
                deleted = topic.syncStatus == SyncStatus.PendingDelete,
            )
        },
        words = topics.flatMap { topic ->
            topic.words.map { word ->
                ClientTopicWordSync(
                    id = word.id,
                    topicId = topic.id,
                    wordText = word.source,
                    translationText = word.translation,
                    ipa = word.ipa,
                    metadata = word.details.toMetadata(),
                    knowledgePercent = word.knowledgePercent.coerceIn(0, 100),
                    deleted = word.syncStatus == SyncStatus.PendingDelete ||
                        topic.syncStatus == SyncStatus.PendingDelete,
                )
            }
        },
        replaceServerState = replaceServerState,
    )
}

fun SyncResponse.toVocabularySyncSnapshot(
    supportedLanguages: List<LanguageOption>,
): VocabularySyncSnapshot {
    val wordsByTopicId = words.groupBy { it.topicId }
    return VocabularySyncSnapshot(
        topics = topics.map { topic ->
            DictionaryTopic(
                id = topic.id,
                title = topic.name,
                sourceLanguage = supportedLanguages.languageFor(topic.sourceLang),
                targetLanguage = supportedLanguages.languageFor(topic.targetLang),
                updatedLabel = TopicUpdatedLabel.Today,
                coverIndex = topic.color.removePrefix(CoverColorPrefix).toIntOrNull()
                    ?: topic.position,
                createdAtEpochMillis = 0L,
                updatedAtEpochMillis = 0L,
                syncStatus = SyncStatus.Synced,
                words = wordsByTopicId[topic.id].orEmpty().map { word ->
                    WordEntry(
                        id = word.id,
                        source = word.wordText,
                        translation = word.translationText,
                        ipa = word.ipa,
                        details = word.metadata.toWordDetails(),
                        knowledgePercent = word.knowledgePercent.coerceIn(0, 100),
                        addedAtEpochMillis = 0L,
                        updatedAtEpochMillis = 0L,
                        syncStatus = SyncStatus.Synced,
                    )
                },
            )
        },
        deletedTopicIds = deletedTopicIds,
        deletedWordIds = deletedWordIds,
    )
}

private fun WordDetails?.toMetadata(): JsonObject {
    val details = this ?: return JsonObject(emptyMap())
    if (!details.shouldPersist) return JsonObject(emptyMap())
    val lexiconSnapshot = LexiconSnapshotMetadata(
        translationId = details.translationId,
        lexiconSchemaVersion = details.lexiconSchemaVersion,
        lexiconRevision = details.lexiconRevision,
    )
    val visibleDetails = details.copy(
        translationId = null,
        lexiconSchemaVersion = null,
        lexiconRevision = null,
    )
    return buildJsonObject {
        if (visibleDetails.shouldPersist) {
            put(
                DetailsMetadataKey,
                syncJson.encodeToJsonElement(visibleDetails),
            )
        }
        if (lexiconSnapshot.hasContent) {
            put(
                LexiconSnapshotMetadataKey,
                syncJson.encodeToJsonElement(lexiconSnapshot),
            )
        }
    }
}

private fun JsonObject.toWordDetails(): WordDetails? {
    val visibleDetails = this[DetailsMetadataKey]?.let { raw ->
        runCatching {
            syncJson.decodeFromJsonElement<WordDetails>(raw)
        }.getOrNull()
    }
    val lexiconSnapshot = this[LexiconSnapshotMetadataKey]?.let { raw ->
        runCatching {
            syncJson.decodeFromJsonElement<LexiconSnapshotMetadata>(raw)
        }.getOrNull()
    }
    val combined = (visibleDetails ?: WordDetails()).copy(
        translationId = lexiconSnapshot?.translationId ?: visibleDetails?.translationId,
        lexiconSchemaVersion = lexiconSnapshot?.lexiconSchemaVersion
            ?: visibleDetails?.lexiconSchemaVersion,
        lexiconRevision = lexiconSnapshot?.lexiconRevision
            ?: visibleDetails?.lexiconRevision,
    )
    return combined.takeIf { it.shouldPersist }
}

private fun List<LanguageOption>.languageFor(code: String): LanguageOption {
    return firstOrNull { it.code == code } ?: LanguageOption(
        code = code,
        name = code.uppercase(),
        shortName = code.uppercase(),
        speechTag = code,
    )
}
