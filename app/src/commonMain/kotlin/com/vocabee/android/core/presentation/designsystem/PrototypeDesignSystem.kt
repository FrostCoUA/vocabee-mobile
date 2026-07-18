package com.vocabee.android.core.presentation.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
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
    val notePeachBorder: Color,
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
    val scrim: Color,
    val progressFill: Color,
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
        notePeachBorder = Color(0xFFF6C9A8),
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
        scrim = Color(0x80111827),
        progressFill = Color(0x21000000),
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
        greenSoft = Color(0xFF14301F),
        notePeach = Color(0xFF3B2417),
        notePeachBorder = Color(0xFF6B3A1F),
        noteGreen = Color(0xFF14301F),
        noteYellowBg = Color(0xFF33290F),
        noteYellowBorder = Color(0xFF5C4A12),
        noteYellowText = Color(0xFFF5CE58),
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
        statTrainBg = Color(0xFF12343A),
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
        scrim = Color(0x99000000),
        progressFill = Color(0x21000000),
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
    val NotePeachBorder: Color get() = palette.notePeachBorder
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

    /** Overlay behind bottom sheets and dialogs. */
    val Scrim: Color get() = palette.scrim

    /**
     * Fixed black 13% fill for the "% learned" progress on colored covers and the practice
     * flip-card back. Intentionally identical in BOTH themes (redesign tokens board): ink-based
     * overlays used to turn white in dark. Same value as [CardKnowledgeFill].
     */
    val ProgressFill: Color get() = palette.progressFill
}

/**
 * Theme accent colors for dictionary covers — matches RD.ACCENTS in the redesign (12 accents,
 * same in both themes). The first 8 keep their legacy order so persisted coverIndex values keep
 * resolving to the same colors; rose..graphite are appended after amber. Append-only list.
 */
internal val PrototypeTopicThemes: List<PrototypeTopicTheme> = listOf(
    PrototypeTopicTheme("indigo", Color(0xFF4F46E5)),
    PrototypeTopicTheme("blue", Color(0xFF5B7BFE)),
    PrototypeTopicTheme("violet", Color(0xFF7C5CF6)),
    PrototypeTopicTheme("grape", Color(0xFF410FA3)),
    PrototypeTopicTheme("royal", Color(0xFF3E63DD)),
    PrototypeTopicTheme("plum", Color(0xFF9333EA)),
    PrototypeTopicTheme("teal", Color(0xFF0E9FA5)),
    PrototypeTopicTheme("amber", Color(0xFFE0820C)),
    PrototypeTopicTheme("rose", Color(0xFFD6336C)),
    PrototypeTopicTheme("emerald", Color(0xFF17845A)),
    PrototypeTopicTheme("navy", Color(0xFF1E40AF)),
    PrototypeTopicTheme("graphite", Color(0xFF52525B)),
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

/**
 * The redesign icon set — one entry per key of `PATHS` in `vocabee-design/redesign/rd-base.js`,
 * except `phone` (its `d` in the source is a duplicate of `star`). Geometry lives in
 * [prototypeIconShapes]; [PrototypeLineIcon] renders it.
 */
internal enum class PrototypeIcon {
    Book,

    /** Closed-book variant the redesign's tab bar uses for "Словники" (RD `bookTab`). */
    BookTab,
    Cards,
    User,

    /** Female profile silhouette (RD `userF`) — for accounts with gender 'f' in the boards. */
    UserF,
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
    Play,
    Trash,
    Dumbbell,
    Copy,
    Share,
    Clip,
    Image,
    Dots,
    Send,
    Plane,
    Car,
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
    Burger,
    Drink,
    Cat,
    Dog,
}

/**
 * Topic cover icons in CANONICAL STORAGE ORDER. Persisted `iconIndex` values are positions in
 * THIS list, so it is append-only: entries 0..14 keep the legacy (pre-redesign) picker order and
 * the redesign additions (car, burger, drink, cat, dog) are appended at 15..19. Never reorder or
 * remove entries — existing dictionaries would silently change icons. The redesign picker
 * DISPLAYS icons in a different order — see [PrototypeTopicIconsPickerOrder].
 */
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
    PrototypeIcon.Car,
    PrototypeIcon.Burger,
    PrototypeIcon.Drink,
    PrototypeIcon.Cat,
    PrototypeIcon.Dog,
)

/**
 * Icon picker DISPLAY order — mirrors P.ICON_TOPICS from the redesign (rd-parts.js).
 * A display-only permutation of [PrototypeTopicIcons]: a picker rendering this order must
 * persist canonical indices via [prototypeTopicIconStorageIndex], never the display position.
 */
