package com.vocabee.android.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.platform.SpeechInputController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Origin rect (in dp) of the pill that we morph from. */
internal data class AddWordOrigin(
    val left: Dp,
    val top: Dp,
    val width: Dp,
    val height: Dp,
)

internal enum class AddWordMode { Idle, Recording, Results }

/** Result returned by the async backend search. The overlay drives its loading/error UI off this. */
internal data class AddWordSearchState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<TranslationOption> = emptyList(),
    val errorMessage: String? = null,
    val tier: String? = null,
    val maxResults: Int? = null,
)

@Composable
internal fun AddWordOverlay(
    topic: DictionaryTopic,
    accent: Color,
    origin: AddWordOrigin,
    speechInputController: SpeechInputController,
    searchRemote: suspend (query: String) -> AddWordSearchState,
    onAddWord: (source: String, translation: String, ipa: String?, details: com.vocabee.android.domain.model.WordDetails?) -> Unit,
    onRemoveWord: (translation: String) -> Unit,
    onClose: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val cleanedQuery = query.trim()
    var searchState by remember { mutableStateOf(AddWordSearchState()) }
    val addedCount = remember { mutableStateOf(0) }

    // Live set of translations currently in this topic — recomputed on every recompose so
    // tapping "+" or "✓" flips the per-row state immediately (the host re-renders us with
    // a fresh `topic` after the store update).
    val existingTranslations = remember(topic.words) {
        topic.words.map { it.translation.trim().lowercase() }.toSet()
    }

    var partialText by remember { mutableStateOf("") }
    var heardText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val morph = remember { Animatable(0f) }
    val content = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        morph.animateTo(1f, animationSpec = tween(420))
        content.animateTo(1f, animationSpec = tween(280))
    }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            content.animateTo(0f, animationSpec = tween(150))
            morph.animateTo(0f, animationSpec = tween(380))
            onClose()
        }
    }

    DisposableEffect(speechInputController) {
        onDispose { speechInputController.stopListening() }
    }

    // Debounced backend search: every keystroke / voice result re-runs the search
    // 1s after the user pauses typing. `isLoading=true` is set immediately so the
    // spinner is visible throughout the wait — the LaunchedEffect cancels and
    // restarts on each new character, keeping the spinner pinned while typing.
    LaunchedEffect(cleanedQuery) {
        if (cleanedQuery.isEmpty()) {
            searchState = AddWordSearchState()
            return@LaunchedEffect
        }
        searchState = searchState.copy(query = cleanedQuery, isLoading = true, errorMessage = null)
        delay(1000)
        searchState = searchRemote(cleanedQuery)
    }

    fun resetSpeech() {
        partialText = ""
        heardText = ""
        speechError = null
    }

    fun startListening() {
        resetSpeech()
        query = ""
        speechInputController.startListening(
            languageTag = topic.sourceLanguage.speechTag,
            onPartialResult = { partialText = it },
            onResult = { recognized ->
                val text = recognized.trim()
                heardText = text
                partialText = ""
                isListening = false
                if (text.isNotBlank()) query = text
            },
            onError = { message ->
                speechError = message
                partialText = ""
                isListening = false
            },
            onListeningChanged = { isListening = it },
        )
    }

    suspend fun stopListeningWithGrace() {
        delay(700)
        speechInputController.stopListening()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { /* swallow taps on backdrop */ },
    ) {
        // Backdrop fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f * morph.value)),
        )

        // Morphing surface — interpolates from pill at origin to full screen
        val cornerDp = (32f * (1f - morph.value)).dp
        val color = lerpColor(accent, PrototypeColor.White, morph.value)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val targetWidth = size.width
                    val targetHeight = size.height
                    val scaleXVal = (origin.width.toPx() + (targetWidth - origin.width.toPx()) * morph.value) / targetWidth
                    val scaleYVal = (origin.height.toPx() + (targetHeight - origin.height.toPx()) * morph.value) / targetHeight
                    val translateXVal = origin.left.toPx() * (1f - morph.value)
                    val translateYVal = origin.top.toPx() * (1f - morph.value)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    scaleX = scaleXVal
                    scaleY = scaleYVal
                    translationX = translateXVal
                    translationY = translateYVal
                }
                .clip(RoundedCornerShape(cornerDp))
                .background(color),
        )

        if (content.value > 0.001f) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .graphicsLayer { alpha = content.value },
            ) {
                AddWordHeader(
                    topic = topic,
                    onClose = ::close,
                )

                AddWordSearchField(
                    value = query,
                    onValueChange = { value ->
                        query = value
                        if (value.isNotEmpty()) heardText = value
                    },
                    onClear = {
                        query = ""
                        resetSpeech()
                    },
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f, fill = true)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    when {
                        isListening || cleanedQuery.isBlank() -> MicStage(
                            accent = accent,
                            isListening = isListening,
                            speechError = speechError,
                            onStart = ::startListening,
                            onStop = { scope.launch { stopListeningWithGrace() } },
                        )
                        searchState.isLoading -> AddWordLoadingState(accent = accent)
                        searchState.errorMessage != null -> AddWordErrorState(
                            message = searchState.errorMessage!!,
                            accent = accent,
                        )
                        else -> AddWordResultsList(
                            query = cleanedQuery,
                            results = searchState.results,
                            tier = searchState.tier,
                            maxResults = searchState.maxResults,
                            accent = accent,
                            existingTranslations = existingTranslations,
                            onAdd = { option ->
                                // Save the canonical learning-word from the variant,
                                // not the user's raw typing. Otherwise a prefix
                                // suggestion ("circumstance" shown while typing
                                // "circum") would persist as "circum".
                                onAddWord(option.learningWord, option.value, option.ipa, option.details)
                                addedCount.value = addedCount.value + 1
                            },
                            onRemove = { option ->
                                onRemoveWord(option.value)
                            },
                        )
                    }
                }

                if (addedCount.value > 0) {
                    AddedCountBar(count = addedCount.value, onDone = ::close)
                }
                Spacer(modifier = Modifier.imePadding())
            }
        }
    }
}

