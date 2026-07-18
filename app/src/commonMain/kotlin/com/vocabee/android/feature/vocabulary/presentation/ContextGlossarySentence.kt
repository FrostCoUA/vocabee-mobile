package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossary
import com.vocabee.android.feature.vocabulary.domain.model.ContextGlossaryToken
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Скільки попап живе без взаємодії (борд 13). */
internal const val ContextPopupAutoDismissMillis = 2_400L

/** Стеля ширини текстової колонки попапа — щоб довгий переклад не роздував його на весь екран. */
internal val ContextPopupTextMaxWidth = 190.dp

/** Горизонтальний відступ підсвітки токена від глифів (борд 13: `padding: 0 4px`). */
internal val ContextTokenHighlightPadding = 4.dp

/**
 * Наскільки розширюються пробіли обабіч підсвіченого слова, щоб плашка не
 * торкалася сусідніх слів. Значення РІЗНІ: при однакових щілина справа виглядає
 * помітно більшою (letter-spacing розподіляється навколо гліфів разом із їхніми
 * side bearings), тож праве підібране на око до візуальної симетрії.
 */
internal val ContextTokenHighlightGapStart = 6.sp
internal val ContextTokenHighlightGapEnd = 2.sp

/** Скруглення підсвітки токена. */
internal val ContextTokenHighlightRadius = 9.dp

/** Розміри «носика» попапа, що вказує на слово. */
internal val ContextPopupCaretWidth = 14.dp
internal val ContextPopupCaretHeight = 7.dp

/**
 * Повний бюджет ширини попапа: padding 13 + текст + 10 + кнопка 34 + padding 8.
 * Попап МУСИТЬ лишатися помітно вужчим за екран, інакше центрування над словом
 * математично неможливе й провайдер притисне його до краю.
 */
internal val ContextPopupMaxWidth = 13.dp + ContextPopupTextMaxWidth + 10.dp + 34.dp + 8.dp

/** Мінімальний відступ попапа від країв екрана і зазор до токена. */
private val ContextPopupScreenMargin = 8.dp
private val ContextPopupTokenGap = 9.dp

/**
 * Релейаут (зміна стилю виділеного токена) не має гасити відкритий попап:
 * нульові/порожні bounds ігноруються на користь попередніх.
 */
internal fun contextPopupBoundsAfterRelayout(
    previous: IntRect?,
    recomputed: IntRect?,
): IntRect? = recomputed?.takeIf { it.width > 0 && it.height > 0 } ?: previous