internal val PrototypeTopicIconsPickerOrder: List<PrototypeIcon> = listOf(
    PrototypeIcon.Plane,
    PrototypeIcon.Car,
    PrototypeIcon.Book,
    PrototypeIcon.Film,
    PrototypeIcon.Music,
    PrototypeIcon.Ball,
    PrototypeIcon.Grad,
    PrototypeIcon.Brief,
    PrototypeIcon.Laptop,
    PrototypeIcon.Food,
    PrototypeIcon.Burger,
    PrototypeIcon.Drink,
    PrototypeIcon.Bag,
    PrototypeIcon.Cat,
    PrototypeIcon.Dog,
    PrototypeIcon.Leaf,
    PrototypeIcon.Heart,
    PrototypeIcon.Child,
    PrototypeIcon.Chat,
    PrototypeIcon.Star,
)

/**
 * Resolves a persisted `iconIndex` to its icon.
 *
 * Migration contract for the 15 → 20 redesign expansion: stored indices always follow the
 * canonical order of [PrototypeTopicIcons] — legacy positions 0..14 are untouched and the five
 * new icons only occupy appended positions 15..19. Existing dictionaries therefore resolve to
 * exactly the same icons with no data migration or remap table, and indices written by the
 * current create-sheet picker (which iterates [PrototypeTopicIcons]) stay valid as well. The new
 * redesign picker order (P.ICON_TOPICS) is applied purely at display time via
 * [PrototypeTopicIconsPickerOrder] + [prototypeTopicIconStorageIndex] for new selections.
 */
internal fun prototypeTopicIcon(iconIndex: Int): PrototypeIcon {
    val size = PrototypeTopicIcons.size
    val idx = ((iconIndex % size) + size) % size
    return PrototypeTopicIcons[idx]
}

/**
 * Canonical storage index of a topic icon — the value pickers must persist as `iconIndex`
 * regardless of their display order (see [PrototypeTopicIcons]).
 */
internal fun prototypeTopicIconStorageIndex(icon: PrototypeIcon): Int =
    PrototypeTopicIcons.indexOf(icon).coerceAtLeast(0)

/**
 * One drawable element of a redesign icon, expressed in the source 24×24 viewBox
 * (`PATHS` + `RD.ic` in `vocabee-design/redesign/rd-base.js`).
 *
 * [filled] mirrors `fill="currentColor" stroke="none"` on the source element — those icons are
 * solid silhouettes. Everything else is stroked with round caps/joins, like `RD.ic` does.
 */
internal sealed interface PrototypeIconShape {
    val filled: Boolean

    /** `<path d="…"/>` — raw SVG path data, parsed with [PathParser]. */
    data class SvgPath(val pathData: String, override val filled: Boolean) : PrototypeIconShape

    /** `<rect x y width height rx/>`. */
    data class SvgRect(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val cornerRadius: Float,
        override val filled: Boolean,
    ) : PrototypeIconShape

    /** `<circle cx cy r/>`. */
    data class SvgCircle(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        override val filled: Boolean,
    ) : PrototypeIconShape
}

private fun strokePath(pathData: String): PrototypeIconShape =
    PrototypeIconShape.SvgPath(pathData, filled = false)

private fun fillPath(pathData: String): PrototypeIconShape =
    PrototypeIconShape.SvgPath(pathData, filled = true)

private fun strokeRect(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    rx: Float,
): PrototypeIconShape = PrototypeIconShape.SvgRect(x, y, width, height, rx, filled = false)

private fun strokeCircle(cx: Float, cy: Float, r: Float): PrototypeIconShape =
    PrototypeIconShape.SvgCircle(cx, cy, r, filled = false)

private fun fillCircle(cx: Float, cy: Float, r: Float): PrototypeIconShape =
    PrototypeIconShape.SvgCircle(cx, cy, r, filled = true)

/**
 * Geometry of every icon, transcribed 1:1 from `PATHS` in `vocabee-design/redesign/rd-base.js`.
 * The `when` is exhaustive on purpose: a new [PrototypeIcon] cannot be added without real
 * geometry, so no icon can silently render as an empty box.
 *
 * `RD.PATHS.phone` is intentionally not mapped — in the source it duplicates the `star` path.
 */
