package com.vocabee.android.feature.vocabulary.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UpdateProfileRequest(
    val speakLang: String? = null,
    val learnLang: String? = null,
    val notificationsEnabled: Boolean? = null,
    val darkThemeEnabled: Boolean? = null,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class SyncRequest(
    val since: String? = null,
)

@Serializable
data class SyncResponse(
    val topics: List<TopicSyncResponse>,
    val words: List<TopicWordSyncResponse>,
    val deletedTopicIds: List<String>,
    val deletedWordIds: List<String>,
    val serverTime: String,
)

@Serializable
data class ApplySyncRequest(
    val topics: List<ClientTopicSync>,
    val words: List<ClientTopicWordSync>,
    val replaceServerState: Boolean = false,
)

@Serializable
data class ClientTopicSync(
    val id: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    val sourceLang: String,
    val targetLang: String,
    val position: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class ClientTopicWordSync(
    val id: String,
    val topicId: String,
    val wordText: String,
    val translationText: String,
    val ipa: String? = null,
    val source: String = "translator",
    val origin: String = "vocabee-mobile",
    val metadata: JsonObject = JsonObject(emptyMap()),
    val knowledgePercent: Int = 0,
    val addedAt: String? = null,
    val updatedAt: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class TopicSyncResponse(
    val id: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    val sourceLang: String,
    val targetLang: String,
    val position: Int,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: String? = null,
    val wordsUpdatedAt: String,
    val wordsSyncedAt: String? = null,
)

@Serializable
data class TopicWordSyncResponse(
    val id: String,
    val topicId: String,
    val wordText: String,
    val translationText: String,
    val ipa: String? = null,
    val knowledgePercent: Int = 0,
    val source: String,
    val origin: String,
    val metadata: JsonObject = JsonObject(emptyMap()),
    val addedAt: String,
    val updatedAt: String,
    val lastSyncedAt: String? = null,
)