@Composable
internal fun ContextGlossarySentence(
    glossary: ContextGlossary,
    targetWord: String,
    savedKeys: Set<String>,
    action: ContextGlossaryTokenAction,
    onAction: (ContextGlossaryToken) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = 3,
    textAlign: TextAlign = TextAlign.Center,
) {
    val density = LocalDensity.current
    val popupMarginPx = with(density) { ContextPopupScreenMargin.roundToPx() }
    val popupGapPx = with(density) { ContextPopupTokenGap.roundToPx() }
    var textLayout by remember(glossary) { mutableStateOf<TextLayoutResult?>(null) }
    var selectedIndex by remember(glossary) { mutableStateOf<Int?>(null) }
    var selectedBounds by remember(glossary) { mutableStateOf<IntRect?>(null) }
    val selectedToken = selectedIndex?.let(glossary.tokens::getOrNull)
    val targetTokenIndexes = remember(glossary, targetWord) {
        contextTargetTokenIndexes(glossary, targetWord)
    }
    val savedTokenIndexes = remember(glossary, savedKeys) {
        contextSavedTokenIndexes(glossary, savedKeys)
    }
    val displayedSentence = remember(glossary, targetTokenIndexes, savedTokenIndexes, selectedIndex) {
        contextSentenceAnnotated(
            sentence = glossary.sentence,
            tokens = glossary.tokens,
            targetTokenIndexes = targetTokenIndexes,
            savedTokenIndexes = savedTokenIndexes,
            selectedIndex = selectedIndex,
        )
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            delay(ContextPopupAutoDismissMillis)
            selectedIndex = null
            selectedBounds = null
        }
    }

    val highlightPaddingPx = with(density) { ContextTokenHighlightPadding.toPx() }
    val highlightRadiusPx = with(density) { ContextTokenHighlightRadius.toPx() }
    // Куди показує носик попапа: центр токена відносно лівого краю попапа.
    var caretCenterPx by remember(glossary) { mutableStateOf<Int?>(null) }
    var caretPointsDown by remember(glossary) { mutableStateOf(true) }
    var bubbleWidthPx by remember(glossary) { mutableStateOf(0) }

    Box(modifier = modifier) {
        Text(
            text = displayedSentence,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val layout = textLayout ?: return@drawBehind
                    glossary.tokens.forEachIndexed { index, token ->
                        val color = contextTokenHighlightColor(
                            contextTokenVisual(
                                index = index,
                                targetTokenIndexes = targetTokenIndexes,
                                savedTokenIndexes = savedTokenIndexes,
                                selectedIndex = selectedIndex,
                            ),
                        ) ?: return@forEachIndexed
                        layout.tokenLineRects(token).forEach { rect ->
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(rect.left - highlightPaddingPx, rect.top),
                                size = Size(
                                    width = rect.width + highlightPaddingPx * 2,
                                    height = rect.height,
                                ),
                                cornerRadius = CornerRadius(highlightRadiusPx, highlightRadiusPx),
                            )
                        }
                    }
                }
                .pointerInput(glossary, targetTokenIndexes) {
                detectTapGestures { position ->
                    val layout = textLayout ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    // Цільове слово лишається лише хайлайтом — тап по ньому просто закриває попап.
                    val candidateIndex = contextSelectableTokenIndexAtOffset(
                        glossary = glossary,
                        offset = offset,
                        targetTokenIndexes = targetTokenIndexes,
                    )
                    val candidateBounds = candidateIndex
                        ?.let(glossary.tokens::getOrNull)
                        ?.let(layout::tokenBounds)
                    val tokenIndex = candidateIndex.takeIf {
                        candidateBounds != null &&
                            position.x >= candidateBounds.left && position.x <= candidateBounds.right &&
                            position.y >= candidateBounds.top && position.y <= candidateBounds.bottom
                    }
                    selectedIndex = tokenIndex
                    selectedBounds = candidateBounds.takeIf { tokenIndex != null }
                }
            },
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                textLayout = layout
                // Виділення робить токен жирним → Text перевимірюється і сюди
                // прилітає новий layout. Порожні/нульові bounds НЕ мають гасити
                // вже відкритий попап, тому лишаємо попередні.
                selectedBounds = if (selectedToken == null) {
                    null
                } else {
                    contextPopupBoundsAfterRelayout(
                        previous = selectedBounds,
                        recomputed = layout.tokenBounds(selectedToken),
                    )
                }
            },
        )

        if (selectedToken != null && selectedBounds != null) {
            Popup(
                popupPositionProvider = AboveTokenPopupPositionProvider(
                    tokenBounds = selectedBounds!!,
                    horizontalMarginPx = popupMarginPx,
                    gapPx = popupGapPx,
                    onPlaced = { popupX, tokenCenterX, above ->
                        val caret = tokenCenterX - popupX
                        if (caretCenterPx != caret) caretCenterPx = caret
                        if (caretPointsDown != above) caretPointsDown = above
                    },
                ),
                onDismissRequest = {
                    selectedIndex = null
                    selectedBounds = null
                },
                // focusable = false: фокусований попап гасне, щойно вікно застосунку
                // забирає фокус назад — попап зникав раніше за власний таймер.
                properties = PopupProperties(focusable = false),
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    if (!caretPointsDown) {
                        ContextPopupCaret(
                            pointsDown = false,
                            centerPx = caretCenterPx,
                            bubbleWidthPx = bubbleWidthPx,
                        )
                    }
                Surface(
                    modifier = Modifier.onSizeChanged { bubbleWidthPx = it.width },
                    shape = RoundedCornerShape(12.dp),
                    color = PrototypeColor.White,
                    border = BorderStroke(1.dp, PrototypeColor.PurpleText.copy(alpha = 0.22f)),
                    shadowElevation = 10.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 13.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // БЕЗ weight(1f): Popup вимірює вміст по МЕЖАХ ЕКРАНА, тож
                        // зважений нащадок роздувався на всю ширину вікна, попап ставав
                        // 1080px і провайдер завжди клампив його до лівого краю.
                        // Ширину обмежуємо явно, а довгий переклад ріжемо ellipsis.
                        Column(modifier = Modifier.widthIn(max = ContextPopupTextMaxWidth)) {
                            Text(
                                text = selectedToken.surface,
                                color = PrototypeColor.Muted2,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = selectedToken.translation,
                                color = PrototypeColor.Ink,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val isSaved = contextBookmarkKey(glossary, selectedToken) in savedKeys
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSaved) PrototypeColor.Yellow.copy(alpha = 0.34f)
                                    else PrototypeColor.Tint,
                                )
                                .clickable(
                                    enabled = contextPopupActionEnabled(action, isSaved),
                                ) {
                                    onAction(selectedToken)
                                    selectedIndex = null
                                    selectedBounds = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            PrototypeLineIcon(
                                icon = contextPopupActionIcon(action, isSaved),
                                modifier = Modifier.size(17.dp),
                                color = if (isSaved) {
                                    PrototypeColor.NoteYellowText
                                } else {
                                    PrototypeColor.PurpleText
                                },
                                strokeWidth = 2.2f,
                            )
                        }
                    }
                }
                    if (caretPointsDown) {
                        ContextPopupCaret(
                            pointsDown = true,
                            centerPx = caretCenterPx,
                            bubbleWidthPx = bubbleWidthPx,
                        )
                    }
                }
            }
        }
    }
}

