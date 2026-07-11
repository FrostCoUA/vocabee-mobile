package com.vocabee.android.core.presentation.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser

private data class PrototypePalette(
    val tint: Color,
    val ink: Color,
    val muted: Color,
    val muted2: Color,
    val muted3: Color,
    val line: Color,
    val line2: Color,
    val fieldBg: Color,
    val neutralSurface: Color,
    val background: Color,
    val stage: Color,
    val white: Color,
    val sheetSurface: Color,
    val sheetControlSurface: Color,
    val sheetHandle: Color,
    val greenSoft: Color,
    val notePeach: Color,
    val noteGreen: Color,
    val noteYellowBg: Color,
    val noteYellowBorder: Color,
    val noteYellowText: Color,
    val contextCardTop: Color,
    val contextCardBottom: Color,
    val contextCardBorder: Color,
    val emptyCardLight: Color,
    val emptyCardSoft: Color,
    val emptyCardStroke: Color,
    val emptyCardStroke2: Color,
    val emptyCardTextDark: Color,
    val emptyCardTextLight: Color,
    val emptyCardHex: Color,
    val emptyCardWordDark: Color,
    val emptyCardWordLight: Color,
    val progressTrack: Color,
    val progressRing: Color,
    val statFlameBg: Color,
    val statTrainBg: Color,
    val switchTrack: Color,
    val dividerLight: Color,
    val chipNeutralBg: Color,
    val red: Color,
    val purpleText: Color,
    val orangeText: Color,
    val greenText: Color,
    val redText: Color,
    val flameText: Color,
    val trainText: Color,
    val snack: Color,
    val snackText: Color,
)

/** Colors lifted from the prototype's CSS custom properties. */
internal object PrototypeColor {
    val Purple = Color(0xFF4F46E5)
    val PurpleDeep = Color(0xFF410FA3)
    val PurpleSoft = Color(0xFF5B50F0)
    val Blue = Color(0xFF5B7BFE)
    val Yellow = Color(0xFFFFCC00)
    val YellowText = Color(0xFF5A4500)
    val Orange = Color(0xFFF76400)
    val Green = Color(0xFF16A34A)
    val FacebookBlue = Color(0xFF1877F2)
    val AvatarStart = Color(0xFF5B50F0)
    val AvatarEnd = Color(0xFF410FA3)

    /** Fixed black overlay for the "% learned" fill on colored cards — same in both themes. */
    val CardKnowledgeFill = Color(0x21000000)

    private val lightPalette = PrototypePalette(
        tint = Color(0xFFE0E7FF),
        ink = Color(0xFF111827),
        muted = Color(0xFF6B7280),
        muted2 = Color(0xFF9CA3AF),
        muted3 = Color(0xFFC2C7D6),
        line = Color(0xFFEDEEF3),
        line2 = Color(0xFFF1F2F6),
        fieldBg = Color(0xFFF5F6FA),
        neutralSurface = Color(0xFFF3F4F8),
        background = Color(0xFFF6F6F9),
        stage = Color(0xFFE8E8EE),
        white = Color(0xFFFFFFFF),
        sheetSurface = Color(0xFFFFFFFF),
        sheetControlSurface = Color(0xFFF3F4F8),
        sheetHandle = Color(0xFFE2E4EC),
        greenSoft = Color(0xFFE3F7EC),
        notePeach = Color(0xFFFFEDE0),
        noteGreen = Color(0xFFE3F7EC),
        noteYellowBg = Color(0xFFFFF8E6),
        noteYellowBorder = Color(0xFFFBE6A8),
        noteYellowText = Color(0xFF8A6400),
        contextCardTop = Color(0xFFF7F8FC),
        contextCardBottom = Color(0xFFF4F5FB),
        contextCardBorder = Color(0xFFEDEFF7),
        emptyCardLight = Color(0xFFEEF0FB),
        emptyCardSoft = Color(0xFFF4F5FB),
        emptyCardStroke = Color(0xFFE5E7F2),
        emptyCardStroke2 = Color(0xFFE7E9F4),
        emptyCardTextDark = Color(0xFFDDE3F7),
        emptyCardTextLight = Color(0xFFE9ECF8),
        emptyCardHex = Color(0xFFC7D2FE),
        emptyCardWordDark = Color(0xFFE2E6F4),
        emptyCardWordLight = Color(0xFFEBEDF7),
        progressTrack = Color(0xFFE6E8F0),
        progressRing = Color(0xFFEEF0FB),
        statFlameBg = Color(0xFFFFF4D6),
        statTrainBg = Color(0xFFE6F6F1),
        switchTrack = Color(0xFFD8DAE3),
        dividerLight = Color(0xFFF1F2F6),
        chipNeutralBg = Color(0xFFE7E9F0),
        red = Color(0xFFDC2626),
        purpleText = Color(0xFF4F46E5),
        orangeText = Color(0xFFC2410C),
        greenText = Color(0xFF15803D),
        redText = Color(0xFFDC2626),
        flameText = Color(0xFFE0820C),
        trainText = Color(0xFF0E9FA5),
        snack = Color(0xFF1F2430),
        snackText = Color(0xFFFFFFFF),
    )