internal fun prototypeIconShapes(icon: PrototypeIcon): List<PrototypeIconShape> = when (icon) {
        PrototypeIcon.Book -> listOf(
            fillPath("M19 2L14 6.5V17.5L19 13V2M6.5 5C4.55 5 2.45 5.4 1 6.5V21.16" +
                "C1 21.41 1.25 21.66 1.5 21.66C1.6 21.66 1.65 21.59 1.75 21.59" +
                "C3.1 20.94 5.05 20.5 6.5 20.5C8.45 20.5 10.55 20.9 12 22" +
                "C13.35 21.15 15.8 20.5 17.5 20.5C19.15 20.5 20.85 20.81 22.25 21.56" +
                "C22.35 21.61 22.4 21.59 22.5 21.59C22.75 21.59 23 21.34 23 21.09V6.5" +
                "C22.4 6.05 21.75 5.75 21 5.5V19C19.9 18.65 18.7 18.5 17.5 18.5" +
                "C15.8 18.5 13.35 19.15 12 20V6.5C10.55 5.4 8.45 5 6.5 5Z"),
        )
        PrototypeIcon.BookTab -> listOf(
            fillPath("M6.5 20C8.2 20 10.65 20.65 12 21.5C13.35 20.65 15.8 20 17.5 20" +
                "C19.15 20 20.85 20.3 22.25 21.05C22.35 21.1 22.4 21.1 22.5 21.1" +
                "C22.75 21.1 23 20.85 23 20.6V6C22.4 5.55 21.75 5.25 21 5" +
                "C19.89 4.65 18.67 4.5 17.5 4.5C15.55 4.5 13.45 4.9 12 6" +
                "C10.55 4.9 8.45 4.5 6.5 4.5C5.33 4.5 4.11 4.65 3 5C2.25 5.25 1.6 5.55 1 6" +
                "V20.6C1 20.85 1.25 21.1 1.5 21.1C1.6 21.1 1.65 21.1 1.75 21.05" +
                "C3.15 20.3 4.85 20 6.5 20M12 19.5V8C13.35 7.15 15.8 6.5 17.5 6.5" +
                "C18.7 6.5 19.9 6.65 21 7V18.5C19.9 18.15 18.7 18 17.5 18" +
                "C15.8 18 13.35 18.65 12 19.5Z"),
        )
        PrototypeIcon.Cards -> listOf(
            strokeRect(3f, 6f, 13f, 15f, 2.5f),
            strokePath("M8 3h10a2.5 2.5 0 0 1 2.5 2.5V17"),
        )
        PrototypeIcon.User -> listOf(
            fillPath("M9,11.75A1.25,1.25 0 0,0 7.75,13A1.25,1.25 0 0,0 9,14.25" +
                "A1.25,1.25 0 0,0 10.25,13A1.25,1.25 0 0,0 9,11.75M15,11.75" +
                "A1.25,1.25 0 0,0 13.75,13A1.25,1.25 0 0,0 15,14.25" +
                "A1.25,1.25 0 0,0 16.25,13A1.25,1.25 0 0,0 15,11.75M12,2A10,10 0 0,0 2,12" +
                "A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,20" +
                "C7.59,20 4,16.41 4,12C4,11.71 4,11.42 4.05,11.14" +
                "C6.41,10.09 8.28,8.16 9.26,5.77C11.07,8.33 14.05,10 17.42,10" +
                "C18.2,10 18.95,9.91 19.67,9.74C19.88,10.45 20,11.21 20,12" +
                "C20,16.41 16.41,20 12,20Z"),
        )
        PrototypeIcon.UserF -> listOf(
            fillPath("M13.75 13C13.75 12.31 14.31 11.75 15 11.75" +
                "S16.25 12.31 16.25 13 15.69 14.25 15 14.25 13.75 13.69 13.75 13M22 12V22" +
                "H2V12C2 6.5 6.5 2 12 2S22 6.5 22 12M4 12C4 16.41 7.59 20 12 20" +
                "S20 16.41 20 12C20 11.21 19.88 10.45 19.67 9.74" +
                "C18.95 9.91 18.2 10 17.42 10C14.05 10 11.07 8.33 9.26 5.77" +
                "C8.28 8.16 6.41 10.09 4.05 11.14C4 11.42 4 11.71 4 12M9 14.25" +
                "C9.69 14.25 10.25 13.69 10.25 13" +
                "S9.69 11.75 9 11.75 7.75 12.31 7.75 13 8.31 14.25 9 14.25Z"),
        )
        PrototypeIcon.Plus -> listOf(strokePath("M12 5v14M5 12h14"))
        PrototypeIcon.Mic -> listOf(
            strokeRect(9f, 2.5f, 6f, 12f, 3f),
            strokePath("M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3"),
        )
        PrototypeIcon.Sound -> listOf(
            strokePath("M4 9v6h4l5 4V5L8 9H4Z"),
            strokePath("M17 9.5a3.5 3.5 0 0 1 0 5M19.5 7a7 7 0 0 1 0 10"),
        )
        PrototypeIcon.Sparkle -> listOf(
            strokePath("M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"),
            strokePath("M19 3.5l1.5 4.7M5 17l1.2 3.2"),
        )
        PrototypeIcon.Check -> listOf(strokePath("M5 12.5l4.5 4.5L19 7"))
        PrototypeIcon.ChevronRight -> listOf(strokePath("M9 5l7 7-7 7"))
        PrototypeIcon.ChevronLeft -> listOf(strokePath("M15 5l-7 7 7 7"))
        PrototypeIcon.ChevronDown -> listOf(strokePath("M5 9l7 7 7-7"))
        PrototypeIcon.Search -> listOf(
            strokeCircle(11f, 11f, 7f),
            strokePath("M20 20l-4-4"),
        )
        PrototypeIcon.Close -> listOf(strokePath("M6 6l12 12M18 6L6 18"))
        PrototypeIcon.Globe -> listOf(
            strokeCircle(12f, 12f, 9f),
            strokePath("M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"),
        )
        PrototypeIcon.Bell -> listOf(
            strokePath("M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z"),
            strokePath("M10 20a2 2 0 0 0 4 0"),
        )
        PrototypeIcon.Moon -> listOf(
            strokePath("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z"),
        )
        PrototypeIcon.Edit -> listOf(strokePath("M14 5l5 5M4 20l1-4L16 5l3 3L8 19l-4 1Z"))
        PrototypeIcon.Help -> listOf(
            strokeCircle(12f, 12f, 9f),
            strokePath("M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-.9.8-.9 1.5v.3"),
            fillCircle(12f, 17f, 0.7f),
        )
        PrototypeIcon.Invite -> listOf(
            strokeCircle(9f, 8f, 3.5f),
            strokePath("M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M18 8v6M15 11h6"),
        )
        PrototypeIcon.Flame -> listOf(
            strokePath("M12 3c1 3-1.5 4-1.5 6.5 0 1.4 1.1 2 1.5 2 .4 0 1.5-.6 1.5-2" +
                "C13.5 8 16 9 16 13a4 4 0 0 1-8 0c0-2 1-3 1-4 0 0-2.5.5-2.5 3.5" +
                "A6 6 0 0 0 18 13c0-6-6-6-6-10Z"),
        )
        PrototypeIcon.Bookmark -> listOf(strokePath("M6 4h12v16l-6-4-6 4V4Z"))
        PrototypeIcon.Star -> listOf(
            fillPath("M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24" +
                "L7.45,13.97L5.82,21L12,17.27Z"),
        )
        PrototypeIcon.ArrowRight -> listOf(strokePath("M5 12h14M13 6l6 6-6 6"))
        PrototypeIcon.Play -> listOf(
            strokeCircle(12f, 12f, 9f),
            fillPath("M10 8.5v7l6-3.5-6-3.5Z"),
        )
        PrototypeIcon.Trash -> listOf(
            strokePath("M4 7h16M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M6 7l1 13h10l1-13M10 11v6" +
                "M14 11v6"),
        )
        PrototypeIcon.Dumbbell -> listOf(
            fillPath("M12 5C10.89 5 10 5.89 10 7S10.89 9 12 9 14 8.11 14 7 13.11 5 12 5M22 1V6" +
                "H20V4H4V6H2V1H4V3H20V1H22M15 11.26V23H13V18H11V23H9V11.26" +
                "C6.93 10.17 5.5 8 5.5 5.5L5.5 5H7.5L7.5 5.5C7.5 8 9.5 10 12 10" +
                "S16.5 8 16.5 5.5L16.5 5H18.5L18.5 5.5C18.5 8 17.07 10.17 15 11.26Z"),
        )
        PrototypeIcon.Copy -> listOf(
            strokeRect(9f, 9f, 11f, 11f, 2.5f),
            strokePath("M15 9V6.5A2.5 2.5 0 0 0 12.5 4h-6A2.5 2.5 0 0 0 4 6.5v6" +
                "A2.5 2.5 0 0 0 6.5 15H9"),
        )
        PrototypeIcon.Share -> listOf(
            strokePath("M12 15V4M8 7.5 12 3.5l4 4"),
            strokePath("M5 12v7.5h14V12"),
        )
        PrototypeIcon.Clip -> listOf(
            strokePath("M20.5 12.5l-7.8 7.8a5.3 5.3 0 0 1-7.5-7.5l8.2-8.2a3.5 3.5 0 0 1 5 5" +
                "l-8.2 8.2a1.77 1.77 0 0 1-2.5-2.5l7.5-7.5"),
        )
        PrototypeIcon.Image -> listOf(
            strokeRect(3.5f, 5f, 17f, 14f, 2.5f),
            strokeCircle(9f, 10f, 1.6f),
            strokePath("M4.5 17.5 10 12l4 4 2.5-2.5 3 3"),
        )
        PrototypeIcon.Dots -> listOf(
            fillCircle(5.5f, 12f, 1.5f),
            fillCircle(12f, 12f, 1.5f),
            fillCircle(18.5f, 12f, 1.5f),
        )
        PrototypeIcon.Send -> listOf(
            strokePath("M22 2 11 13"),
            strokePath("M22 2 15 22l-4-9-9-4 20-7Z"),
        )
        PrototypeIcon.Plane -> listOf(
            fillPath("M20.56 3.91C21.15 4.5 21.15 5.45 20.56 6.03L16.67 9.92L18.79 19.11" +
                "L17.38 20.53L13.5 13.1L9.6 17L9.96 19.47L8.89 20.53L7.13 17.35L3.94 15.58" +
                "L5 14.5L7.5 14.87L11.37 11L3.94 7.09L5.36 5.68L14.55 7.8L18.44 3.91" +
                "C19 3.33 20 3.33 20.56 3.91Z"),
        )
        PrototypeIcon.Car -> listOf(
            fillPath("M16,6L19,10H21C22.11,10 23,10.89 23,12V15H21A3,3 0 0,1 18,18" +
                "A3,3 0 0,1 15,15H9A3,3 0 0,1 6,18A3,3 0 0,1 3,15H1V12" +
                "C1,10.89 1.89,10 3,10L6,6H16M10.5,7.5H6.75L4.86,10H10.5V7.5M12,7.5V10" +
                "H17.14L15.25,7.5H12M6,13.5A1.5,1.5 0 0,0 4.5,15A1.5,1.5 0 0,0 6,16.5" +
                "A1.5,1.5 0 0,0 7.5,15A1.5,1.5 0 0,0 6,13.5M18,13.5A1.5,1.5 0 0,0 16.5,15" +
                "A1.5,1.5 0 0,0 18,16.5A1.5,1.5 0 0,0 19.5,15A1.5,1.5 0 0,0 18,13.5Z"),
        )
        PrototypeIcon.Film -> listOf(
            fillPath("M18,14.5V11A1,1 0 0,0 17,10H16C18.24,8.39 18.76,5.27 17.15,3" +
                "C15.54,0.78 12.42,0.26 10.17,1.87C9.5,2.35 8.96,3 8.6,3.73" +
                "C6.25,2.28 3.17,3 1.72,5.37C0.28,7.72 1,10.8 3.36,12.25" +
                "C3.57,12.37 3.78,12.5 4,12.58V21A1,1 0 0,0 5,22H17A1,1 0 0,0 18,21V17.5" +
                "L22,21.5V10.5L18,14.5M13,4A2,2 0 0,1 15,6A2,2 0 0,1 13,8A2,2 0 0,1 11,6" +
                "A2,2 0 0,1 13,4M6,6A2,2 0 0,1 8,8A2,2 0 0,1 6,10A2,2 0 0,1 4,8" +
                "A2,2 0 0,1 6,6Z"),
        )
        PrototypeIcon.Brief -> listOf(
            fillPath("M10,2H14A2,2 0 0,1 16,4V6H20A2,2 0 0,1 22,8V19A2,2 0 0,1 20,21H4" +
                "C2.89,21 2,20.1 2,19V8C2,6.89 2.89,6 4,6H8V4C8,2.89 8.89,2 10,2M14,6V4H10" +
                "V6H14Z"),
        )
        PrototypeIcon.Grad -> listOf(
            fillPath("M12,3L1,9L12,15L21,10.09V17H23V9M5,13.18V17.18L12,21L19,17.18V13.18L12,17" +
                "L5,13.18Z"),
        )
        PrototypeIcon.Food -> listOf(
            fillPath("M11,9H9V2H7V9H5V2H3V9C3,11.12 4.66,12.84 6.75,12.97V22H9.25V12.97" +
                "C11.34,12.84 13,11.12 13,9V2H11V9M16,6V14H18.5V22H21V2" +
                "C18.24,2 16,4.24 16,6Z"),
        )
        PrototypeIcon.Ball -> listOf(
            fillPath("M16.93 17.12L16.13 15.76L17.59 11.39L19 10.92L20 11.67" +
                "C20 11.7 20 11.75 20 11.81C20 11.88 20.03 11.94 20.03 12" +
                "C20.03 13.97 19.37 15.71 18.06 17.21L16.93 17.12M9.75 15L8.38 10.97" +
                "L12 8.43L15.62 10.97L14.25 15H9.75M12 20.03" +
                "C11.12 20.03 10.29 19.89 9.5 19.61L8.81 18.1L9.47 17H14.58L15.19 18.1" +
                "L14.5 19.61C13.71 19.89 12.88 20.03 12 20.03M5.94 17.21" +
                "C5.41 16.59 4.95 15.76 4.56 14.75C4.17 13.73 3.97 12.81 3.97 12" +
                "C3.97 11.94 4 11.88 4 11.81C4 11.75 4 11.7 4 11.67L5 10.92L6.41 11.39" +
                "L7.87 15.76L7.07 17.12L5.94 17.21M11 5.29V6.69L7 9.46L5.66 9.04L5.24 7.68" +
                "C5.68 7 6.33 6.32 7.19 5.66S8.87 4.57 9.65 4.35L11 5.29M14.35 4.35" +
                "C15.13 4.57 15.95 5 16.81 5.66C17.67 6.32 18.32 7 18.76 7.68L18.34 9.04" +
                "L17 9.47L13 6.7V5.29L14.35 4.35M4.93 4.93C3 6.89 2 9.25 2 12" +
                "S3 17.11 4.93 19.07 9.25 22 12 22 17.11 21 19.07 19.07 22 14.75 22 12 21 " +
                "6.89 19.07 4.93 14.75 2 12 2 6.89 3 4.93 4.93Z"),
        )
        PrototypeIcon.Music -> listOf(
            fillPath("M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5" +
                "A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5" +
                "A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14" +
                "C6.04,14 6.55,14.12 7,14.34V6L21,3Z"),
        )
        PrototypeIcon.Leaf -> listOf(
            fillPath("M10,21V18H3L8,13H5L10,8H7L12,3L17,8H14L19,13H16L21,18H14V21H10Z"),
        )
        PrototypeIcon.Laptop -> listOf(
            fillPath("M3 6H21V4H3C1.9 4 1 4.9 1 6V18C1 19.1 1.9 20 3 20H7V18H3V6M13 12H9V13.78" +
                "C8.39 14.33 8 15.11 8 16C8 16.89 8.39 17.67 9 18.22V20H13V18.22" +
                "C13.61 17.67 14 16.88 14 16S13.61 14.33 13 13.78V12M11 17.5" +
                "C10.17 17.5 9.5 16.83 9.5 16" +
                "S10.17 14.5 11 14.5 12.5 15.17 12.5 16 11.83 17.5 11 17.5M22 8H16" +
                "C15.5 8 15 8.5 15 9V19C15 19.5 15.5 20 16 20H22C22.5 20 23 19.5 23 19V9" +
                "C23 8.5 22.5 8 22 8M21 18H17V10H21V18Z"),
        )
        PrototypeIcon.Bag -> listOf(
            fillPath("M17,18C15.89,18 15,18.89 15,20A2,2 0 0,0 17,22A2,2 0 0,0 19,20" +
                "C19,18.89 18.1,18 17,18M1,2V4H3L6.6,11.59L5.24,14.04" +
                "C5.09,14.32 5,14.65 5,15A2,2 0 0,0 7,17H19V15H7.42" +
                "A0.25,0.25 0 0,1 7.17,14.75C7.17,14.7 7.18,14.66 7.2,14.63L8.1,13H15.55" +
                "C16.3,13 16.96,12.58 17.3,11.97L20.88,5.5C20.95,5.34 21,5.17 21,5" +
                "A1,1 0 0,0 20,4H5.21L4.27,2M7,18C5.89,18 5,18.89 5,20A2,2 0 0,0 7,22" +
                "A2,2 0 0,0 9,20C9,18.89 8.1,18 7,18Z"),
        )
        PrototypeIcon.Heart -> listOf(
            fillPath("M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3" +
                "C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3" +
                "C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"),
        )
        PrototypeIcon.Child -> listOf(
            fillPath("M12.5 11.5C13.3 11.5 14 10.8 14 10" +
                "S13.3 8.5 12.5 8.5 11 9.2 11 10 11.7 11.5 12.5 11.5M5.5 6" +
                "C6.6 6 7.5 5.1 7.5 4S6.6 2 5.5 2 3.5 2.9 3.5 4 4.4 6 5.5 6M7.5 22V15H9V9" +
                "C9 7.9 8.1 7 7 7H4C2.9 7 2 7.9 2 9V15H3.5V22H7.5M14 22V18H15V14" +
                "C15 13.2 14.3 12.5 13.5 12.5H11.5C10.7 12.5 10 13.2 10 14V18H11V22H14" +
                "M18.5 6C19.6 6 20.5 5.1 20.5 4" +
                "S19.6 2 18.5 2 16.5 2.9 16.5 4 17.4 6 18.5 6M22 9V15H20.5V22H17V14" +
                "C17 12.6 16.2 11.4 15 10.9V9C15 7.9 15.9 7 17 7H20C21.1 7 22 7.9 22 9Z"),
        )
        // УВАГА: у джерелі дизайну (rd-base.js) під ключем `chat` лежить шлях
        // ПІСТОЛЕТА — копі-пейст-баг того ж роду, що й `phone`, який дублює `star`.
        // Для теми «спілкування» малюємо мовну бульбашку; решта геометрії — з дизайну.
        PrototypeIcon.Chat -> listOf(
            fillPath("M12,3C17.5,3 22,6.58 22,11C22,15.42 17.5,19 12,19" +
                "C10.76,19 9.57,18.82 8.47,18.5C5.55,21 2,21 2,21" +
                "C4.33,18.67 4.7,17.1 4.75,16.5C3.05,15.07 2,13.13 2,11" +
                "C2,6.58 6.5,3 12,3Z"),
        )
        PrototypeIcon.Burger -> listOf(
            fillPath("M18.06 23H19.72C20.56 23 21.25 22.35 21.35 21.53L23 5.05H18V1H16.03V5.05" +
                "H11.06L11.36 7.39C13.07 7.86 14.67 8.71 15.63 9.65" +
                "C17.07 11.07 18.06 12.54 18.06 14.94V23M1 22V21H16.03V22" +
                "C16.03 22.54 15.58 23 15 23H2C1.45 23 1 22.54 1 22M16.03 15" +
                "C16.03 7 1 7 1 15H16.03M1 17H16V19H1V17Z"),
        )
        PrototypeIcon.Drink -> listOf(
            fillPath("M9.5 3C7.56 3 5.85 4.24 5.23 6.08C3.36 6.44 2 8.09 2 10" +
                "C2 12.21 3.79 14 6 14V22H17V20H20C20.55 20 21 19.55 21 19V11" +
                "C21 10.45 20.55 10 20 10H18V8C18 5.79 16.21 4 14 4H12.32" +
                "C11.5 3.35 10.53 3 9.5 3M9.5 5C10.29 5 11.03 5.37 11.5 6H14" +
                "C15.11 6 16 6.9 16 8H12C10 8 9.32 9.13 8.5 10.63C7.68 12.13 6 12 6 12" +
                "C4.89 12 4 11.11 4 10C4 8.9 4.89 8 6 8H7V7.5C7 6.12 8.12 5 9.5 5M17 12H19" +
                "V18H17Z"),
        )
        PrototypeIcon.Cat -> listOf(
            fillPath("M12,8L10.67,8.09C9.81,7.07 7.4,4.5 5,4.5C5,4.5 3.03,7.46 4.96,11.41" +
                "C4.41,12.24 4.07,12.67 4,13.66L2.07,13.95L2.28,14.93L4.04,14.67" +
                "L4.18,15.38L2.61,16.32L3.08,17.21L4.53,16.32C5.68,18.76 8.59,20 12,20" +
                "C15.41,20 18.32,18.76 19.47,16.32L20.92,17.21L21.39,16.32L19.82,15.38" +
                "L19.96,14.67L21.72,14.93L21.93,13.95L20,13.66" +
                "C19.93,12.67 19.59,12.24 19.04,11.41C20.97,7.46 19,4.5 19,4.5" +
                "C16.6,4.5 14.19,7.07 13.33,8.09L12,8M9,11A1,1 0 0,1 10,12A1,1 0 0,1 9,13" +
                "A1,1 0 0,1 8,12A1,1 0 0,1 9,11M15,11A1,1 0 0,1 16,12A1,1 0 0,1 15,13" +
                "A1,1 0 0,1 14,12A1,1 0 0,1 15,11M11,14H13L12.3,15.39" +
                "C12.5,16.03 13.06,16.5 13.75,16.5A1.5,1.5 0 0,0 15.25,15H15.75" +
                "A2,2 0 0,1 13.75,17C13,17 12.35,16.59 12,16V16H12" +
                "C11.65,16.59 11,17 10.25,17A2,2 0 0,1 8.25,15H8.75" +
                "A1.5,1.5 0 0,0 10.25,16.5C10.94,16.5 11.5,16.03 11.7,15.39L11,14Z"),
        )
        PrototypeIcon.Dog -> listOf(
            fillPath("M18,4C16.29,4 15.25,4.33 14.65,4.61C13.88,4.23 13,4 12,4" +
                "C11,4 10.12,4.23 9.35,4.61C8.75,4.33 7.71,4 6,4C3,4 1,12 1,14" +
                "C1,14.83 2.32,15.59 4.14,15.9C4.78,18.14 7.8,19.85 11.5,20V15.72" +
                "C10.91,15.35 10,14.68 10,14C10,13 12,13 12,13C12,13 14,13 14,14" +
                "C14,14.68 13.09,15.35 12.5,15.72V20C16.2,19.85 19.22,18.14 19.86,15.9" +
                "C21.68,15.59 23,14.83 23,14C23,12 21,4 18,4M4.15,13.87" +
                "C3.65,13.75 3.26,13.61 3,13.5C3.25,10.73 5.2,6.4 6.05,6" +
                "C6.59,6 7,6.06 7.37,6.11C5.27,8.42 4.44,12.04 4.15,13.87M9,12" +
                "A1,1 0 0,1 8,11C8,10.46 8.45,10 9,10A1,1 0 0,1 10,11" +
                "C10,11.56 9.55,12 9,12M15,12A1,1 0 0,1 14,11C14,10.46 14.45,10 15,10" +
                "A1,1 0 0,1 16,11C16,11.56 15.55,12 15,12M19.85,13.87" +
                "C19.56,12.04 18.73,8.42 16.63,6.11C17,6.06 17.41,6 17.95,6" +
                "C18.8,6.4 20.75,10.73 21,13.5C20.75,13.61 20.36,13.75 19.85,13.87Z"),
        )
}

