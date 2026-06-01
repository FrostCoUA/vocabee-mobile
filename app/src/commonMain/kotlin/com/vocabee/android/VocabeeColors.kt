package com.vocabee.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class VocabeeColor {
    Transparent,
    White,
    Honey,
    Ink,
    Meadow,
    Sky,
    Paper,
    SoftLine,
    SurfaceVariant,
    OnSurfaceVariant,
    TextPrimary,
    TextSecondary,
    TextTertiary,
    TextMuted,
    TextFaded,
    IconMuted,
    BottomTabInactive,
    BottomTabSelectedSurface,
    AccentOrange,
    BrandOrange,
    NeutralSurface,
    InputSurface,
    DisabledSurface,
    DisabledText,
    DisabledAction,
    ChevronMuted,
    Divider,
    CoverSelectedBorder,
    TopicActionSurface,
    HeaderBlueMuted,
    TranslationRowSurface,
    TranslationRowBorder,
    VoiceListeningOuter,
    VoiceIdleOuter,
    PracticeIconBackground,
    TopicCoralBackground,
    TopicCoralBubble,
    TopicCoralAccent,
    TopicGreenBackground,
    TopicGreenBubble,
    TopicGreenAccent,
    TopicBlueBackground,
    TopicBlueBubble,
    TopicBlueAccent,
    TopicRoseBackground,
    TopicRoseBubble,
    TopicRoseAccent,
    TopicYellowBackground,
    TopicYellowBubble,
    TopicYellowAccent,
    TopicTealBackground,
    TopicTealBubble,
    TopicTealAccent,
    TopicPurpleBackground,
    TopicPurpleBubble,
    TopicPurpleAccent,
    TopicSalmonBackground,
    TopicSalmonBubble,
    TopicSalmonAccent,
    TopicSandBackground,
    TopicSandBubble,
    TopicSandAccent,
    TopicLavenderBackground,
    TopicLavenderBubble,
    TopicLavenderAccent,
    LanguageEnglish,
    LanguageRussian,
    LanguagePolish,
    LanguageGerman,
    LanguageSpanish,
}

internal interface VocabeeColorResolver {
    fun color(key: VocabeeColor): Color
}

internal val LocalVocabeeColors = staticCompositionLocalOf<VocabeeColorResolver> {
    DefaultVocabeeColorResolver
}

@Composable
internal fun vocabeeColor(key: VocabeeColor): Color {
    return LocalVocabeeColors.current.color(key)
}

