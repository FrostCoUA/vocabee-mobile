package com.vocabee.android.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.vocabee.android.VocabeeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
sealed interface VocabeeRoute : NavKey {
    @Serializable
    data object DictionaryHome : VocabeeRoute

    @Serializable
    data class TopicDetail(val topicId: String) : VocabeeRoute

    @Serializable
    data class KeyboardInput(val topicId: String) : VocabeeRoute

    @Serializable
    data class VoiceInput(val topicId: String) : VocabeeRoute

    @Serializable
    data object Practice : VocabeeRoute

    @Serializable
    data object Settings : VocabeeRoute

    @Serializable
    data class LanguagePicker(val target: LanguagePickerTarget) : VocabeeRoute
}

@Serializable
enum class LanguagePickerTarget {
    Speaking,
    Learning,
}

val vocabeeSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(VocabeeRoute.DictionaryHome.serializer())
            subclass(VocabeeRoute.TopicDetail.serializer())
            subclass(VocabeeRoute.KeyboardInput.serializer())
            subclass(VocabeeRoute.VoiceInput.serializer())
            subclass(VocabeeRoute.Practice.serializer())
            subclass(VocabeeRoute.Settings.serializer())
            subclass(VocabeeRoute.LanguagePicker.serializer())
        }
    }
}

enum class AppTab(
    val labelKey: VocabeeString,
    val icon: String,
    val route: VocabeeRoute,
) {
    Dictionary(VocabeeString.TabDictionary, "▢", VocabeeRoute.DictionaryHome),
    Practice(VocabeeString.TabPractice, "ϟ", VocabeeRoute.Practice),
    Settings(VocabeeString.TabSettings, "♙", VocabeeRoute.Settings),
}

fun selectedTabFor(route: VocabeeRoute?): AppTab {
    return when (route) {
        VocabeeRoute.Practice -> AppTab.Practice
        VocabeeRoute.Settings -> AppTab.Settings
        is VocabeeRoute.LanguagePicker -> AppTab.Settings
        is VocabeeRoute.KeyboardInput -> AppTab.Dictionary
        is VocabeeRoute.VoiceInput -> AppTab.Dictionary
        else -> AppTab.Dictionary
    }
}
