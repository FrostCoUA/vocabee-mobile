package com.vocabee.android.feature.vocabulary.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
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
    data object Practice : VocabeeRoute

    @Serializable
    data object Settings : VocabeeRoute

    @Serializable
    data object InviteFriends : VocabeeRoute

    @Serializable
    data object HelpSupport : VocabeeRoute
}

val vocabeeSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(VocabeeRoute.DictionaryHome.serializer())
            subclass(VocabeeRoute.TopicDetail.serializer())
            subclass(VocabeeRoute.Practice.serializer())
            subclass(VocabeeRoute.InviteFriends.serializer())
            subclass(VocabeeRoute.HelpSupport.serializer())
            subclass(VocabeeRoute.Settings.serializer())
        }
    }
}

enum class AppTab(val route: VocabeeRoute) {
    Dictionary(VocabeeRoute.DictionaryHome),
    Practice(VocabeeRoute.Practice),
    Settings(VocabeeRoute.Settings),
}

fun selectedTabFor(route: VocabeeRoute?): AppTab {
    return when (route) {
        VocabeeRoute.Practice -> AppTab.Practice
        VocabeeRoute.Settings -> AppTab.Settings
        else -> AppTab.Dictionary
    }
}