/**
 * «Носик» попапа — трикутник, що вказує на слово. Горизонтально стоїть під
 * центром токена; біля країв екрана попап зсунутий, тож носик клампиться, щоб
 * не з'їхати за скруглення бульбашки.
 */
@Composable
private fun ContextPopupCaret(
    pointsDown: Boolean,
    centerPx: Int?,
    bubbleWidthPx: Int,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { ContextPopupCaretWidth.toPx() }
    val edgeInsetPx = with(density) { 14.dp.toPx() }
    val fallbackCenter = bubbleWidthPx / 2f
    val center = centerPx?.toFloat() ?: fallbackCenter
    val minCenter = edgeInsetPx + widthPx / 2f
    val maxCenter = (bubbleWidthPx - edgeInsetPx - widthPx / 2f).coerceAtLeast(minCenter)
    val clamped = center.coerceIn(minCenter, maxCenter)
    val offsetPx = (clamped - widthPx / 2f).roundToInt()

    Canvas(
        modifier = Modifier
            .offset { IntOffset(offsetPx, 0) }
            .size(width = ContextPopupCaretWidth, height = ContextPopupCaretHeight),
    ) {
        val path = Path().apply {
            if (pointsDown) {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            } else {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            }
            close()
        }
        drawPath(path = path, color = PrototypeColor.White)
    }
}

internal enum class ContextGlossaryTokenAction {
    Bookmark,
    AddToDictionary,
}

/**
 * Стан токена речення. Підкреслень немає взагалі — стан читається лише із заливки
 * (борд 13: «Слова без підкреслень: звичайний текст тапабельний»).
 */
internal enum class ContextTokenVisual {
    /** Цільове слово картки: жовтий хайлайт, не інтерактивне. */
    Target,

    /** Слово вже в закладках / у словнику: жовта заливка 30%. */
    Saved,

    /** Відкритий попап над словом: заливка tint. */
    Selected,

    /** Звичайне слово: без оформлення, але тапабельне. */
    Plain,
}

internal fun contextTokenVisual(
    index: Int,
    targetTokenIndexes: Set<Int>,
    savedTokenIndexes: Set<Int>,
    selectedIndex: Int?,
): ContextTokenVisual = when {
    index in targetTokenIndexes -> ContextTokenVisual.Target
    // Збережений стан важливіший за виділення: попап над збереженим словом
    // лишає жовту заливку (борд 13, фрейм 3).
    index in savedTokenIndexes -> ContextTokenVisual.Saved
    index == selectedIndex -> ContextTokenVisual.Selected
    else -> ContextTokenVisual.Plain
}

/**
 * Лише колір і накреслення тексту. Заливка НЕ йде через [SpanStyle.background]:
 * той малює прямокутник впритул до глифів, а борд 13 вимагає підсвітку з
 * горизонтальним відступом і скругленням — її малює [contextTokenHighlightColor]
 * у drawBehind.
 */
internal fun contextTokenSpanStyle(visual: ContextTokenVisual): SpanStyle? = when (visual) {
    ContextTokenVisual.Target -> SpanStyle(
        color = PrototypeColor.YellowText,
        fontWeight = FontWeight.ExtraBold,
    )
    ContextTokenVisual.Saved -> SpanStyle(
        color = PrototypeColor.NoteYellowText,
        fontWeight = FontWeight.ExtraBold,
    )
    ContextTokenVisual.Selected -> SpanStyle(
        color = PrototypeColor.PurpleText,
        fontWeight = FontWeight.ExtraBold,
    )
    ContextTokenVisual.Plain -> null
}