    private val darkPalette = PrototypePalette(
        tint = Color(0xFF2C3060),
        ink = Color(0xFFF3F5FA),
        muted = Color(0xFFAEB7C8),
        muted2 = Color(0xFF7D8699),
        muted3 = Color(0xFF59627A),
        line = Color(0xFF2A3143),
        line2 = Color(0xFF232938),
        fieldBg = Color(0xFF1B2130),
        neutralSurface = Color(0xFF232939),
        background = Color(0xFF0F131D),
        stage = Color(0xFF111827),
        white = Color(0xFF171D2A),
        sheetSurface = Color(0xFF1E2433),
        sheetControlSurface = Color(0xFF2A3143),
        sheetHandle = Color(0xFF3B4358),
        greenSoft = Color(0xFF123526),
        notePeach = Color(0xFF3B2417),
        noteGreen = Color(0xFF123526),
        noteYellowBg = Color(0xFF332A12),
        noteYellowBorder = Color(0xFF6A5115),
        noteYellowText = Color(0xFFFFD978),
        contextCardTop = Color(0xFF1C2231),
        contextCardBottom = Color(0xFF191F2C),
        contextCardBorder = Color(0xFF2A3143),
        emptyCardLight = Color(0xFF202738),
        emptyCardSoft = Color(0xFF1B2130),
        emptyCardStroke = Color(0xFF30384B),
        emptyCardStroke2 = Color(0xFF293041),
        emptyCardTextDark = Color(0xFF384159),
        emptyCardTextLight = Color(0xFF30384B),
        emptyCardHex = Color(0xFF394269),
        emptyCardWordDark = Color(0xFF384159),
        emptyCardWordLight = Color(0xFF30384B),
        progressTrack = Color(0xFF2A3143),
        progressRing = Color(0xFF222839),
        statFlameBg = Color(0xFF392817),
        statTrainBg = Color(0xFF123437),
        switchTrack = Color(0xFF333B50),
        dividerLight = Color(0xFF232938),
        chipNeutralBg = Color(0xFF2A3143),
        red = Color(0xFFE5484D),
        purpleText = Color(0xFF9AA4FF),
        orangeText = Color(0xFFFF9A62),
        greenText = Color(0xFF43D17C),
        redText = Color(0xFFFF7B81),
        flameText = Color(0xFFFFB65C),
        trainText = Color(0xFF3ECAD1),
        snack = Color(0xFF2A3143),
        snackText = Color(0xFFF3F5FA),
    )

    private var palette = lightPalette

    fun useDarkTheme(enabled: Boolean) {
        palette = if (enabled) darkPalette else lightPalette
    }