@Composable
private fun AddWordHeader(
    topic: DictionaryTopic,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Додати у ",
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                text = "«${topic.title}»",
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Read-only language pair indicator. Was a dropdown opening the language
        // sheet — dropped because language for a topic isn't editable here (it's
        // baked in at topic-create time). Just two flags + arrow now.
        Surface(
            shape = RoundedCornerShape(11.dp),
            color = PrototypeColor.NeutralSurface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(languageFlag(topic.sourceLanguage.code), fontSize = 16.sp)
                PrototypeLineIcon(
                    icon = PrototypeIcon.ArrowRight,
                    modifier = Modifier.size(12.dp),
                    color = PrototypeColor.Muted2,
                    strokeWidth = 2f,
                )
                Text(languageFlag(topic.targetLanguage.code), fontSize = 16.sp)
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(PrototypeColor.NeutralSurface)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Close,
                modifier = Modifier.size(22.dp),
                color = PrototypeColor.Muted,
                strokeWidth = 2.2f,
            )
        }
    }
}

@Composable
private fun AddWordSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Don't auto-focus to avoid forcing keyboard open immediately
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PrototypeColor.FieldBg)
            .border(BorderStroke(1.5.dp, PrototypeColor.Line), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        PrototypeLineIcon(
            icon = PrototypeIcon.Search,
            modifier = Modifier.size(20.dp),
            color = PrototypeColor.Muted2,
            strokeWidth = 2f,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            textStyle = TextStyle(
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrototypeColor.Purple),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            "Введи слово англійською…",
                            color = PrototypeColor.Muted2,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        )
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.ChipNeutralBg)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Close,
                    modifier = Modifier.size(16.dp),
                    color = PrototypeColor.Muted2,
                    strokeWidth = 2.2f,
                )
            }
        }
    }
}

