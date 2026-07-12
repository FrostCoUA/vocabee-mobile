package com.vocabee.android.core.presentation.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.vocabee.android.resources.Res
import com.vocabee.android.resources.manrope_bold
import com.vocabee.android.resources.manrope_extrabold
import com.vocabee.android.resources.manrope_medium
import com.vocabee.android.resources.manrope_regular
import com.vocabee.android.resources.manrope_semibold
import org.jetbrains.compose.resources.Font

/**
 * Manrope — the typeface every design board is drawn in. Bundled for both
 * platforms so text metrics finally match the mocks (system fonts are wider
 * on iOS, which caused truncated labels).
 */
@Composable
fun manropeFamily(): FontFamily = FontFamily(
    Font(Res.font.manrope_regular, FontWeight.Normal),
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope_semibold, FontWeight.SemiBold),
    Font(Res.font.manrope_bold, FontWeight.Bold),
    Font(Res.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Material3 typography with every style re-based onto Manrope. */
@Composable
fun manropeTypography(): Typography {
    val family = manropeFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}