/** Builds the outline of one icon element, still in the design's 24×24 viewBox coordinates. */
private fun PrototypeIconShape.toPath(): Path = when (this) {
    is PrototypeIconShape.SvgPath -> PathParser().parsePathString(pathData).toPath()
    is PrototypeIconShape.SvgRect -> Path().apply {
        addRoundRect(
            RoundRect(
                left = x,
                top = y,
                right = x + width,
                bottom = y + height,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            ),
        )
    }
    is PrototypeIconShape.SvgCircle -> Path().apply {
        addOval(Rect(center = Offset(cx, cy), radius = radius))
    }
}

@Composable
internal fun PrototypeLineIcon(
    icon: PrototypeIcon,
    modifier: Modifier = Modifier,
    color: Color = PrototypeColor.Ink,
    strokeWidth: Float = 1.9f,
) {
    // Outlines keep the design's 24×24 coordinates and are parsed once per icon; the canvas is
    // scaled instead of the paths, so resizing never re-parses and [strokeWidth] keeps its SVG
    // meaning — it scales with the viewBox exactly like `stroke-width` inside RD.ic.
    val shapes = remember(icon) { prototypeIconShapes(icon).map { it.toPath() to it.filled } }
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        translate(left = (size.width - 24f * unit) / 2f, top = (size.height - 24f * unit) / 2f) {
            scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero) {
                shapes.forEach { (path, filled) ->
                    drawPath(path = path, color = color, style = if (filled) Fill else stroke)
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

/**
 * Мови, які застосунок ПОКАЗУЄ в пікерах (онбординг, «Я вивчаю», вибір мови словника).
 *
 * Це вітрина, а не список підтримуваних мов: `VocabularyRepository.supportedLanguages` навмисно
 * лишається ширшим, щоб уже створені словники з прихованими мовами далі резолвились у свій
 * `LanguageOption` (локаль для STT/TTS). Код, якого тут нема, деградує через [prototypeLanguage]
 * до нейтрального прапора замість падіння.
 */
internal val PrototypeLanguages: List<PrototypeLanguage> = listOf(
    PrototypeLanguage("uk", "Українська", "🇺🇦"),
    PrototypeLanguage("en", "Англійська", "🇬🇧"),
    PrototypeLanguage("de", "Німецька", "🇩🇪"),
    PrototypeLanguage("es", "Іспанська", "🇪🇸"),
    PrototypeLanguage("fr", "Французька", "🇫🇷"),
    PrototypeLanguage("pl", "Польська", "🇵🇱"),
    PrototypeLanguage("it", "Італійська", "🇮🇹"),
    PrototypeLanguage("lt", "Литовська", "🇱🇹"),
)

/**
 * Дескриптор мови за кодом. Для коду поза [PrototypeLanguages] (напр. словник, створений до
 * скорочення списку) повертає безпечний плейсхолдер — код замість назви й нейтральний прапор,
 * тож жоден екран не падає.
 */
internal fun prototypeLanguage(code: String): PrototypeLanguage {
    return PrototypeLanguages.firstOrNull { it.code == code }
        ?: PrototypeLanguage(code, code, "🏳️")
}

internal fun languageFlag(code: String): String = prototypeLanguage(code).flag

internal fun languageName(code: String): String = prototypeLanguage(code).name