/**
 * Single composable that hosts both the idle and recording states. We previously
 * had two separate column layouts — when the user pressed the mic, the waveform
 * appeared ABOVE the button and pushed the whole column down, making the mic
 * "jump". Now the waveform slot is always present (fixed 80dp + 30dp spacer),
 * just empty when idle, so the mic stays nailed to the same vertical position.
 */
@Composable
private fun MicStage(
    accent: Color,
    isListening: Boolean,
    speechError: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val micColor = if (isListening) PrototypeColor.Orange else accent
    val primary = when {
        isListening -> "Слухаю…"
        speechError != null -> "Не вдалося розпізнати"
        else -> "Продиктуй слово"
    }
    val secondary = when {
        isListening -> "торкнись, щоб зупинити"
        else -> speechError ?: "або почни вводити його у поле вгорі"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Reserved waveform slot — same height whether or not we're listening,
        // so the mic doesn't move between states.
        Box(
            modifier = Modifier.height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isListening) VoiceWaveform()
        }
        Spacer(modifier = Modifier.height(30.dp))
        HoldToTalkButton(
            color = micColor,
            listening = isListening,
            onStart = onStart,
            onStop = onStop,
        )
        Text(
            text = primary,
            modifier = Modifier.padding(top = if (isListening) 24.dp else 22.dp),
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (isListening) 17.sp else 18.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = secondary,
            modifier = Modifier.padding(top = if (isListening) 5.dp else 6.dp),
            color = PrototypeColor.Muted,
            fontWeight = if (isListening) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = if (isListening) 13.5.sp else 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HoldToTalkButton(
    color: Color,
    listening: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Box(
        modifier = Modifier.size(116.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (listening) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(3.dp, PrototypeColor.Orange.copy(alpha = 0.42f)), CircleShape),
            )
        }
        Surface(
            modifier = Modifier
                .size(104.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onStart()
                            try {
                                tryAwaitRelease()
                            } finally {
                                onStop()
                            }
                        },
                    )
                },
            shape = CircleShape,
            color = color,
            shadowElevation = 14.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Mic,
                    modifier = Modifier.size(if (listening) 32.dp else 34.dp),
                    color = PrototypeColor.White,
                    strokeWidth = 1.9f,
                )
            }
        }
    }
}

/**
 * Animated waveform — 28 bars pulsing between 8dp and 56dp height with a staggered
 * phase, matching the prototype's CSS `@keyframes waveBar` (0.7s ease-in-out
 * alternate, delay = (k * 45) mod 600 ms).
 *
 * One [rememberInfiniteTransition] drives all bars; per-bar phase comes from a
 * [StartOffset] so we don't pay for 28 independent transitions.
 */
@Composable
private fun VoiceWaveform() {
    val transition = rememberInfiniteTransition(label = "voice-waveform")
    val barCount = 28
    val cycleMillis = 700

    Row(
        modifier = Modifier.height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(barCount) { index ->
            val offsetMillis = (index * 45) % 600
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = cycleMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(offsetMillis),
                ),
                label = "bar-$index",
            )
            // Match prototype range: 8dp → 56dp, opacity 0.5 → 1.0.
            val barHeight = (8f + progress * 48f).dp
            val barAlpha = 0.5f + progress * 0.5f
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(PrototypeColor.Orange.copy(alpha = barAlpha)),
            )
        }
    }
}

