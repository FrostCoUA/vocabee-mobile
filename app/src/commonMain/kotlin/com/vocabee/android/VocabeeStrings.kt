package com.vocabee.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class VocabeeString {
    NavigationBack,
    NavigationClose,
    TabDictionary,
    TabPractice,
    TabSettings,
    HomeSummary,
    HomeTitle,
    EmptyDictionaryTitleLine1,
    EmptyDictionaryTitleLine2,
    EmptyDictionarySubtitle,
    EmptyDictionaryCreateFirst,
    NewDictionary,
    NewTopicTile,
    TopicTileStart,
    TopicPracticeCta,
    EmptyTopicTitle,
    EmptyTopicSubtitle,
    TopicActionPractice,
    TopicActionShuffle,
    JustAdded,
    KeyboardInputPlaceholder,
    Done,
    KeyboardEnterWord,
    KeyboardSearchTranslations,
    TranslationHint,
    KeyboardFoundCount,
    KeyboardTapToAdd,
    TranslationOptionSource,
    VoiceSpeakPrompt,
    VoiceHoldMic,
    VoiceReleaseToStop,
    VoiceHoldAndSpeak,
    VoiceListening,
    VoiceInitialInstruction,
    VoiceHeard,
    VoiceRetry,
    Mic,
    TranslationNotePrimary,
    TranslationNoteAlternative,
    TranslationNoteAdditional,
    TranslationNoteAlreadyAdded,
    UpdatedToday,
    UpdatedYesterday,
    SampleSentenceDefault,
    PracticeEyebrow,
    PracticeTitle,
    PracticeComingSoonLine1,
    PracticeComingSoonLine2,
    PracticeSubtitle,
    InDevelopment,
    SettingsEyebrow,
    ProfileTitle,
    ProfileStreakLabel,
    ProfileWordsLabel,
    ProfileTestsLabel,
    LanguageSectionTitle,
    SpeakingLanguageTitle,
    SpeakingLanguageSubtitle,
    LearningLanguageTitle,
    LearningLanguageSubtitle,
    NotificationsTitle,
    DarkThemeTitle,
    ProfileName,
    LanguagePickerSelectLanguage,
    LanguagePickerSearchPlaceholder,
    LanguagePickerRecent,
    LanguagePickerAll,
    TopicNotFoundTitle,
    TopicNotFoundSubtitle,
    NewTopicCancel,
    NewTopicCreate,
    NewTopicTitle,
    NewTopicSubtitle,
    NewTopicPlaceholder,
    CoverColor,
    NewTopicPreviewTitle,
    LanguageNameUkrainian,
    LanguageNameEnglish,
    LanguageNameRussian,
    LanguageNamePolish,
    LanguageNameGerman,
    LanguageNameSpanish,
    LanguageNativeNameUkrainian,
    LanguageNativeNameEnglish,
    LanguageNativeNameRussian,
    LanguageNativeNamePolish,
    LanguageNativeNameGerman,
    LanguageNativeNameSpanish,
}

enum class VocabeeQuantityString {
    TopicCount,
    WordCount,
    DaysAgo,
    WeeksAgo,
}

internal interface VocabeeStringResolver {
    fun string(key: VocabeeString, vararg formatArgs: Any): String
    fun quantityString(key: VocabeeQuantityString, quantity: Int, vararg formatArgs: Any): String
}

internal val LocalVocabeeStrings = staticCompositionLocalOf<VocabeeStringResolver> {
    EnglishVocabeeStringResolver
}

@Composable
internal fun vocabeeString(key: VocabeeString, vararg formatArgs: Any): String {
    return LocalVocabeeStrings.current.string(key, *formatArgs)
}

@Composable
internal fun vocabeeQuantityString(
    key: VocabeeQuantityString,
    quantity: Int,
    vararg formatArgs: Any,
): String {
    return LocalVocabeeStrings.current.quantityString(key, quantity, *formatArgs)
}

