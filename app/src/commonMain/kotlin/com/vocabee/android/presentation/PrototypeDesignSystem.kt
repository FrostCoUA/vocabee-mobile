package com.vocabee.android.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/** Colors lifted from the prototype's CSS custom properties. */
internal object PrototypeColor {
    val Purple = Color(0xFF4F46E5)
    val PurpleDeep = Color(0xFF410FA3)
    val PurpleSoft = Color(0xFF5B50F0)
    val Blue = Color(0xFF5B7BFE)
    val Tint = Color(0xFFE0E7FF)
    val Yellow = Color(0xFFFFCC00)
    val YellowText = Color(0xFF5A4500)
    val Orange = Color(0xFFF76400)
    val Ink = Color(0xFF111827)
    val Muted = Color(0xFF6B7280)
    val Muted2 = Color(0xFF9CA3AF)
    val Muted3 = Color(0xFFC2C7D6)
    val Line = Color(0xFFEDEEF3)
    val Line2 = Color(0xFFF1F2F6)
    val FieldBg = Color(0xFFF5F6FA)
    val NeutralSurface = Color(0xFFF3F4F8)
    val Background = Color(0xFFF6F6F9)
    val Stage = Color(0xFFE8E8EE)
    val White = Color(0xFFFFFFFF)
    val Green = Color(0xFF16A34A)
    val GreenSoft = Color(0xFFE3F7EC)
    val Red = Color(0xFFDC2626)
    val NotePeach = Color(0xFFFFEDE0)
    val NoteGreen = Color(0xFFE3F7EC)
    val NoteYellowBg = Color(0xFFFFF8E6)
    val NoteYellowBorder = Color(0xFFFBE6A8)
    val NoteYellowText = Color(0xFF8A6400)
    val ContextCardTop = Color(0xFFF7F8FC)
    val ContextCardBottom = Color(0xFFF4F5FB)
    val ContextCardBorder = Color(0xFFEDEFF7)
    val EmptyCardLight = Color(0xFFEEF0FB)
    val EmptyCardSoft = Color(0xFFF4F5FB)
    val EmptyCardStroke = Color(0xFFE5E7F2)
    val EmptyCardStroke2 = Color(0xFFE7E9F4)
    val EmptyCardTextDark = Color(0xFFDDE3F7)
    val EmptyCardTextLight = Color(0xFFE9ECF8)
    val EmptyCardHex = Color(0xFFC7D2FE)
    val EmptyCardWordDark = Color(0xFFE2E6F4)
    val EmptyCardWordLight = Color(0xFFEBEDF7)
    val ProgressTrack = Color(0xFFE6E8F0)
    val ProgressRing = Color(0xFFEEF0FB)
    val FacebookBlue = Color(0xFF1877F2)
    val StatFlameBg = Color(0xFFFFF4D6)
    val StatFlameText = Color(0xFFE0820C)
    val StatTrainBg = Color(0xFFE6F6F1)
    val StatTrainText = Color(0xFF0E9FA5)
    val SwitchTrack = Color(0xFFD8DAE3)
    val DividerLight = Color(0xFFF1F2F6)
    val ChipNeutralBg = Color(0xFFE7E9F0)
    val AvatarStart = Color(0xFF5B50F0)
    val AvatarEnd = Color(0xFF410FA3)
}

/** Theme accent colors for dictionary covers — kept in prototype order. */
internal val PrototypeTopicThemes: List<PrototypeTopicTheme> = listOf(
    PrototypeTopicTheme("indigo", Color(0xFF4F46E5)),
    PrototypeTopicTheme("blue", Color(0xFF5B7BFE)),
    PrototypeTopicTheme("violet", Color(0xFF7C5CF6)),
    PrototypeTopicTheme("grape", Color(0xFF410FA3)),
    PrototypeTopicTheme("royal", Color(0xFF3E63DD)),
    PrototypeTopicTheme("plum", Color(0xFF9333EA)),
    PrototypeTopicTheme("teal", Color(0xFF0E9FA5)),
    PrototypeTopicTheme("amber", Color(0xFFE0820C)),
)