    val Tint: Color get() = palette.tint
    val Ink: Color get() = palette.ink
    val Muted: Color get() = palette.muted
    val Muted2: Color get() = palette.muted2
    val Muted3: Color get() = palette.muted3
    val Line: Color get() = palette.line
    val Line2: Color get() = palette.line2
    val FieldBg: Color get() = palette.fieldBg
    val NeutralSurface: Color get() = palette.neutralSurface
    val Background: Color get() = palette.background
    val Stage: Color get() = palette.stage
    val White: Color get() = palette.white
    val SheetSurface: Color get() = palette.sheetSurface
    val SheetControlSurface: Color get() = palette.sheetControlSurface
    val SheetHandle: Color get() = palette.sheetHandle
    val GreenSoft: Color get() = palette.greenSoft
    val NotePeach: Color get() = palette.notePeach
    val NoteGreen: Color get() = palette.noteGreen
    val NoteYellowBg: Color get() = palette.noteYellowBg
    val NoteYellowBorder: Color get() = palette.noteYellowBorder
    val NoteYellowText: Color get() = palette.noteYellowText
    val ContextCardTop: Color get() = palette.contextCardTop
    val ContextCardBottom: Color get() = palette.contextCardBottom
    val ContextCardBorder: Color get() = palette.contextCardBorder
    val EmptyCardLight: Color get() = palette.emptyCardLight
    val EmptyCardSoft: Color get() = palette.emptyCardSoft
    val EmptyCardStroke: Color get() = palette.emptyCardStroke
    val EmptyCardStroke2: Color get() = palette.emptyCardStroke2
    val EmptyCardTextDark: Color get() = palette.emptyCardTextDark
    val EmptyCardTextLight: Color get() = palette.emptyCardTextLight
    val EmptyCardHex: Color get() = palette.emptyCardHex
    val EmptyCardWordDark: Color get() = palette.emptyCardWordDark
    val EmptyCardWordLight: Color get() = palette.emptyCardWordLight
    val ProgressTrack: Color get() = palette.progressTrack
    val ProgressRing: Color get() = palette.progressRing
    val StatFlameBg: Color get() = palette.statFlameBg
    val StatTrainBg: Color get() = palette.statTrainBg
    val SwitchTrack: Color get() = palette.switchTrack
    val DividerLight: Color get() = palette.dividerLight
    val ChipNeutralBg: Color get() = palette.chipNeutralBg

    /** Red for fills (delete). Brighter in dark for legibility. */
    val Red: Color get() = palette.red

    /** Theme-aware accent colors for text/icons on themed surfaces — lighter in dark. */
    val PurpleText: Color get() = palette.purpleText
    val OrangeText: Color get() = palette.orangeText
    val GreenText: Color get() = palette.greenText
    val RedText: Color get() = palette.redText
    val StatFlameText: Color get() = palette.flameText
    val StatTrainText: Color get() = palette.trainText

    /** Snackbar surface + text. */
    val Snack: Color get() = palette.snack
    val SnackText: Color get() = palette.snackText
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
    Dumbbell,
    Copy,
    Plane,
    Film,
    Brief,
    Grad,
    Food,
    Ball,
    Music,
    Leaf,
    Laptop,
    Bag,
    Heart,
    Child,
    Chat,
}

/** Topic cover icons in prototype order (matches P.ICON_TOPICS in the redesign). */
internal val PrototypeTopicIcons: List<PrototypeIcon> = listOf(
    PrototypeIcon.Plane,
    PrototypeIcon.Book,
    PrototypeIcon.Film,
    PrototypeIcon.Brief,
    PrototypeIcon.Grad,
    PrototypeIcon.Food,
    PrototypeIcon.Ball,
    PrototypeIcon.Music,
    PrototypeIcon.Leaf,
    PrototypeIcon.Laptop,
    PrototypeIcon.Bag,
    PrototypeIcon.Heart,
    PrototypeIcon.Child,
    PrototypeIcon.Chat,
    PrototypeIcon.Star,
)