/**
 * Заливку має ЛИШЕ цільове слово раунду — воно і є питанням картки.
 * Слова, по яких тапають (вибране, збережене), відрізняються самим кольором
 * тексту: інакше в реченні світиться кілька плашок і губиться акцент.
 */
internal fun contextTokenHighlightColor(visual: ContextTokenVisual): Color? = when (visual) {
    ContextTokenVisual.Target -> PrototypeColor.Yellow
    ContextTokenVisual.Saved, ContextTokenVisual.Selected, ContextTokenVisual.Plain -> null
}

internal fun contextPopupActionIcon(
    action: ContextGlossaryTokenAction,
    isSaved: Boolean,
): PrototypeIcon = when {
    isSaved -> PrototypeIcon.Check
    action == ContextGlossaryTokenAction.AddToDictionary -> PrototypeIcon.Plus
    else -> PrototypeIcon.Bookmark
}

/**
 * У тренуванні повторний тап знімає закладку, у словнику збережене слово
 * повторно не додається.
 */
internal fun contextPopupActionEnabled(
    action: ContextGlossaryTokenAction,
    isSaved: Boolean,
): Boolean = action == ContextGlossaryTokenAction.Bookmark || !isSaved

internal fun contextTokenIndexAtOffset(glossary: ContextGlossary, offset: Int): Int? =
    glossary.tokens.indexOfFirst { token -> offset in token.start until token.endExclusive }
        .takeIf { it >= 0 }

internal fun contextSelectableTokenIndexAtOffset(
    glossary: ContextGlossary,
    offset: Int,
    targetTokenIndexes: Set<Int>,
): Int? = contextTokenIndexAtOffset(glossary, offset)?.takeUnless(targetTokenIndexes::contains)

internal fun contextSavedTokenIndexes(
    glossary: ContextGlossary,
    savedKeys: Set<String>,
): Set<Int> {
    if (savedKeys.isEmpty()) return emptySet()
    return glossary.tokens.indices.filterTo(mutableSetOf()) { index ->
        contextBookmarkKey(glossary, glossary.tokens[index]) in savedKeys
    }
}

internal fun contextBookmarkKey(
    glossary: ContextGlossary,
    token: ContextGlossaryToken,
): String = contextTranslationKey(
    sourceLang = glossary.sourceLang,
    targetLang = glossary.targetLang,
    source = token.lemma?.trim()?.takeIf { it.isNotEmpty() } ?: token.normalized,
    translation = token.translation,
)

internal fun contextTranslationKey(
    sourceLang: String,
    targetLang: String,
    source: String,
    translation: String,
): String = listOf(
    sourceLang.trim().lowercase(),
    targetLang.trim().lowercase(),
    source.trim().lowercase(),
    translation.trim().lowercase(),
).joinToString(":")

internal fun contextTargetTokenIndexes(
    glossary: ContextGlossary,
    targetWord: String,
): Set<Int> {
    val normalizedTarget = targetWord.trim().lowercase()
    if (normalizedTarget.isBlank()) return emptySet()
    if (normalizedTarget.any(Char::isWhitespace)) {
        val targetParts = normalizedTarget.split(Regex("\\s+")).filter(String::isNotBlank)
        val matches = mutableSetOf<Int>()
        for (start in 0..(glossary.tokens.size - targetParts.size).coerceAtLeast(-1)) {
            val matchesPhrase = targetParts.indices.all { partIndex ->
                val token = glossary.tokens[start + partIndex]
                token.normalized == targetParts[partIndex] ||
                    token.lemma?.trim()?.lowercase() == targetParts[partIndex]
            }
            if (matchesPhrase) {
                targetParts.indices.forEach { partIndex -> matches += start + partIndex }
            }
        }
        return matches
    }
    return glossary.tokens.indices.filterTo(mutableSetOf()) { index ->
        val token = glossary.tokens[index]
        val lemma = token.lemma?.trim()?.lowercase()
        lemma == normalizedTarget || token.normalized == normalizedTarget ||
            (lemma == null && normalizedTarget.length >= 3 && token.normalized.startsWith(normalizedTarget))
    }
}