internal data class PrototypeTopicTheme(
    val key: String,
    val color: Color,
)

internal fun prototypeTopicTheme(coverIndex: Int): PrototypeTopicTheme {
    val size = PrototypeTopicThemes.size
    val idx = ((coverIndex % size) + size) % size
    return PrototypeTopicThemes[idx]
}

internal enum class PrototypeIcon {
    Book,
    Cards,
    User,
    Plus,
    Mic,
    Sound,
    Sparkle,
    Check,
    ChevronRight,
    ChevronLeft,
    ChevronDown,
    Search,
    Close,
    Globe,
    Bell,
    Moon,
    Edit,
    Help,
    Invite,
    Flame,
    Bookmark,
    Star,
    ArrowRight,
}

@Composable
internal fun PrototypeLineIcon(
    icon: PrototypeIcon,
    modifier: Modifier = Modifier,
    color: Color = PrototypeColor.Ink,
    strokeWidth: Float = 1.9f,
) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(
            width = strokeWidth * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun x(value: Float) = value * unit + (size.width - 24f * unit) / 2f
        fun y(value: Float) = value * unit + (size.height - 24f * unit) / 2f
        fun offset(xValue: Float, yValue: Float) = Offset(x(xValue), y(yValue))
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(
                color = color,
                start = offset(x1, y1),
                end = offset(x2, y2),
                strokeWidth = stroke.width,
                cap = StrokeCap.Round,
            )
        }
        fun path(block: Path.() -> Unit) {
            val path = Path().apply(block)
            drawPath(path = path, color = color, style = stroke)
        }
        fun rect(left: Float, top: Float, width: Float, height: Float, radius: Float) {
            drawRoundRect(
                color = color,
                topLeft = offset(left, top),
                size = Size(width * unit, height * unit),
                cornerRadius = CornerRadius(radius * unit, radius * unit),
                style = stroke,
            )
        }

        when (icon) {
            PrototypeIcon.Book -> {
                path {
                    moveTo(x(4f), y(5.5f))
                    quadraticTo(x(4f), y(3f), x(6.5f), y(3f))
                    lineTo(x(20f), y(3f))
                    lineTo(x(20f), y(18f))
                    lineTo(x(6.5f), y(18f))
                    quadraticTo(x(4f), y(18f), x(4f), y(20.5f))
                    close()
                }
                line(4f, 20.5f, 20f, 20.5f)
            }
            PrototypeIcon.Cards -> {
                rect(3f, 6f, 13f, 15f, 2.5f)
                path {
                    moveTo(x(8f), y(3f))
                    lineTo(x(18f), y(3f))
                    quadraticTo(x(20.5f), y(3f), x(20.5f), y(5.5f))
                    lineTo(x(20.5f), y(17f))
                }
            }
            PrototypeIcon.User -> {
                drawCircle(color = color, radius = 4f * unit, center = offset(12f, 8f), style = stroke)
                path {
                    moveTo(x(5f), y(20f))
                    cubicTo(x(5f), y(16.5f), x(8.1f), y(14f), x(12f), y(14f))
                    cubicTo(x(15.9f), y(14f), x(19f), y(16.5f), x(19f), y(20f))
                }
            }
            PrototypeIcon.Plus -> {
                line(12f, 5f, 12f, 19f)
                line(5f, 12f, 19f, 12f)
            }
            PrototypeIcon.Mic -> {
                rect(9f, 2.5f, 6f, 12f, 3f)
                path {
                    moveTo(x(5.5f), y(11.5f))
                    cubicTo(x(5.5f), y(15.1f), x(8.4f), y(18f), x(12f), y(18f))
                    cubicTo(x(15.6f), y(18f), x(18.5f), y(15.1f), x(18.5f), y(11.5f))
                }
                line(12f, 18f, 12f, 21f)
            }
            PrototypeIcon.Sound -> {
                path {
                    moveTo(x(4f), y(9f))
                    lineTo(x(8f), y(9f))
                    lineTo(x(13f), y(5f))
                    lineTo(x(13f), y(19f))
                    lineTo(x(8f), y(15f))
                    lineTo(x(4f), y(15f))
                    close()
                }
                path {
                    moveTo(x(17f), y(9.5f))
                    cubicTo(x(18.2f), y(10.8f), x(18.2f), y(13.2f), x(17f), y(14.5f))
                }
                path {
                    moveTo(x(19.5f), y(7f))
                    cubicTo(x(22f), y(9.6f), x(22f), y(14.4f), x(19.5f), y(17f))
                }
            }
            PrototypeIcon.Sparkle -> {
                path {
                    moveTo(x(12f), y(3f))
                    lineTo(x(13.8f), y(8.2f))
                    lineTo(x(19f), y(10f))
                    lineTo(x(13.8f), y(11.8f))
                    lineTo(x(12f), y(17f))
                    lineTo(x(10.2f), y(11.8f))
                    lineTo(x(5f), y(10f))
                    lineTo(x(10.2f), y(8.2f))
                    close()
                }
                line(19f, 3.5f, 20.5f, 8.2f)
                line(5f, 17f, 6.2f, 20.2f)
            }
            PrototypeIcon.Check -> {
                line(5f, 12.5f, 9.5f, 17f)
                line(9.5f, 17f, 19f, 7f)
            }
            PrototypeIcon.ChevronRight -> path {
                moveTo(x(9f), y(5f))
                lineTo(x(16f), y(12f))
                lineTo(x(9f), y(19f))
            }
            PrototypeIcon.ChevronLeft -> path {
                moveTo(x(15f), y(5f))
                lineTo(x(8f), y(12f))
                lineTo(x(15f), y(19f))
            }
            PrototypeIcon.ChevronDown -> path {
                moveTo(x(5f), y(9f))
                lineTo(x(12f), y(16f))
                lineTo(x(19f), y(9f))
            }
            PrototypeIcon.Search -> {
                drawCircle(color = color, radius = 7f * unit, center = offset(11f, 11f), style = stroke)
                line(16f, 16f, 20f, 20f)
            }
            PrototypeIcon.Close -> {
                line(6f, 6f, 18f, 18f)
                line(18f, 6f, 6f, 18f)
            }
            PrototypeIcon.Globe -> {
                drawCircle(color = color, radius = 9f * unit, center = offset(12f, 12f), style = stroke)
                line(3f, 12f, 21f, 12f)
                path {
                    moveTo(x(12f), y(3f))
                    cubicTo(x(15f), y(6f), x(15f), y(18f), x(12f), y(21f))
                    cubicTo(x(9f), y(18f), x(9f), y(6f), x(12f), y(3f))
                }
            }
            PrototypeIcon.Bell -> {
                path {
                    moveTo(x(6f), y(9f))
                    cubicTo(x(6f), y(5.7f), x(8.7f), y(3f), x(12f), y(3f))
                    cubicTo(x(15.3f), y(3f), x(18f), y(5.7f), x(18f), y(9f))
                    cubicTo(x(18f), y(14f), x(20f), y(15f), x(20f), y(15f))
                    lineTo(x(4f), y(15f))
                    cubicTo(x(4f), y(15f), x(6f), y(14f), x(6f), y(9f))
                }
                path {
                    moveTo(x(10f), y(20f))
                    cubicTo(x(10.5f), y(21.3f), x(13.5f), y(21.3f), x(14f), y(20f))
                }
            }
            PrototypeIcon.Moon -> path {
                moveTo(x(20f), y(14.5f))
                cubicTo(x(18.5f), y(18.5f), x(13.8f), y(20.5f), x(9.5f), y(19f))
                cubicTo(x(4.8f), y(17.3f), x(2.4f), y(12.1f), x(4.1f), y(7.5f))
                cubicTo(x(5.1f), y(4.8f), x(7.2f), y(3.1f), x(9.5f), y(4f))
                cubicTo(x(8.3f), y(8.3f), x(12f), y(15.7f), x(20f), y(14.5f))
            }
            PrototypeIcon.Edit -> {
                line(14f, 5f, 19f, 10f)
                path {
                    moveTo(x(4f), y(20f))
                    lineTo(x(5f), y(16f))
                    lineTo(x(16f), y(5f))
                    lineTo(x(19f), y(8f))
                    lineTo(x(8f), y(19f))
                    close()
                }
            }
            PrototypeIcon.Help -> {
                drawCircle(color = color, radius = 9f * unit, center = offset(12f, 12f), style = stroke)
                path {
                    moveTo(x(9.5f), y(9.5f))
                    cubicTo(x(10f), y(7.4f), x(13.7f), y(7.4f), x(14.3f), y(9.6f))
                    cubicTo(x(14.8f), y(11.5f), x(12f), y(11.8f), x(12f), y(13.6f))
                }
                drawCircle(color = color, radius = 0.8f * unit, center = offset(12f, 17f))
            }
            PrototypeIcon.Invite -> {
                drawCircle(color = color, radius = 3.5f * unit, center = offset(9f, 8f), style = stroke)
                path {
                    moveTo(x(3f), y(20f))
                    cubicTo(x(3f), y(16.7f), x(5.7f), y(14.5f), x(9f), y(14.5f))
                    cubicTo(x(12.3f), y(14.5f), x(15f), y(16.7f), x(15f), y(20f))
                }
                line(18f, 8f, 18f, 14f)
                line(15f, 11f, 21f, 11f)
            }
            PrototypeIcon.Flame -> path {
                moveTo(x(12f), y(3f))
                cubicTo(x(13f), y(6f), x(10.5f), y(7f), x(10.5f), y(9.5f))
                cubicTo(x(10.5f), y(10.9f), x(11.6f), y(11.5f), x(12f), y(11.5f))
                cubicTo(x(12.4f), y(11.5f), x(13.5f), y(10.9f), x(13.5f), y(9.5f))
                cubicTo(x(13.5f), y(8f), x(16f), y(9f), x(16f), y(13f))
                cubicTo(x(16f), y(17.4f), x(8f), y(17.4f), x(8f), y(13f))
                cubicTo(x(8f), y(11f), x(9f), y(10f), x(9f), y(9f))
                cubicTo(x(6.5f), y(9.5f), x(6.5f), y(12f), x(6.5f), y(12.5f))
                cubicTo(x(6.5f), y(18.8f), x(18f), y(18.8f), x(18f), y(13f))
                cubicTo(x(18f), y(7f), x(12f), y(9f), x(12f), y(3f))
            }
            PrototypeIcon.Bookmark -> path {
                moveTo(x(6f), y(4f))
                lineTo(x(18f), y(4f))
                lineTo(x(18f), y(20f))
                lineTo(x(12f), y(16f))
                lineTo(x(6f), y(20f))
                close()
            }
            PrototypeIcon.Star -> path {
                moveTo(x(12f), y(3.5f))
                lineTo(x(14.6f), y(8.8f))
                lineTo(x(20.5f), y(9.7f))
                lineTo(x(16.2f), y(13.8f))
                lineTo(x(17.2f), y(19.6f))
                lineTo(x(12f), y(17f))
                lineTo(x(6.8f), y(19.6f))
                lineTo(x(7.8f), y(13.8f))
                lineTo(x(3.5f), y(9.7f))
                lineTo(x(9.4f), y(8.8f))
                close()
            }
            PrototypeIcon.ArrowRight -> {
                line(5f, 12f, 19f, 12f)
                path {
                    moveTo(x(13f), y(6f))
                    lineTo(x(19f), y(12f))
                    lineTo(x(13f), y(18f))
                }
            }
        }
    }
}

