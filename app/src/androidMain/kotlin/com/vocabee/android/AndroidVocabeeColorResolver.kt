package com.vocabee.android

import android.content.Context
import androidx.compose.ui.graphics.Color

internal class AndroidVocabeeColorResolver(
    private val context: Context,
) : VocabeeColorResolver {
    override fun color(key: VocabeeColor): Color {
        return Color(context.getColor(key.resId))
    }
}

private val VocabeeColor.resId: Int
    get() = when (this) {
        VocabeeColor.Transparent -> R.color.vocabee_transparent
        VocabeeColor.White -> R.color.vocabee_white
        VocabeeColor.Honey -> R.color.vocabee_honey
        VocabeeColor.Ink -> R.color.vocabee_ink
        VocabeeColor.Meadow -> R.color.vocabee_meadow
        VocabeeColor.Sky -> R.color.vocabee_sky
        VocabeeColor.Paper -> R.color.vocabee_paper
        VocabeeColor.SoftLine -> R.color.vocabee_soft_line
        VocabeeColor.SurfaceVariant -> R.color.vocabee_surface_variant
        VocabeeColor.OnSurfaceVariant -> R.color.vocabee_on_surface_variant
        VocabeeColor.TextPrimary -> R.color.vocabee_text_primary
        VocabeeColor.TextSecondary -> R.color.vocabee_text_secondary
        VocabeeColor.TextTertiary -> R.color.vocabee_text_tertiary
        VocabeeColor.TextMuted -> R.color.vocabee_text_muted
        VocabeeColor.TextFaded -> R.color.vocabee_text_faded
        VocabeeColor.IconMuted -> R.color.vocabee_icon_muted
        VocabeeColor.BottomTabInactive -> R.color.vocabee_bottom_tab_inactive
        VocabeeColor.BottomTabSelectedSurface -> R.color.vocabee_bottom_tab_selected_surface
        VocabeeColor.AccentOrange -> R.color.vocabee_accent_orange
        VocabeeColor.BrandOrange -> R.color.vocabee_brand_orange
        VocabeeColor.NeutralSurface -> R.color.vocabee_neutral_surface
        VocabeeColor.InputSurface -> R.color.vocabee_input_surface
        VocabeeColor.DisabledSurface -> R.color.vocabee_disabled_surface
        VocabeeColor.DisabledText -> R.color.vocabee_disabled_text
        VocabeeColor.DisabledAction -> R.color.vocabee_disabled_action
        VocabeeColor.ChevronMuted -> R.color.vocabee_chevron_muted
        VocabeeColor.Divider -> R.color.vocabee_divider
        VocabeeColor.CoverSelectedBorder -> R.color.vocabee_cover_selected_border
        VocabeeColor.TopicActionSurface -> R.color.vocabee_topic_action_surface
        VocabeeColor.HeaderBlueMuted -> R.color.vocabee_header_blue_muted
        VocabeeColor.TranslationRowSurface -> R.color.vocabee_translation_row_surface
        VocabeeColor.TranslationRowBorder -> R.color.vocabee_translation_row_border
        VocabeeColor.VoiceListeningOuter -> R.color.vocabee_voice_listening_outer
        VocabeeColor.VoiceIdleOuter -> R.color.vocabee_voice_idle_outer
        VocabeeColor.PracticeIconBackground -> R.color.vocabee_practice_icon_background
        VocabeeColor.TopicCoralBackground -> R.color.vocabee_topic_coral_background
        VocabeeColor.TopicCoralBubble -> R.color.vocabee_topic_coral_bubble
        VocabeeColor.TopicCoralAccent -> R.color.vocabee_topic_coral_accent
        VocabeeColor.TopicGreenBackground -> R.color.vocabee_topic_green_background
        VocabeeColor.TopicGreenBubble -> R.color.vocabee_topic_green_bubble
        VocabeeColor.TopicGreenAccent -> R.color.vocabee_topic_green_accent
        VocabeeColor.TopicBlueBackground -> R.color.vocabee_topic_blue_background
        VocabeeColor.TopicBlueBubble -> R.color.vocabee_topic_blue_bubble
        VocabeeColor.TopicBlueAccent -> R.color.vocabee_topic_blue_accent
        VocabeeColor.TopicRoseBackground -> R.color.vocabee_topic_rose_background
        VocabeeColor.TopicRoseBubble -> R.color.vocabee_topic_rose_bubble
        VocabeeColor.TopicRoseAccent -> R.color.vocabee_topic_rose_accent
        VocabeeColor.TopicYellowBackground -> R.color.vocabee_topic_yellow_background
        VocabeeColor.TopicYellowBubble -> R.color.vocabee_topic_yellow_bubble
        VocabeeColor.TopicYellowAccent -> R.color.vocabee_topic_yellow_accent
        VocabeeColor.TopicTealBackground -> R.color.vocabee_topic_teal_background
        VocabeeColor.TopicTealBubble -> R.color.vocabee_topic_teal_bubble
        VocabeeColor.TopicTealAccent -> R.color.vocabee_topic_teal_accent
        VocabeeColor.TopicPurpleBackground -> R.color.vocabee_topic_purple_background
        VocabeeColor.TopicPurpleBubble -> R.color.vocabee_topic_purple_bubble
        VocabeeColor.TopicPurpleAccent -> R.color.vocabee_topic_purple_accent
        VocabeeColor.TopicSalmonBackground -> R.color.vocabee_topic_salmon_background
        VocabeeColor.TopicSalmonBubble -> R.color.vocabee_topic_salmon_bubble
        VocabeeColor.TopicSalmonAccent -> R.color.vocabee_topic_salmon_accent
        VocabeeColor.TopicSandBackground -> R.color.vocabee_topic_sand_background
        VocabeeColor.TopicSandBubble -> R.color.vocabee_topic_sand_bubble
        VocabeeColor.TopicSandAccent -> R.color.vocabee_topic_sand_accent
        VocabeeColor.TopicLavenderBackground -> R.color.vocabee_topic_lavender_background
        VocabeeColor.TopicLavenderBubble -> R.color.vocabee_topic_lavender_bubble
        VocabeeColor.TopicLavenderAccent -> R.color.vocabee_topic_lavender_accent
        VocabeeColor.LanguageEnglish -> R.color.vocabee_language_english
        VocabeeColor.LanguageRussian -> R.color.vocabee_language_russian
        VocabeeColor.LanguagePolish -> R.color.vocabee_language_polish
        VocabeeColor.LanguageGerman -> R.color.vocabee_language_german
        VocabeeColor.LanguageSpanish -> R.color.vocabee_language_spanish
    }