private object DefaultVocabeeColorResolver : VocabeeColorResolver {
    override fun color(key: VocabeeColor): Color {
        return when (key) {
            VocabeeColor.Transparent -> Color.Transparent
            VocabeeColor.White -> Color(0xFFFFFFFF)
            VocabeeColor.Honey -> Color(0xFFFFC247)
            VocabeeColor.Ink -> Color(0xFF1D2329)
            VocabeeColor.Meadow -> Color(0xFF2F8F6B)
            VocabeeColor.Sky -> Color(0xFF4B7BEC)
            VocabeeColor.Paper -> Color(0xFFFFFBF3)
            VocabeeColor.SoftLine -> Color(0xFFE7DED0)
            VocabeeColor.SurfaceVariant -> Color(0xFFF3EDE2)
            VocabeeColor.OnSurfaceVariant -> Color(0xFF5C645E)
            VocabeeColor.TextPrimary -> Color(0xFF363029)
            VocabeeColor.TextSecondary -> Color(0xFF756B60)
            VocabeeColor.TextTertiary -> Color(0xFF8F857B)
            VocabeeColor.TextMuted -> Color(0xFF948A7D)
            VocabeeColor.TextFaded -> Color(0xFFB0A69A)
            VocabeeColor.IconMuted -> Color(0xFF9D9286)
            VocabeeColor.BottomTabInactive -> Color(0xFF8E857B)
            VocabeeColor.BottomTabSelectedSurface -> Color(0xFFFFF0E7)
            VocabeeColor.AccentOrange -> Color(0xFFE96C35)
            VocabeeColor.BrandOrange -> Color(0xFFF26C2F)
            VocabeeColor.NeutralSurface -> Color(0xFFF4EFE8)
            VocabeeColor.InputSurface -> Color(0xFFF8F2EA)
            VocabeeColor.DisabledSurface -> Color(0xFFEDE1D0)
            VocabeeColor.DisabledText -> Color(0xFF8F806F)
            VocabeeColor.DisabledAction -> Color(0xFFCDBFB1)
            VocabeeColor.ChevronMuted -> Color(0xFFC0B7AC)
            VocabeeColor.Divider -> Color(0xFFF1E8DC)
            VocabeeColor.CoverSelectedBorder -> Color(0xFF4D5D43)
            VocabeeColor.TopicActionSurface -> Color(0xFFE6F2FB)
            VocabeeColor.HeaderBlueMuted -> Color(0xFF4D7296)
            VocabeeColor.TranslationRowSurface -> Color(0xFFF6FAF8)
            VocabeeColor.TranslationRowBorder -> Color(0xFFD9E8E0)
            VocabeeColor.VoiceListeningOuter -> Color(0xFFFFDEC8)
            VocabeeColor.VoiceIdleOuter -> Color(0xFFFFEEE3)
            VocabeeColor.PracticeIconBackground -> Color(0xFFD9EBC2)
            VocabeeColor.TopicCoralBackground -> Color(0xFFFFC9A8)
            VocabeeColor.TopicCoralBubble -> Color(0xFFFFE1CD)
            VocabeeColor.TopicCoralAccent -> Color(0xFF8A4829)
            VocabeeColor.TopicGreenBackground -> Color(0xFFD4E8C0)
            VocabeeColor.TopicGreenBubble -> Color(0xFFE5F2D8)
            VocabeeColor.TopicGreenAccent -> Color(0xFF486E3A)
            VocabeeColor.TopicBlueBackground -> Color(0xFFC4DDF0)
            VocabeeColor.TopicBlueBubble -> Color(0xFFD9EAF8)
            VocabeeColor.TopicBlueAccent -> Color(0xFF294D73)
            VocabeeColor.TopicRoseBackground -> Color(0xFFEFC4D0)
            VocabeeColor.TopicRoseBubble -> Color(0xFFF7D8E2)
            VocabeeColor.TopicRoseAccent -> Color(0xFF91405A)
            VocabeeColor.TopicYellowBackground -> Color(0xFFF8E197)
            VocabeeColor.TopicYellowBubble -> Color(0xFFFFEEC0)
            VocabeeColor.TopicYellowAccent -> Color(0xFF806425)
            VocabeeColor.TopicTealBackground -> Color(0xFFABDCCF)
            VocabeeColor.TopicTealBubble -> Color(0xFFC7ECE3)
            VocabeeColor.TopicTealAccent -> Color(0xFF246F61)
            VocabeeColor.TopicPurpleBackground -> Color(0xFFD9CEF2)
            VocabeeColor.TopicPurpleBubble -> Color(0xFFEAE1FA)
            VocabeeColor.TopicPurpleAccent -> Color(0xFF554281)
            VocabeeColor.TopicSalmonBackground -> Color(0xFFFFBBA6)
            VocabeeColor.TopicSalmonBubble -> Color(0xFFFFD7CB)
            VocabeeColor.TopicSalmonAccent -> Color(0xFF9A4B32)
            VocabeeColor.TopicSandBackground -> Color(0xFFE4D4AA)
            VocabeeColor.TopicSandBubble -> Color(0xFFF0E6C7)
            VocabeeColor.TopicSandAccent -> Color(0xFF705B2B)
            VocabeeColor.TopicLavenderBackground -> Color(0xFFC8B8E5)
            VocabeeColor.TopicLavenderBubble -> Color(0xFFE0D6F1)
            VocabeeColor.TopicLavenderAccent -> Color(0xFF5A4A82)
            VocabeeColor.LanguageEnglish -> Color(0xFFB44E63)
            VocabeeColor.LanguageRussian -> Color(0xFF6E7B8B)
            VocabeeColor.LanguagePolish -> Color(0xFFD64B4B)
            VocabeeColor.LanguageGerman -> Color(0xFF2F2D2C)
            VocabeeColor.LanguageSpanish -> Color(0xFFE0A11A)
        }
    }
}