@Composable
internal fun PrototypeLogo(
    modifier: Modifier = Modifier,
    color: Color = PrototypeColor.Purple,
    accent: Color = PrototypeColor.Yellow,
) {
    Canvas(modifier) {
        val scale = size.minDimension / 116f
        val origin = Offset(size.width / 2f, size.height / 2f)
        val points = listOf(
            Offset(26f, 0f),
            Offset(13f, 22.5f),
            Offset(-13f, 22.5f),
            Offset(-26f, 0f),
            Offset(-13f, -22.5f),
            Offset(13f, -22.5f),
        )

        fun drawHex(centerX: Float, centerY: Float, fill: Color) {
            val path = Path().apply {
                val first = points.first()
                moveTo(origin.x + (centerX + first.x) * scale, origin.y + (centerY + first.y) * scale)
                points.drop(1).forEach { point ->
                    lineTo(origin.x + (centerX + point.x) * scale, origin.y + (centerY + point.y) * scale)
                }
                close()
            }
            drawPath(path = path, color = fill)
            drawPath(
                path = path,
                color = fill,
                style = Stroke(width = 6f * scale, join = StrokeJoin.Round),
            )
        }

        drawHex(-19.5f, 0f, color)
        drawHex(19.5f, -22.5f, color)
        drawHex(19.5f, 22.5f, accent)
    }
}