private object EnglishVocabeeStringResolver : VocabeeStringResolver {
    override fun string(key: VocabeeString, vararg formatArgs: Any): String {
        return when (key) {
            VocabeeString.NavigationBack -> "Back"
            VocabeeString.NavigationClose -> "Close"
            VocabeeString.TabDictionary -> "Dictionaries"
            VocabeeString.TabPractice -> "Practice"
            VocabeeString.TabSettings -> "Profile"
            VocabeeString.HomeSummary -> "%1\$s · %2\$s"
            VocabeeString.HomeTitle -> "My dictionaries"
            VocabeeString.EmptyDictionaryTitleLine1 -> "Your dictionaries"
            VocabeeString.EmptyDictionaryTitleLine2 -> "will appear here"
            VocabeeString.EmptyDictionarySubtitle -> "Create a topic and add words with the keyboard or voice."
            VocabeeString.EmptyDictionaryCreateFirst -> "Create first"
            VocabeeString.NewDictionary -> "New dictionary"
            VocabeeString.NewTopicTile -> "New topic"
            VocabeeString.TopicTileStart -> "Start"
            VocabeeString.TopicPracticeCta -> "Practice"
            VocabeeString.EmptyTopicTitle -> "The list is empty"
            VocabeeString.EmptyTopicSubtitle -> "Add the first word with the keyboard or voice"
            VocabeeString.TopicActionPractice -> "ϟ Start practice"
            VocabeeString.TopicActionShuffle -> "↝ Shuffle"
            VocabeeString.JustAdded -> "JUST ADDED"
            VocabeeString.KeyboardInputPlaceholder -> "Word to translate..."
            VocabeeString.Done -> "Done"
            VocabeeString.KeyboardEnterWord -> "ENTER A WORD"
            VocabeeString.KeyboardSearchTranslations -> "SEARCHING TRANSLATIONS"
            VocabeeString.TranslationHint -> "%1\$s → %2\$s · translation will appear below"
            VocabeeString.KeyboardFoundCount -> "FOUND · %1\$d"
            VocabeeString.KeyboardTapToAdd -> "TAP ⊕ TO ADD"
            VocabeeString.TranslationOptionSource -> "from: %1\$s"
            VocabeeString.VoiceSpeakPrompt -> "speak..."
            VocabeeString.VoiceHoldMic -> "Hold the microphone"
            VocabeeString.VoiceReleaseToStop -> "RELEASE TO STOP"
            VocabeeString.VoiceHoldAndSpeak -> "HOLD AND SPEAK"
            VocabeeString.VoiceListening -> "listening"
            VocabeeString.VoiceInitialInstruction -> "Hold the microphone and say a word."
            VocabeeString.VoiceHeard -> "HEARD"
            VocabeeString.VoiceRetry -> "Not right? Hold the microphone again."
            VocabeeString.Mic -> "Mic"
            VocabeeString.TranslationNotePrimary -> "primary option"
            VocabeeString.TranslationNoteAlternative -> "alternative"
            VocabeeString.TranslationNoteAdditional -> "another option"
            VocabeeString.TranslationNoteAlreadyAdded -> "added earlier · %1\$s"
            VocabeeString.UpdatedToday -> "today"
            VocabeeString.UpdatedYesterday -> "yesterday"
            VocabeeString.SampleSentenceDefault -> "Use %1\$s in a short sentence."
            VocabeeString.PracticeEyebrow -> "KNOWLEDGE CHECK"
            VocabeeString.PracticeTitle -> "Practice"
            VocabeeString.PracticeComingSoonLine1 -> "Practice tests"
            VocabeeString.PracticeComingSoonLine2 -> "coming soon"
            VocabeeString.PracticeSubtitle -> "Here you will check whether you remember words: cards, translations, audio. For now, focus on filling your dictionaries."
            VocabeeString.InDevelopment -> "In development"
            VocabeeString.SettingsEyebrow -> "SETTINGS"
            VocabeeString.ProfileTitle -> "Profile"
            VocabeeString.ProfileStreakLabel -> "streak"
            VocabeeString.ProfileWordsLabel -> "words"
            VocabeeString.ProfileTestsLabel -> "tests"
            VocabeeString.LanguageSectionTitle -> "LANGUAGE"
            VocabeeString.SpeakingLanguageTitle -> "I speak"
            VocabeeString.SpeakingLanguageSubtitle -> "The language we translate from"
            VocabeeString.LearningLanguageTitle -> "I am learning"
            VocabeeString.LearningLanguageSubtitle -> "The language we translate to and recognize"
            VocabeeString.NotificationsTitle -> "Notifications"
            VocabeeString.DarkThemeTitle -> "Dark theme"
            VocabeeString.ProfileName -> "Anna K."
            VocabeeString.LanguagePickerSelectLanguage -> "SELECT LANGUAGE"
            VocabeeString.LanguagePickerSearchPlaceholder -> "Search language..."
            VocabeeString.LanguagePickerRecent -> "RECENT"
            VocabeeString.LanguagePickerAll -> "ALL LANGUAGES"
            VocabeeString.TopicNotFoundTitle -> "Topic not found"
            VocabeeString.TopicNotFoundSubtitle -> "Return to the topic list"
            VocabeeString.NewTopicCancel -> "Cancel"
            VocabeeString.NewTopicCreate -> "Create"
            VocabeeString.NewTopicTitle -> "New dictionary"
            VocabeeString.NewTopicSubtitle -> "Name the topic and choose a cover color."
            VocabeeString.NewTopicPlaceholder -> "Topic name"
            VocabeeString.CoverColor -> "COLOR"
            VocabeeString.NewTopicPreviewTitle -> "Topic name"
            VocabeeString.LanguageNameUkrainian -> "Ukrainian"
            VocabeeString.LanguageNameEnglish -> "English"
            VocabeeString.LanguageNameRussian -> "Russian"
            VocabeeString.LanguageNamePolish -> "Polish"
            VocabeeString.LanguageNameGerman -> "German"
            VocabeeString.LanguageNameSpanish -> "Spanish"
            VocabeeString.LanguageNativeNameUkrainian -> "Українська"
            VocabeeString.LanguageNativeNameEnglish -> "English"
            VocabeeString.LanguageNativeNameRussian -> "Русский"
            VocabeeString.LanguageNativeNamePolish -> "Polski"
            VocabeeString.LanguageNativeNameGerman -> "Deutsch"
            VocabeeString.LanguageNativeNameSpanish -> "Español"
        }.withFormatArgs(formatArgs)
    }

    override fun quantityString(
        key: VocabeeQuantityString,
        quantity: Int,
        vararg formatArgs: Any,
    ): String {
        val template = when (key) {
            VocabeeQuantityString.TopicCount -> if (quantity == 1) "%1\$d topic" else "%1\$d topics"
            VocabeeQuantityString.WordCount -> if (quantity == 1) "%1\$d word" else "%1\$d words"
            VocabeeQuantityString.DaysAgo -> if (quantity == 1) "%1\$d day ago" else "%1\$d days ago"
            VocabeeQuantityString.WeeksAgo -> if (quantity == 1) "%1\$d week ago" else "%1\$d weeks ago"
        }
        return template.withFormatArgs(formatArgs)
    }
}

private fun String.withFormatArgs(args: Array<out Any>): String {
    var result = this
    args.forEachIndexed { index, arg ->
        val value = arg.toString()
        result = result
            .replace("%${index + 1}\$s", value)
            .replace("%${index + 1}\$d", value)
    }
    return result
}
