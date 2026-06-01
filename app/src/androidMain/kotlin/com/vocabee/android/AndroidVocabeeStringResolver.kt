package com.vocabee.android

import android.content.Context

internal class AndroidVocabeeStringResolver(
    private val context: Context,
) : VocabeeStringResolver {
    override fun string(key: VocabeeString, vararg formatArgs: Any): String {
        return if (formatArgs.isEmpty()) {
            context.getString(key.resId)
        } else {
            context.getString(key.resId, *formatArgs)
        }
    }

    override fun quantityString(
        key: VocabeeQuantityString,
        quantity: Int,
        vararg formatArgs: Any,
    ): String {
        return if (formatArgs.isEmpty()) {
            context.resources.getQuantityString(key.resId, quantity)
        } else {
            context.resources.getQuantityString(key.resId, quantity, *formatArgs)
        }
    }
}

private val VocabeeString.resId: Int
    get() = when (this) {
        VocabeeString.NavigationBack -> R.string.navigation_back
        VocabeeString.NavigationClose -> R.string.navigation_close
        VocabeeString.TabDictionary -> R.string.tab_dictionary
        VocabeeString.TabPractice -> R.string.tab_practice
        VocabeeString.TabSettings -> R.string.tab_settings
        VocabeeString.HomeSummary -> R.string.home_summary
        VocabeeString.HomeTitle -> R.string.home_title
        VocabeeString.EmptyDictionaryTitleLine1 -> R.string.empty_dictionary_title_line_1
        VocabeeString.EmptyDictionaryTitleLine2 -> R.string.empty_dictionary_title_line_2
        VocabeeString.EmptyDictionarySubtitle -> R.string.empty_dictionary_subtitle
        VocabeeString.EmptyDictionaryCreateFirst -> R.string.empty_dictionary_create_first
        VocabeeString.NewDictionary -> R.string.new_dictionary
        VocabeeString.NewTopicTile -> R.string.new_topic_tile
        VocabeeString.TopicTileStart -> R.string.topic_tile_start
        VocabeeString.TopicPracticeCta -> R.string.topic_practice_cta
        VocabeeString.EmptyTopicTitle -> R.string.empty_topic_title
        VocabeeString.EmptyTopicSubtitle -> R.string.empty_topic_subtitle
        VocabeeString.TopicActionPractice -> R.string.topic_action_practice
        VocabeeString.TopicActionShuffle -> R.string.topic_action_shuffle
        VocabeeString.JustAdded -> R.string.just_added
        VocabeeString.KeyboardInputPlaceholder -> R.string.keyboard_input_placeholder
        VocabeeString.Done -> R.string.done
        VocabeeString.KeyboardEnterWord -> R.string.keyboard_enter_word
        VocabeeString.KeyboardSearchTranslations -> R.string.keyboard_search_translations
        VocabeeString.TranslationHint -> R.string.translation_hint
        VocabeeString.KeyboardFoundCount -> R.string.keyboard_found_count
        VocabeeString.KeyboardTapToAdd -> R.string.keyboard_tap_to_add
        VocabeeString.TranslationOptionSource -> R.string.translation_option_source
        VocabeeString.VoiceSpeakPrompt -> R.string.voice_speak_prompt
        VocabeeString.VoiceHoldMic -> R.string.voice_hold_mic
        VocabeeString.VoiceReleaseToStop -> R.string.voice_release_to_stop
        VocabeeString.VoiceHoldAndSpeak -> R.string.voice_hold_and_speak
        VocabeeString.VoiceListening -> R.string.voice_listening
        VocabeeString.VoiceInitialInstruction -> R.string.voice_initial_instruction
        VocabeeString.VoiceHeard -> R.string.voice_heard
        VocabeeString.VoiceRetry -> R.string.voice_retry
        VocabeeString.Mic -> R.string.mic
        VocabeeString.TranslationNotePrimary -> R.string.translation_note_primary
        VocabeeString.TranslationNoteAlternative -> R.string.translation_note_alternative
        VocabeeString.TranslationNoteAdditional -> R.string.translation_note_additional
        VocabeeString.TranslationNoteAlreadyAdded -> R.string.translation_note_already_added
        VocabeeString.UpdatedToday -> R.string.updated_today
        VocabeeString.UpdatedYesterday -> R.string.updated_yesterday
        VocabeeString.SampleSentenceDefault -> R.string.sample_sentence_default
        VocabeeString.PracticeEyebrow -> R.string.practice_eyebrow
        VocabeeString.PracticeTitle -> R.string.practice_title
        VocabeeString.PracticeComingSoonLine1 -> R.string.practice_coming_soon_line_1
        VocabeeString.PracticeComingSoonLine2 -> R.string.practice_coming_soon_line_2
        VocabeeString.PracticeSubtitle -> R.string.practice_subtitle
        VocabeeString.InDevelopment -> R.string.in_development
        VocabeeString.SettingsEyebrow -> R.string.settings_eyebrow
        VocabeeString.ProfileTitle -> R.string.profile_title
        VocabeeString.ProfileStreakLabel -> R.string.profile_streak_label
        VocabeeString.ProfileWordsLabel -> R.string.profile_words_label
        VocabeeString.ProfileTestsLabel -> R.string.profile_tests_label
        VocabeeString.LanguageSectionTitle -> R.string.language_section_title
        VocabeeString.SpeakingLanguageTitle -> R.string.speaking_language_title
        VocabeeString.SpeakingLanguageSubtitle -> R.string.speaking_language_subtitle
        VocabeeString.LearningLanguageTitle -> R.string.learning_language_title
        VocabeeString.LearningLanguageSubtitle -> R.string.learning_language_subtitle
        VocabeeString.NotificationsTitle -> R.string.notifications_title
        VocabeeString.DarkThemeTitle -> R.string.dark_theme_title
        VocabeeString.ProfileName -> R.string.profile_name
        VocabeeString.LanguagePickerSelectLanguage -> R.string.language_picker_select_language
        VocabeeString.LanguagePickerSearchPlaceholder -> R.string.language_picker_search_placeholder
        VocabeeString.LanguagePickerRecent -> R.string.language_picker_recent
        VocabeeString.LanguagePickerAll -> R.string.language_picker_all
        VocabeeString.TopicNotFoundTitle -> R.string.topic_not_found_title
        VocabeeString.TopicNotFoundSubtitle -> R.string.topic_not_found_subtitle
        VocabeeString.NewTopicCancel -> R.string.new_topic_cancel
        VocabeeString.NewTopicCreate -> R.string.new_topic_create
        VocabeeString.NewTopicTitle -> R.string.new_topic_title
        VocabeeString.NewTopicSubtitle -> R.string.new_topic_subtitle
        VocabeeString.NewTopicPlaceholder -> R.string.new_topic_placeholder
        VocabeeString.CoverColor -> R.string.cover_color
        VocabeeString.NewTopicPreviewTitle -> R.string.new_topic_preview_title
        VocabeeString.LanguageNameUkrainian -> R.string.language_name_ukrainian
        VocabeeString.LanguageNameEnglish -> R.string.language_name_english
        VocabeeString.LanguageNameRussian -> R.string.language_name_russian
        VocabeeString.LanguageNamePolish -> R.string.language_name_polish
        VocabeeString.LanguageNameGerman -> R.string.language_name_german
        VocabeeString.LanguageNameSpanish -> R.string.language_name_spanish
        VocabeeString.LanguageNativeNameUkrainian -> R.string.language_native_name_ukrainian
        VocabeeString.LanguageNativeNameEnglish -> R.string.language_native_name_english
        VocabeeString.LanguageNativeNameRussian -> R.string.language_native_name_russian
        VocabeeString.LanguageNativeNamePolish -> R.string.language_native_name_polish
        VocabeeString.LanguageNativeNameGerman -> R.string.language_native_name_german
        VocabeeString.LanguageNativeNameSpanish -> R.string.language_native_name_spanish
    }

private val VocabeeQuantityString.resId: Int
    get() = when (this) {
        VocabeeQuantityString.TopicCount -> R.plurals.topic_count
        VocabeeQuantityString.WordCount -> R.plurals.word_count
        VocabeeQuantityString.DaysAgo -> R.plurals.days_ago
        VocabeeQuantityString.WeeksAgo -> R.plurals.weeks_ago
    }