internal fun contextSentenceAnnotated(
    sentence: String,
    tokens: List<ContextGlossaryToken>,
    targetTokenIndexes: Set<Int>,
    savedTokenIndexes: Set<Int>,
    selectedIndex: Int?,
): AnnotatedString = buildAnnotatedString {
    append(sentence)
    tokens.forEachIndexed { index, token ->
        if (token.start !in sentence.indices || token.endExclusive > sentence.length) {
            return@forEachIndexed
        }
        val visual = contextTokenVisual(
            index = index,
            targetTokenIndexes = targetTokenIndexes,
            savedTokenIndexes = savedTokenIndexes,
            selectedIndex = selectedIndex,
        )
        contextTokenSpanStyle(visual)?.let { style ->
            addStyle(style = style, start = token.start, end = token.endExclusive)
        }

        // Підсвітка малюється ширшою за глифи, тож без цього плашка налізає на
        // сусідні слова. Розширюємо САМЕ сусідні пробіли (letterSpacing), а не
        // вставляємо символи: індекси токенів приходять з бекенда й зсув їх зламав би.
        if (contextTokenHighlightColor(visual) == null) return@forEachIndexed
        if (sentence.getOrNull(token.start - 1)?.isWhitespace() == true) {
            addStyle(
                style = SpanStyle(letterSpacing = ContextTokenHighlightGapStart),
                start = token.start - 1,
                end = token.start,
            )
        }
        if (sentence.getOrNull(token.endExclusive)?.isWhitespace() == true) {
            addStyle(
                style = SpanStyle(letterSpacing = ContextTokenHighlightGapEnd),
                start = token.endExclusive,
                end = token.endExclusive + 1,
            )
        }
    }
}

/**
 * Прямокутники токена по рядках: перенесене слово дає окремий бокс на кожен
 * рядок, тож підсвітка не малює величезну пляму через два рядки.
 */
private fun TextLayoutResult.tokenLineRects(token: ContextGlossaryToken): List<Rect> {
    if (token.start !in layoutInput.text.indices || token.endExclusive > layoutInput.text.length) {
        return emptyList()
    }
    val rects = mutableListOf<Rect>()
    var current: Rect? = null
    for (offset in token.start until token.endExclusive) {
        val box = getBoundingBox(offset)
        val open = current
        current = if (open != null && open.top == box.top) {
            Rect(
                left = min(open.left, box.left),
                top = open.top,
                right = max(open.right, box.right),
                bottom = max(open.bottom, box.bottom),
            )
        } else {
            open?.let(rects::add)
            box
        }
    }
    current?.let(rects::add)
    return rects
}

private fun TextLayoutResult.tokenBounds(token: ContextGlossaryToken): IntRect? {
    if (token.start !in layoutInput.text.indices || token.endExclusive > layoutInput.text.length) {
        return null
    }
    var union: Rect? = null
    for (offset in token.start until token.endExclusive) {
        val box = getBoundingBox(offset)
        union = union?.let { current ->
            Rect(
                left = min(current.left, box.left),
                top = min(current.top, box.top),
                right = max(current.right, box.right),
                bottom = max(current.bottom, box.bottom),
            )
        } ?: box
    }
    return union?.let { bounds ->
        IntRect(
            left = bounds.left.roundToInt(),
            top = bounds.top.roundToInt(),
            right = bounds.right.roundToInt(),
            bottom = bounds.bottom.roundToInt(),
        )
    }
}

/**
 * Ставить попап по центру над натиснутим токеном. Відступи приходять уже в
 * пікселях, щоб провайдер лишався чистим і перевірявся юніт-тестом.
 */
internal class AboveTokenPopupPositionProvider(
    private val tokenBounds: IntRect,
    private val horizontalMarginPx: Int,
    private val gapPx: Int,
    /** Куди попап став: (лівий край попапа, центр токена, чи він НАД словом). */
    private val onPlaced: (popupX: Int, tokenCenterX: Int, above: Boolean) -> Unit = { _, _, _ -> },
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val tokenCenterX = anchorBounds.left + tokenBounds.left + tokenBounds.width / 2
        val maximumX = (windowSize.width - popupContentSize.width - horizontalMarginPx)
            .coerceAtLeast(horizontalMarginPx)
        val x = (tokenCenterX - popupContentSize.width / 2)
            .coerceIn(horizontalMarginPx, maximumX)
        val aboveY = anchorBounds.top + tokenBounds.top - popupContentSize.height - gapPx
        val above = aboveY >= horizontalMarginPx
        val y = if (above) aboveY else anchorBounds.top + tokenBounds.bottom + gapPx
        onPlaced(x, tokenCenterX, above)
        return IntOffset(x, y)
    }
}
