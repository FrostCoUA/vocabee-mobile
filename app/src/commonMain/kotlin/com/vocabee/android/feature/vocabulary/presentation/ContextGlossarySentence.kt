package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
    var textLayout by remember(glossary) { mutableStateOf<TextLayoutResult?>(null) }
    var selectedIndex by remember(glossary) { mutableStateOf<Int?>(null) }
    var selectedBounds by remember(glossary) { mutableStateOf<IntRect?>(null) }
    val selectedToken = selectedIndex?.let(glossary.tokens::getOrNull)
    val targetTokenIndexes = remember(glossary, targetWord) {
        contextTargetTokenIndexes(glossary, targetWord)
    }
    val displayedSentence = remember(glossary, targetWord, selectedToken) {
        clickableContextSentence(
            sentence = glossary.sentence,
            tokens = glossary.tokens,
            targetTokenIndexes = targetTokenIndexes,
            selectedToken = selectedToken,
        )
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            delay(2_400)
            selectedIndex = null
            selectedBounds = null
        }
    }

    Box(modifier = modifier) {
        Text(
            text = displayedSentence,
            modifier = Modifier.fillMaxWidth().pointerInput(glossary) {
                detectTapGestures { position ->
                    val layout = textLayout ?: return@detectTapGestures
                    val offset = layout.getOffsetForPosition(position)
                    val candidateIndex = contextTokenIndexAtOffset(glossary, offset)
                        ?.takeUnless(targetTokenIndexes::contains)
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
                selectedBounds = selectedToken?.let(layout::tokenBounds)
            },
        )

        if (selectedToken != null && selectedBounds != null) {
            Popup(
                popupPositionProvider = AboveTokenPopupPositionProvider(selectedBounds!!),
                onDismissRequest = {
                    selectedIndex = null
                    selectedBounds = null
                },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrototypeColor.White,
                    border = BorderStroke(1.dp, PrototypeColor.PurpleText.copy(alpha = 0.22f)),
                    shadowElevation = 10.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 13.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedToken.surface,
                            color = PrototypeColor.Muted2,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = selectedToken.translation,
                            color = PrototypeColor.Ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                        }
                        val savedKey = contextBookmarkKey(glossary, selectedToken)
                        val isSaved = savedKey in savedKeys
                        Box(
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSaved) PrototypeColor.Yellow.copy(alpha = 0.34f)
                                    else PrototypeColor.Tint,
                                )
                                .clickable(enabled = action == ContextGlossaryTokenAction.Bookmark || !isSaved) {
                                    onAction(selectedToken)
                                    selectedIndex = null
                                    selectedBounds = null
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            PrototypeLineIcon(
                                icon = when {
                                    action == ContextGlossaryTokenAction.AddToDictionary && isSaved -> PrototypeIcon.Check
                                    action == ContextGlossaryTokenAction.AddToDictionary -> PrototypeIcon.Plus
                                    else -> PrototypeIcon.Bookmark
                                },
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
            }
        }
    }
}

internal enum class ContextGlossaryTokenAction {
    Bookmark,
    AddToDictionary,
}

internal fun contextTokenIndexAtOffset(glossary: ContextGlossary, offset: Int): Int? =
    glossary.tokens.indexOfFirst { token -> offset in token.start until token.endExclusive }
        .takeIf { it >= 0 }

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

private fun clickableContextSentence(
    sentence: String,
    tokens: List<ContextGlossaryToken>,
    targetTokenIndexes: Set<Int>,
    selectedToken: ContextGlossaryToken?,
): AnnotatedString = buildAnnotatedString {
    append(sentence)
    tokens.forEachIndexed { index, token ->
        addStyle(
            style = if (index in targetTokenIndexes) {
                SpanStyle(
                    background = PrototypeColor.Yellow,
                    color = PrototypeColor.YellowText,
                    fontWeight = FontWeight.ExtraBold,
                )
            } else {
                SpanStyle(textDecoration = TextDecoration.Underline)
            },
            start = token.start,
            end = token.endExclusive,
        )
    }
    if (selectedToken != null) {
        addStyle(
            style = SpanStyle(
                color = PrototypeColor.PurpleText,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.ExtraBold,
            ),
            start = selectedToken.start,
            end = selectedToken.endExclusive,
        )
    }
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

private class AboveTokenPopupPositionProvider(
    private val tokenBounds: IntRect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val horizontalMargin = 8
        val gap = 9
        val tokenCenterX = anchorBounds.left + tokenBounds.left + tokenBounds.width / 2
        val maximumX = (windowSize.width - popupContentSize.width - horizontalMargin)
            .coerceAtLeast(horizontalMargin)
        val x = (tokenCenterX - popupContentSize.width / 2)
            .coerceIn(horizontalMargin, maximumX)
        val aboveY = anchorBounds.top + tokenBounds.top - popupContentSize.height - gap
        val y = if (aboveY >= horizontalMargin) {
            aboveY
        } else {
            anchorBounds.top + tokenBounds.bottom + gap
        }
        return IntOffset(x, y)
    }
}