internal fun prototypeTopicIcon(iconIndex: Int): PrototypeIcon {
    val size = PrototypeTopicIcons.size
    val idx = ((iconIndex % size) + size) % size
    return PrototypeTopicIcons[idx]
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
        // Draws an SVG path defined in the 24×24 viewBox, scaled/centered like the helpers above.
        fun svg(pathData: String) {
            val parsed = PathParser().parsePathString(pathData).toPath()
            val matrix = Matrix()
            matrix.translate(
                (size.width - 24f * unit) / 2f,
                (size.height - 24f * unit) / 2f,
            )
            matrix.scale(unit, unit)
            parsed.transform(matrix)
            drawPath(path = parsed, color = color, style = stroke)
        }

        when (icon) {
            PrototypeIcon.Book -> {
                path {
                    moveTo(x(4.5f), y(5.5f))
                    cubicTo(x(7.2f), y(4.3f), x(10f), y(4.5f), x(12f), y(6.4f))
                    lineTo(x(12f), y(19f))
                    cubicTo(x(10f), y(17.2f), x(7.2f), y(16.9f), x(4.5f), y(18.1f))
                    close()
                }
                path {
                    moveTo(x(19.5f), y(5.5f))
                    cubicTo(x(16.8f), y(4.3f), x(14f), y(4.5f), x(12f), y(6.4f))
                    lineTo(x(12f), y(19f))
                    cubicTo(x(14f), y(17.2f), x(16.8f), y(16.9f), x(19.5f), y(18.1f))
                    close()
                }
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
            PrototypeIcon.Dumbbell -> {
                line(9.6f, 12f, 14.4f, 12f)
                rect(5.9f, 7.9f, 3.3f, 8.2f, 1.5f)
                rect(14.8f, 7.9f, 3.3f, 8.2f, 1.5f)
                rect(2.9f, 9.9f, 2.1f, 4.2f, 1f)
                rect(19f, 9.9f, 2.1f, 4.2f, 1f)
            }
            PrototypeIcon.Copy -> {
                rect(9f, 9f, 11f, 11f, 2.5f)
                path {
                    moveTo(x(15f), y(9f))
                    lineTo(x(15f), y(6.5f))
                    quadraticTo(x(15f), y(4f), x(12.5f), y(4f))
                    lineTo(x(6.5f), y(4f))
                    quadraticTo(x(4f), y(4f), x(4f), y(6.5f))
                    lineTo(x(4f), y(12.5f))
                    quadraticTo(x(4f), y(15f), x(6.5f), y(15f))
                    lineTo(x(9f), y(15f))
                }
            }
            PrototypeIcon.Plane -> {
                svg("M21 15.5 3 11l2-3 4 1 4-5 2 1-2 4 5 1 1.5 2.5a1.4 1.4 0 0 1-1.5 2Z")
                svg("M6 20h9")
            }
            PrototypeIcon.Film -> {
                rect(3f, 5f, 18f, 14f, 2.5f)
                svg("M7 5v14M17 5v14M3 9.5h4M3 14.5h4M17 9.5h4M17 14.5h4")
            }
            PrototypeIcon.Brief -> {
                rect(3f, 7f, 18f, 13f, 2.5f)
                svg("M8 7V5.5A2.5 2.5 0 0 1 10.5 3h3A2.5 2.5 0 0 1 16 5.5V7M3 12.5h18")
            }
            PrototypeIcon.Grad -> {
                svg("M12 4 2 9l10 5 10-5-10-5Z")
                svg("M6 11.2V16c0 1 2.7 2.5 6 2.5s6-1.5 6-2.5v-4.8M22 9v5")
            }
            PrototypeIcon.Food -> {
                svg("M6 3v8M9 3v8M7.5 11v10M6 6.5h3")
                svg("M16 3c-1.5 1-2 3-2 5s.5 3 2 3 2-1 2-3-.5-4-2-5Zm0 8v10")
            }
            PrototypeIcon.Ball -> {
                drawCircle(color = color, radius = 9f * unit, center = offset(12f, 12f), style = stroke)
                svg("M5.6 5.8c3 3.4 3 9 0 12.4M18.4 5.8c-3 3.4-3 9 0 12.4")
            }
            PrototypeIcon.Music -> {
                svg("M9 18V6l10-2v12")
                drawCircle(color = color, radius = 2.5f * unit, center = offset(6.5f, 18f), style = stroke)
                drawCircle(color = color, radius = 2.5f * unit, center = offset(16.5f, 16f), style = stroke)
            }
            PrototypeIcon.Leaf -> {
                svg("M12 21v-7")
                svg("M12 14c-4 0-6-2.5-6-6 3 0 6 1.5 6 6Z")
                svg("M12 12c0-4 2.5-6 6-6 0 3.5-2 6-6 6Z")
            }
            PrototypeIcon.Laptop -> {
                rect(4f, 4.5f, 16f, 11.5f, 2f)
                svg("M2 19.5h20")
            }
            PrototypeIcon.Bag -> {
                svg("M6 8h12l-1 12H7L6 8Z")
                svg("M9 8V6a3 3 0 0 1 6 0v2")
            }
            PrototypeIcon.Heart -> {
                svg("M12 20S4 15 4 9a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 9c0 6-8 11-8 11Z")
            }
            PrototypeIcon.Child -> {
                drawCircle(color = color, radius = 3f * unit, center = offset(12f, 6.8f), style = stroke)
                svg("M6 20c0-3.6 2.7-5.8 6-5.8s6 2.2 6 5.8")
                svg("M9.4 3.6 8.2 2.2M14.6 3.6l1.2-1.4")
            }
            PrototypeIcon.Chat -> {
                svg("M12 4c5 0 9 3 9 6.8s-4 6.8-9 6.8c-1 0-2-.1-2.9-.4L5 19.4l.8-3C4.1 15 3 13 3 10.8 3 7 7 4 12 4Z")
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
