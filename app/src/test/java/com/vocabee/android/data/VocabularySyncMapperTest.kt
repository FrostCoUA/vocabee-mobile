package com.vocabee.android.data

import com.vocabee.android.feature.vocabulary.data.api.SyncResponse
import com.vocabee.android.feature.vocabulary.data.api.TopicSyncResponse
import com.vocabee.android.feature.vocabulary.data.api.TopicWordSyncResponse
import com.vocabee.android.feature.vocabulary.data.sync.toApplySyncRequest
import com.vocabee.android.feature.vocabulary.data.sync.toVocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.VocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.model.WordSense
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularySyncMapperTest {
    @Test
    fun visibleDetailsAndLexiconControlRoundTripThroughSeparateMetadataObjects() {
        val details = WordDetails(
            translationId = "translation-obvious",
            lexiconSchemaVersion = 1,
            lexiconRevision = "sha256:obvious-v2",
            senses = listOf(
                WordSense(
                    definition = "Easy to see, understand, or recognize.",
                    examples = listOf("The answer seems obvious now."),
                ),
            ),
        )
        val request = snapshot(details).toApplySyncRequest(
            expectedUserId = UserId,
            replaceServerState = false,
        )
        assertEquals(UserId, request.expectedUserId)
        val metadata = request.words.single().metadata

        val visibleDetails = requireNotNull(metadata["details"]).jsonObject
        assertFalse("translationId" in visibleDetails)
        assertFalse("lexiconSchemaVersion" in visibleDetails)
        assertFalse("lexiconRevision" in visibleDetails)
        val control = requireNotNull(metadata["lexiconSnapshot"]).jsonObject
        assertEquals("translation-obvious", control.getValue("translationId").jsonPrimitive.content)
        assertEquals("1", control.getValue("lexiconSchemaVersion").jsonPrimitive.content)
        assertEquals("sha256:obvious-v2", control.getValue("lexiconRevision").jsonPrimitive.content)

        val restored = response(metadata)
            .toVocabularySyncSnapshot(SupportedLanguages)
            .topics
            .single()
            .words
            .single()
            .details

        assertEquals(details, restored)
    }

    @Test
    fun controlOnlySnapshotSurvivesSyncWithoutCreatingVisibleDetails() {
        val details = WordDetails(
            translationId = "translation-obvious",
            lexiconSchemaVersion = 1,
            lexiconRevision = "sha256:obvious-v2",
        )
        val metadata = snapshot(details)
            .toApplySyncRequest(
                expectedUserId = UserId,
                replaceServerState = false,
            )
            .words
            .single()
            .metadata

        assertNull(metadata["details"])
        assertTrue(metadata["lexiconSnapshot"] != null)

        val restored = requireNotNull(
            response(metadata)
                .toVocabularySyncSnapshot(SupportedLanguages)
                .topics
                .single()
                .words
                .single()
                .details,
        )
        assertTrue(restored.isEmpty)
        assertTrue(restored.shouldPersist)
        assertEquals(details, restored)
    }

    private fun snapshot(details: WordDetails): VocabularySyncSnapshot =
        VocabularySyncSnapshot(
            topics = listOf(
                DictionaryTopic(
                    id = TopicId,
                    title = "Common",
                    sourceLanguage = English,
                    targetLanguage = Ukrainian,
                    words = listOf(
                        WordEntry(
                            id = WordId,
                            source = "obvious",
                            translation = "очевидний",
                            details = details,
                        ),
                    ),
                ),
            ),
        )

    private fun response(metadata: kotlinx.serialization.json.JsonObject): SyncResponse =
        SyncResponse(
            topics = listOf(
                TopicSyncResponse(
                    id = TopicId,
                    name = "Common",
                    color = "cover-0",
                    sourceLang = "en",
                    targetLang = "uk",
                    position = 0,
                    createdAt = "2026-08-10T00:00:00.000Z",
                    updatedAt = "2026-08-10T00:00:00.000Z",
                    wordsUpdatedAt = "2026-08-10T00:00:00.000Z",
                ),
            ),
            words = listOf(
                TopicWordSyncResponse(
                    id = WordId,
                    topicId = TopicId,
                    wordText = "obvious",
                    translationText = "очевидний",
                    source = "seed",
                    origin = "vocabee-translate/en_uk/primary",
                    metadata = metadata,
                    addedAt = "2026-08-10T00:00:00.000Z",
                    updatedAt = "2026-08-10T00:00:00.000Z",
                ),
            ),
            deletedTopicIds = emptyList(),
            deletedWordIds = emptyList(),
            serverTime = "2026-08-10T00:00:01.000Z",
            lexiconSchemaVersion = 1,
        )

    private companion object {
        const val UserId = "00000000-0000-4000-8000-000000000001"
        const val TopicId = "00000000-0000-0000-0000-000000000001"
        const val WordId = "00000000-0000-0000-0000-000000000002"
        val English = LanguageOption("en", "English", "EN", "en-US")
        val Ukrainian = LanguageOption("uk", "Ukrainian", "UA", "uk-UA")
        val SupportedLanguages = listOf(English, Ukrainian)
    }
}