@Composable
internal fun HoneycombWatermark(
    modifier: Modifier = Modifier,
    color: Color = PrototypeColor.White.copy(alpha = 0.16f),
) {
    Canvas(modifier) {
        val scale = size.minDimension / 116f
        val origin = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 5f * scale, join = StrokeJoin.Round)
        val points = listOf(
            Offset(26f, 0f),
            Offset(13f, 22.5f),
            Offset(-13f, 22.5f),
            Offset(-26f, 0f),
            Offset(-13f, -22.5f),
            Offset(13f, -22.5f),
        )
        listOf(
            Offset(-19.5f, 0f),
            Offset(19.5f, -22.5f),
            Offset(19.5f, 22.5f),
        ).forEach { center ->
            val path = Path().apply {
                val first = points.first()
                moveTo(origin.x + (center.x + first.x) * scale, origin.y + (center.y + first.y) * scale)
                points.drop(1).forEach { point ->
                    lineTo(origin.x + (center.x + point.x) * scale, origin.y + (center.y + point.y) * scale)
                }
                close()
            }
            drawPath(path = path, color = color, style = stroke)
        }
    }
}

/**
 * Language descriptor for the prototype-only UI. Code matches LanguageOption.code so
 * dictionary lookups stay aligned.
 */
internal data class PrototypeLanguage(
    val code: String,
    val name: String,
    val flag: String,
)

internal val PrototypeLanguages: List<PrototypeLanguage> = listOf(
    PrototypeLanguage("uk", "Українська", "🇺🇦"),
    PrototypeLanguage("en", "Англійська", "🇬🇧"),
    PrototypeLanguage("de", "Німецька", "🇩🇪"),
    PrototypeLanguage("es", "Іспанська", "🇪🇸"),
    PrototypeLanguage("fr", "Французька", "🇫🇷"),
    PrototypeLanguage("pl", "Польська", "🇵🇱"),
    PrototypeLanguage("it", "Італійська", "🇮🇹"),
)

internal fun prototypeLanguage(code: String): PrototypeLanguage {
    return PrototypeLanguages.firstOrNull { it.code == code }
        ?: PrototypeLanguage(code, code, "🏳️")
}

internal fun languageFlag(code: String): String = prototypeLanguage(code).flag

internal fun languageName(code: String): String = prototypeLanguage(code).name