@Composable
private fun AddWordLoadingState(accent: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = accent,
            strokeWidth = 3.dp,
        )
        Text(
            text = "Шукаю переклад…",
            modifier = Modifier.padding(top = 16.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun AddWordErrorState(message: String, accent: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PrototypeLineIcon(
            icon = PrototypeIcon.Close,
            modifier = Modifier.size(26.dp),
            color = accent.copy(alpha = 0.72f),
            strokeWidth = 2f,
        )
        Text(
            text = "Не вдалось отримати переклад",
            modifier = Modifier.padding(top = 12.dp),
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 6.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddWordResultsList(
    query: String,
    results: List<TranslationOption>,
    tier: String?,
    maxResults: Int?,
    accent: Color,
    existingTranslations: Set<String>,
    onAdd: (TranslationOption) -> Unit,
    onRemove: (TranslationOption) -> Unit,
) {
    if (results.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Search,
                modifier = Modifier.size(26.dp),
                color = Color(0xFFD1D5DB),
                strokeWidth = 1.7f,
            )
            Text(
                text = "Нічого не знайдено для «$query»",
                modifier = Modifier.padding(top = 12.dp),
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        items(results, key = { it.value }) { option ->
            // Live "is this translation in the topic right now?" check. Either the
            // server marked it `alreadyAdded` at search time, OR the user just
            // tapped "+" on it during this overlay session and the topic state
            // updated. Both flow through the same set so the toggle is instant.
            val isAdded = option.alreadyAdded ||
                existingTranslations.contains(option.value.trim().lowercase())
            AddWordResultRow(
                query = query,
                option = option,
                accent = accent,
                isAdded = isAdded,
                onAdd = { onAdd(option) },
                onRemove = { onRemove(option) },
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Sparkle,
                    modifier = Modifier.size(13.dp),
                    color = PrototypeColor.Purple,
                    strokeWidth = 1.7f,
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = footerCaptionFor(tier = tier, maxResults = maxResults),
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

private fun footerCaptionFor(tier: String?, maxResults: Int?): String {
    // Per-tier caps were lifted server-side — anonymous/registered/premium all
    // see whatever the provider returns. Keep the AI-attribution line; drop the
    // misleading "до N варіантів" + "увійди для більше" copy.
    return "Переклади та приклади згенеровано AI"
}

@Composable
private fun AddWordResultRow(
    query: String,
    option: TranslationOption,
    accent: Color,
    isAdded: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val rowBg = if (isAdded) Color(0xFFF6F7FB) else PrototypeColor.White
    val rowBorder = if (isAdded) Color.Transparent else PrototypeColor.Line

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(rowBg)
            .border(BorderStroke(1.5.dp, rowBorder), RoundedCornerShape(17.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    // Canonical word from the gateway response, not the user's raw
                    // typing — so "circumstanc" still renders "circumstance".
                    text = option.learningWord,
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (option.ipa != null) {
                    Text(
                        text = option.ipa,
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PrototypeLineIcon(
                    icon = PrototypeIcon.Sparkle,
                    modifier = Modifier.size(11.dp),
                    color = PrototypeColor.Purple,
                    strokeWidth = 1.7f,
                )
            }
            Text(
                text = option.value,
                modifier = Modifier.padding(top = 3.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 44×44 toggle button — same geometry whether adding or removing so the
        // row doesn't jump when state flips. Purple ✓ when in the dictionary
        // (click to remove), accent + when not (click to add).
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isAdded) PrototypeColor.Purple else accent)
                .clickable(onClick = if (isAdded) onRemove else onAdd),
            contentAlignment = Alignment.Center,
        ) {
            PrototypeLineIcon(
                icon = if (isAdded) PrototypeIcon.Check else PrototypeIcon.Plus,
                modifier = Modifier.size(20.dp),
                color = PrototypeColor.White,
                strokeWidth = 2.6f,
            )
        }
    }
}

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * clamped,
        green = start.green + (end.green - start.green) * clamped,
        blue = start.blue + (end.blue - start.blue) * clamped,
        alpha = start.alpha + (end.alpha - start.alpha) * clamped,
    )
}

@Composable
private fun AddedCountBar(count: Int, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, PrototypeColor.DividerLight), RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row {
            Text(
                text = count.toString(),
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
            )
            Text(
                text = " " + if (count == 1) "слово додано" else "слів додано",
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
        Text(
            text = "Готово",
            modifier = Modifier
                .clickable(onClick = onDone)
                .padding(8.dp),
            color = PrototypeColor.Purple,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        )
    }
}
