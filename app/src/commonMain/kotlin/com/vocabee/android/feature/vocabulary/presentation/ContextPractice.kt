package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.prototypeTopicTheme
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* ============================================================
 * «Слово в контексті» — pair-based practice (design board 6).
 * A pair = (source word, one of its translations); eligible words
 * have 2+ saved translations; each trainable pair needs an example
 * sentence (WordDetails.senses[].examples).
 * ============================================================ */

internal enum class PracticeMode { Classic, Context }

internal data class ContextPair(
    val topicId: String,
    val topicTitle: String,
    val accent: Color,
    val word: WordEntry,
    val sentence: String,
    /** All translations of this source word in the topic (including own). */
    val groupTranslations: List<String>,
    /** translation → its example sentence, for the mini-hint on a wrong pick. */
    val sentenceByTranslation: Map<String, String>,
)

private enum class ContextDirection { EnToUk, UkToEn }

private sealed interface ContextCard {
    val key: String

    data class Recognition(
        val pair: ContextPair,
        val options: List<String>,
        val direction: ContextDirection,
    ) : ContextCard {
        override val key: String = "recog:${pair.word.id}:${direction.name}"
    }

    data class Recall(val pair: ContextPair) : ContextCard {
        override val key: String = "recall:${pair.word.id}"
    }

    data class Matching(
        val source: String,
        val ipa: String?,
        val topicTitle: String,
        val pairs: List<ContextPair>,
    ) : ContextCard {
        override val key: String = "match:${pairs.first().word.id}"
    }
}

private data class ConfusionEntry(
    val source: String,
    val correctTranslation: String,
    val chosenTranslation: String,
)

/* ---------- peek & bookmark (design board 7) ---------- */

internal data class PeekBookmark(
    val word: String,
    val sentence: String,
    val topicId: String,
)

private sealed interface PeekState {
    data object Loading : PeekState

    data class Flipped(
        val text: String,
        val savedTopicTitle: String? = null,
    ) : PeekState
}

private data class ActivePeek(
    val cardKey: String,
    val wordKey: String,
    val state: PeekState,
)

/** Config-driven later (D4); free in-round peeks before coins kick in. */
private const val FreePeeksPerRound = 5

private fun normalizePeekWord(raw: String): String =
    raw.trim().trim { !it.isLetter() }.lowercase()

/* ---------- eligibility & deck building ---------- */

internal fun WordEntry.contextSentence(): String? {
    return details?.senses
        ?.firstNotNullOfOrNull { sense -> sense.examples.firstOrNull { it.isNotBlank() } }
        ?.trim()
}

private fun normalizedSource(word: WordEntry): String = word.source.trim().lowercase()

/**
 * Члени групи, чиє речення унікальне всередині групи. Коли переклади ділять один
 * details-блоб (те саме слово збережене двічі), речення збігаються — тоді жодну
 * відповідь не можна назвати правильною чесно, і такі члени не тренуються.
 */
private fun uniqueSentenceMembers(group: List<WordEntry>): List<Pair<WordEntry, String>> {
    val withSentence = group.mapNotNull { member -> member.contextSentence()?.let { member to it } }
    val counts = withSentence.groupingBy { it.second.trim().lowercase() }.eachCount()
    return withSentence.filter { (counts[it.second.trim().lowercase()] ?: 0) == 1 }
}

/** Words that belong to a 2+-translation group and carry a unique example sentence. */
internal fun DictionaryTopic.contextPairCount(): Int {
    return words.groupBy(::normalizedSource)
        .values
        .filter { group -> group.size >= 2 }
        .sumOf { group -> uniqueSentenceMembers(group).size }
}

private fun buildContextPairs(topics: List<DictionaryTopic>): List<ContextPair> {
    return topics.flatMap { topic ->
        val accent = prototypeTopicTheme(topic.coverIndex).color
        topic.words.groupBy(::normalizedSource)
            .values
            .filter { group -> group.size >= 2 }
            .flatMap { group ->
                val translations = group.map { it.translation }
                val members = uniqueSentenceMembers(group)
                val sentences = members.associate { (member, sentence) -> member.translation to sentence }
                members.map { (member, sentence) ->
                    ContextPair(
                        topicId = topic.id,
                        topicTitle = topic.title,
                        accent = accent,
                        word = member,
                        sentence = sentence,
                        groupTranslations = translations,
                        sentenceByTranslation = sentences,
                    )
                }
            }
    }
}

private const val RecallThreshold = 40
private const val ProductionThreshold = 60
private const val MatchingThreshold = 50
private const val ContextDeckSize = 10

private fun buildContextDeck(
    topics: List<DictionaryTopic>,
    seed: Int,
): List<ContextCard> {
    val random = Random(seed)
    val pairs = buildContextPairs(topics)
    if (pairs.isEmpty()) return emptyList()

    val cards = mutableListOf<ContextCard>()
    val consumedWordIds = mutableSetOf<String>()

    // Matching = the word's "final boss": 3+ sentence-backed pairs, all mature.
    val matchingGroups = pairs
        .groupBy { it.topicId to normalizedSource(it.word) }
        .values
        .filter { group ->
            group.size >= 3 && group.all { it.word.knowledgePercent >= MatchingThreshold }
        }
        .shuffled(random)
        .take(1)
    matchingGroups.forEach { group ->
        val shown = group.shuffled(random).take(4)
        cards += ContextCard.Matching(
            source = shown.first().word.source,
            ipa = shown.first().word.ipa,
            topicTitle = shown.first().topicTitle,
            pairs = shown,
        )
        consumedWordIds += shown.map { it.word.id }
    }

    // Weakest pairs first; format and direction mature with the pair.
    val singles = pairs
        .filter { it.word.id !in consumedWordIds }
        .shuffled(random)
        .sortedBy { it.word.knowledgePercent.coerceIn(0, 100) }

    val allSources = pairs.map { it.word.source }.distinct()
    singles.forEach { pair ->
        val knowledge = pair.word.knowledgePercent
        cards += when {
            knowledge >= ProductionThreshold -> {
                val distractors = allSources
                    .filter { !it.equals(pair.word.source, ignoreCase = true) }
                    .shuffled(random)
                    .take(3)
                ContextCard.Recognition(
                    pair = pair,
                    options = (distractors + pair.word.source).shuffled(random),
                    direction = ContextDirection.UkToEn,
                )
            }

            knowledge >= RecallThreshold -> ContextCard.Recall(pair)

            else -> {
                val distractors = pair.groupTranslations
                    .filter { it != pair.word.translation }
                    .shuffled(random)
                    .take(3)
                ContextCard.Recognition(
                    pair = pair,
                    options = (distractors + pair.word.translation).shuffled(random),
                    direction = ContextDirection.EnToUk,
                )
            }
        }
        if (cards.size >= ContextDeckSize) return@forEach
    }

    // Anti-repeat: avoid the same source word on adjacent cards.
    val deck = cards.take(ContextDeckSize).toMutableList()
    for (i in 1 until deck.size) {
        if (deck[i].sourceKey() == deck[i - 1].sourceKey()) {
            val swap = (i + 1 until deck.size).firstOrNull { deck[it].sourceKey() != deck[i - 1].sourceKey() }
            if (swap != null) {
                val tmp = deck[i]
                deck[i] = deck[swap]
                deck[swap] = tmp
            }
        }
    }
    return deck
}

private fun ContextCard.sourceKey(): String = when (this) {
    is ContextCard.Recognition -> normalizedSource(pair.word)
    is ContextCard.Recall -> normalizedSource(pair.word)
    is ContextCard.Matching -> source.trim().lowercase()
}

/* ---------- sentence highlight ---------- */

private fun highlightedSentence(
    sentence: String,
    target: String,
    blank: Boolean = false,
): AnnotatedString {
    val highlightStyle = SpanStyle(
        background = PrototypeColor.Yellow,
        color = PrototypeColor.YellowText,
        fontWeight = FontWeight.ExtraBold,
    )
    val match = Regex("(?i)\\b" + Regex.escape(target.trim()) + "[A-Za-z'’-]*")
        .find(sentence)
        ?: return buildAnnotatedString { append(sentence) }
    return buildAnnotatedString {
        append(sentence.substring(0, match.range.first))
        withStyle(highlightStyle) {
            append(if (blank) " ____ " else match.value)
        }
        append(sentence.substring(match.range.last + 1))
    }
}

/* ============================================================
 * Setup additions: mode cards + empty state
 * ============================================================ */

@Composable
internal fun PracticeModeCards(
    mode: PracticeMode,
    onModeChange: (PracticeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        PracticeModeCard(
            title = "Класика",
            subtitle = "флеш-картки, всі слова",
            icon = PrototypeIcon.Cards,
            selected = mode == PracticeMode.Classic,
            onClick = { onModeChange(PracticeMode.Classic) },
            modifier = Modifier.weight(1f),
        )
        PracticeModeCard(
            title = "Слово в контексті",
            subtitle = "для слів із 2+ перекладами",
            icon = PrototypeIcon.Chat,
            selected = mode == PracticeMode.Context,
            onClick = { onModeChange(PracticeMode.Context) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PracticeModeCard(
    title: String,
    subtitle: String,
    icon: PrototypeIcon,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PrototypeColor.White,
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) PrototypeColor.Purple else PrototypeColor.Line,
        ),
        shadowElevation = if (selected) 8.dp else 4.dp,
    ) {
        Box(modifier = Modifier.padding(start = 14.dp, top = 15.dp, end = 14.dp, bottom = 15.dp)) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(PrototypeColor.Purple),
                    contentAlignment = Alignment.Center,
                ) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Check,
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.6f,
                    )
                }
            }
            Column {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) PrototypeColor.Tint else PrototypeColor.NeutralSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLineIcon(
                            icon = icon,
                            modifier = Modifier.size(22.dp),
                            color = if (selected) PrototypeColor.PurpleText else PrototypeColor.Muted,
                            strokeWidth = 2f,
                        )
                    }
                }
                // Long titles («Слово в контексті») shrink instead of ellipsizing.
                var titleScale by remember(title) { mutableStateOf(1f) }
                Text(
                    text = title,
                    modifier = Modifier.padding(top = 12.dp),
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp * titleScale,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (result.hasVisualOverflow && titleScale > 0.72f) {
                            titleScale -= 0.04f
                        }
                    },
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
internal fun ContextPracticeEmptyState(
    onOpenDictionaries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            Surface(
                modifier = Modifier.padding(top = 10.dp, start = 10.dp).size(width = 128.dp, height = 78.dp),
                shape = RoundedCornerShape(13.dp),
                color = PrototypeColor.White,
                border = BorderStroke(1.5.dp, PrototypeColor.Line),
            ) {}
            Surface(
                modifier = Modifier.size(width = 128.dp, height = 78.dp),
                shape = RoundedCornerShape(13.dp),
                color = PrototypeColor.Tint.copy(alpha = 0.55f),
            ) {}
        }
        Text(
            text = "Немає слів із кількома перекладами",
            modifier = Modifier.padding(top = 18.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Такі пари з’являються, коли слово має 2+ збережені значення. Додай перекладів — і цей режим відкриється.",
            modifier = Modifier.padding(top = 9.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 14.5.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable(onClick = onOpenDictionaries),
            shape = RoundedCornerShape(16.dp),
            color = PrototypeColor.NeutralSurface,
            border = BorderStroke(1.4.dp, PrototypeColor.Line),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Book,
                    modifier = Modifier.size(19.dp),
                    color = PrototypeColor.Ink,
                    strokeWidth = 2f,
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = "До словників",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

/* ============================================================
 * Session
 * ============================================================ */

@Composable
internal fun ContextPracticeSession(
    topics: List<DictionaryTopic>,
    allTopics: List<DictionaryTopic>,
    onAnswerWord: (topicId: String, wordId: String, deltaPercent: Int) -> Unit,
    onExit: () -> Unit,
    peekTranslate: suspend (word: String, topic: DictionaryTopic) -> String?,
    canSpendPeek: () -> Boolean,
    onSpendPeek: () -> Boolean,
    onPeekBlocked: () -> Unit,
    onOpenBookmark: (topicId: String, word: String) -> Unit,
    onRoundCompleted: () -> Unit,
) {
    val topicIds = topics.map { it.id }
    var shuffleSeed by remember(topicIds) { mutableIntStateOf(Random.nextInt()) }
    val deck = remember(topicIds, shuffleSeed) { buildContextDeck(topics, shuffleSeed) }
    var index by remember(deck) { mutableIntStateOf(0) }
    var correctAnswers by remember(deck) { mutableIntStateOf(0) }
    var done by remember(deck) { mutableStateOf(false) }
    val confusions = remember(deck) { mutableListOf<ConfusionEntry>() }

    // --- peek & bookmarks (board 7) ---
    val scope = rememberCoroutineScope()
    var freePeeksLeft by remember(deck) { mutableIntStateOf(FreePeeksPerRound) }
    val peekCache = remember(deck) { mutableStateMapOf<String, String>() }
    val bookmarks = remember(deck) { mutableStateListOf<PeekBookmark>() }
    var activePeek by remember(deck) { mutableStateOf<ActivePeek?>(null) }

    fun addBookmark(rawWord: String, sentence: String, topicId: String) {
        val word = normalizePeekWord(rawWord)
        if (word.isBlank() || bookmarks.any { it.word == word }) return
        bookmarks += PeekBookmark(word = word, sentence = sentence, topicId = topicId)
    }

    fun requestPeek(cardKey: String, rawWord: String, topicId: String) {
        val word = normalizePeekWord(rawWord)
        if (word.isBlank()) return
        val wordKey = "$cardKey:$word"
        if (activePeek?.wordKey == wordKey) {
            activePeek = null
            return
        }

        // Saved words: instant, free, offline — user's own translations.
        val savedMatches = allTopics.flatMap { topic ->
            topic.words.filter { normalizedSource(it) == word }.map { topic to it }
        }
        if (savedMatches.isNotEmpty()) {
            val translations = savedMatches.map { it.second.translation }.distinct().take(3)
            activePeek = ActivePeek(
                cardKey = cardKey,
                wordKey = wordKey,
                state = PeekState.Flipped(
                    text = translations.joinToString(" · "),
                    savedTopicTitle = savedMatches.first().first.title,
                ),
            )
            return
        }

        peekCache[word]?.let { cached ->
            activePeek = ActivePeek(cardKey, wordKey, PeekState.Flipped(cached))
            return
        }

        if (freePeeksLeft <= 0) {
            if (!canSpendPeek() || !onSpendPeek()) {
                onPeekBlocked()
                return
            }
        } else {
            freePeeksLeft -= 1
        }

        val topic = allTopics.firstOrNull { it.id == topicId } ?: topics.firstOrNull() ?: return
        activePeek = ActivePeek(cardKey, wordKey, PeekState.Loading)
        scope.launch {
            val translation = peekTranslate(word, topic)
            if (activePeek?.wordKey != wordKey) return@launch
            if (translation.isNullOrBlank()) {
                activePeek = null
            } else {
                peekCache[word] = translation
                activePeek = ActivePeek(cardKey, wordKey, PeekState.Flipped(translation))
            }
        }
    }

    // Auto-revert the flip after ~2.2s.
    LaunchedEffect(activePeek) {
        val current = activePeek ?: return@LaunchedEffect
        if (current.state is PeekState.Flipped) {
            delay(2200)
            if (activePeek == current) activePeek = null
        }
    }

    fun moveNext() {
        activePeek = null
        if (index + 1 >= deck.size) {
            done = true
            onRoundCompleted()
        } else {
            index += 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background)
            .statusBarsPadding(),
    ) {
        ContextSessionHeader(
            step = (index + 1).coerceAtMost(deck.size),
            total = deck.size,
            correct = correctAnswers,
            showProgress = deck.isNotEmpty() && !done,
            bookmarksCount = bookmarks.size,
            onClose = onExit,
        )

        when {
            deck.isEmpty() -> ContextPracticeEmptyState(
                onOpenDictionaries = onExit,
                modifier = Modifier.weight(1f),
            )

            done -> ContextDoneState(
                correctAnswers = correctAnswers,
                total = deck.size,
                confusions = confusions,
                bookmarks = bookmarks,
                onOpenBookmark = onOpenBookmark,
                onRestart = {
                    shuffleSeed = Random.nextInt()
                },
                onChooseTopics = onExit,
                modifier = Modifier.weight(1f),
            )

            else -> {
                val card = deck[index]
                Box(modifier = Modifier.weight(1f)) {
                    when (card) {
                        is ContextCard.Recognition -> RecognitionCardView(
                            card = card,
                            activePeek = activePeek,
                            freePeeksLeft = freePeeksLeft,
                            onPeek = { word -> requestPeek(card.key, word, card.pair.topicId) },
                            onBookmark = { word -> addBookmark(word, card.pair.sentence, card.pair.topicId) },
                            onBookmarkFromPeek = { word ->
                                addBookmark(word, card.pair.sentence, card.pair.topicId)
                                activePeek = null
                            },
                            onAnswered = { correct, chosen ->
                                onAnswerWord(
                                    card.pair.topicId,
                                    card.pair.word.id,
                                    if (correct) KnowledgeStepPercent else -KnowledgeStepPercent,
                                )
                                if (correct) {
                                    correctAnswers += 1
                                } else if (chosen != null && card.direction == ContextDirection.EnToUk) {
                                    confusions += ConfusionEntry(
                                        source = card.pair.word.source,
                                        correctTranslation = card.pair.word.translation,
                                        chosenTranslation = chosen,
                                    )
                                }
                            },
                            onNext = ::moveNext,
                        )

                        is ContextCard.Recall -> RecallCardView(
                            card = card,
                            activePeek = activePeek,
                            freePeeksLeft = freePeeksLeft,
                            onPeek = { word -> requestPeek(card.key, word, card.pair.topicId) },
                            onBookmark = { word -> addBookmark(word, card.pair.sentence, card.pair.topicId) },
                            onBookmarkFromPeek = { word ->
                                addBookmark(word, card.pair.sentence, card.pair.topicId)
                                activePeek = null
                            },
                            onAnswered = { correct ->
                                onAnswerWord(
                                    card.pair.topicId,
                                    card.pair.word.id,
                                    if (correct) KnowledgeStepPercent else -KnowledgeStepPercent,
                                )
                                if (correct) correctAnswers += 1
                            },
                            onNext = ::moveNext,
                        )

                        is ContextCard.Matching -> MatchingCardView(
                            card = card,
                            onPairResolved = { pair, firstTryCorrect, chosen ->
                                onAnswerWord(
                                    pair.topicId,
                                    pair.word.id,
                                    if (firstTryCorrect) KnowledgeStepPercent else -KnowledgeStepPercent,
                                )
                                if (!firstTryCorrect && chosen != null) {
                                    confusions += ConfusionEntry(
                                        source = pair.word.source,
                                        correctTranslation = pair.word.translation,
                                        chosenTranslation = chosen,
                                    )
                                }
                            },
                            onAllResolved = { allFirstTry ->
                                if (allFirstTry) correctAnswers += 1
                            },
                            onNext = ::moveNext,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextSessionHeader(
    step: Int,
    total: Int,
    correct: Int,
    showProgress: Boolean,
    bookmarksCount: Int,
    onClose: () -> Unit,
) {
    Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Слово в контексті",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrototypeColor.Ink,
                    letterSpacing = (-0.4).sp,
                )
                if (showProgress) {
                    Text(
                        text = "$step / $total · правильно $correct",
                        modifier = Modifier.padding(top = 3.dp),
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                    )
                }
            }
            if (bookmarksCount > 0) {
                Surface(
                    modifier = Modifier.padding(end = 9.dp),
                    shape = CircleShape,
                    color = PrototypeColor.Yellow.copy(alpha = 0.25f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Bookmark,
                            modifier = Modifier.size(14.dp),
                            color = PrototypeColor.NoteYellowText,
                            strokeWidth = 2.2f,
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$bookmarksCount",
                            color = PrototypeColor.NoteYellowText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        )
                    }
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
                    modifier = Modifier.size(19.dp),
                    color = PrototypeColor.Muted,
                    strokeWidth = 2.2f,
                )
            }
        }
        if (showProgress) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.ProgressTrack),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((step.toFloat() / total).coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(PrototypeColor.Purple),
                )
            }
        }
    }
}

/* ---------- shared card chrome ---------- */

@Composable
private fun DirectionBadge(direction: ContextDirection) {
    val enToUk = direction == ContextDirection.EnToUk
    Surface(
        shape = CircleShape,
        color = if (enToUk) PrototypeColor.Tint else PrototypeColor.Yellow.copy(alpha = 0.25f),
    ) {
        Text(
            text = if (enToUk) "EN → UK · розуміння" else "UK → EN · вживання",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (enToUk) PrototypeColor.PurpleText else PrototypeColor.NoteYellowText,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ContextBigCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 120.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = PrototypeColor.White,
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CardTagRow(direction: ContextDirection, formatLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionBadge(direction)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = formatLabel,
            color = PrototypeColor.Muted2,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

private enum class ChipState { Idle, Correct, Wrong, Dim }

@Composable
private fun AnswerChip(
    text: String,
    state: ChipState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val background = when (state) {
        ChipState.Correct -> PrototypeColor.NoteGreen
        ChipState.Wrong -> PrototypeColor.NotePeach
        else -> PrototypeColor.NeutralSurface
    }
    val textColor = when (state) {
        ChipState.Correct -> PrototypeColor.GreenText
        ChipState.Wrong -> PrototypeColor.OrangeText
        ChipState.Dim -> PrototypeColor.Muted3
        ChipState.Idle -> PrototypeColor.Ink
    }
    val borderColor = when (state) {
        ChipState.Correct -> PrototypeColor.Green
        ChipState.Wrong -> PrototypeColor.Orange
        else -> Color.Transparent
    }
    Surface(
        modifier = modifier
            .height(48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CircleShape,
        color = background,
        border = BorderStroke(1.6.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (state == ChipState.Correct || state == ChipState.Wrong) {
                PrototypeLineIcon(
                    icon = if (state == ChipState.Correct) PrototypeIcon.Check else PrototypeIcon.Close,
                    modifier = Modifier.size(15.dp),
                    color = textColor,
                    strokeWidth = 2.6f,
                )
                Spacer(modifier = Modifier.width(7.dp))
            }
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NextButton(
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = if (primary) PrototypeColor.Purple else PrototypeColor.Tint,
        shadowElevation = if (primary) 10.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Далі",
                color = if (primary) Color.White else PrototypeColor.PurpleText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp,
            )
            Spacer(modifier = Modifier.width(9.dp))
            PrototypeLineIcon(
                icon = PrototypeIcon.ChevronRight,
                modifier = Modifier.size(19.dp),
                color = if (primary) Color.White else PrototypeColor.PurpleText,
                strokeWidth = 2.4f,
            )
        }
    }
}

/* ---------- peekable sentence (board 7) ---------- */

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun PeekableSentence(
    sentence: String,
    targetWord: String,
    blankTarget: Boolean,
    cardKey: String,
    activePeek: ActivePeek?,
    freePeeksLeft: Int,
    onPeek: (word: String) -> Unit,
    onBookmark: (word: String) -> Unit,
    onBookmarkFromPeek: (word: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetNormalized = normalizePeekWord(targetWord)
    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            sentence.split(" ").forEach { token ->
                val core = normalizePeekWord(token)
                val isTarget = core.isNotBlank() &&
                    (core == targetNormalized || core.startsWith(targetNormalized))
                val wordKey = "$cardKey:$core"
                val peek = activePeek?.takeIf { it.wordKey == wordKey }
                when {
                    isTarget -> Surface(shape = RoundedCornerShape(7.dp), color = PrototypeColor.Yellow) {
                        Text(
                            text = if (blankTarget) " ____ " else token,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                            color = PrototypeColor.YellowText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                        )
                    }

                    peek != null -> PeekedWordChip(
                        state = peek.state,
                        onBookmark = { onBookmarkFromPeek(core) },
                    )

                    core.isBlank() -> Text(
                        text = token,
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                    )

                    else -> Text(
                        text = token,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .combinedClickable(
                                onClick = { onPeek(core) },
                                onLongClick = { onBookmark(core) },
                            ),
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 30.sp,
                    )
                }
            }
        }
        val showCounter = activePeek?.cardKey == cardKey &&
            activePeek.state is PeekState.Flipped &&
            (activePeek.state as PeekState.Flipped).savedTopicTitle == null
        if (showCounter && freePeeksLeft <= 2) {
            Text(
                text = if (freePeeksLeft > 0) {
                    "ще $freePeeksLeft ${ukrainianPlural(freePeeksLeft, "безкоштовне", "безкоштовні", "безкоштовних")}"
                } else {
                    "далі — 1 монетка за підглядання"
                },
                modifier = Modifier.padding(top = 8.dp),
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
            )
        }
    }
}

@Composable
private fun PeekedWordChip(
    state: PeekState,
    onBookmark: () -> Unit,
) {
    when (state) {
        is PeekState.Loading -> Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(width = 92.dp, height = 26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrototypeColor.NeutralSurface)
                .alpha(0.8f),
        )

        is PeekState.Flipped -> {
            val saved = state.savedTopicTitle != null
            val accent = if (saved) Color(0xFF0E9FA5) else PrototypeColor.PurpleText
            var appeared by remember(state) { mutableStateOf(false) }
            LaunchedEffect(state) { appeared = true }
            val rotation by animateFloatAsState(
                targetValue = if (appeared) 0f else 180f,
                animationSpec = tween(250),
                label = "peek-flip",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.graphicsLayer {
                        rotationX = rotation
                        cameraDistance = 12f * density
                    },
                    shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp),
                    color = if (saved) accent.copy(alpha = 0.14f) else PrototypeColor.Tint,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.text,
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!saved) {
                            Spacer(modifier = Modifier.width(6.dp))
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Bookmark,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(onClick = onBookmark),
                                color = accent,
                                strokeWidth = 2.2f,
                            )
                        }
                    }
                }
                if (saved) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent,
                        border = BorderStroke(1.2.dp, accent),
                    ) {
                        Text(
                            text = "у ${state.savedTopicTitle}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/* ---------- format 1: recognition ---------- */

@Composable
private fun RecognitionCardView(
    card: ContextCard.Recognition,
    activePeek: ActivePeek?,
    freePeeksLeft: Int,
    onPeek: (word: String) -> Unit,
    onBookmark: (word: String) -> Unit,
    onBookmarkFromPeek: (word: String) -> Unit,
    onAnswered: (correct: Boolean, chosenWrong: String?) -> Unit,
    onNext: () -> Unit,
) {
    var chosen by remember(card.key) { mutableStateOf<String?>(null) }
    val correctAnswer = when (card.direction) {
        ContextDirection.EnToUk -> card.pair.word.translation
        ContextDirection.UkToEn -> card.pair.word.source
    }
    val answered = chosen != null
    val answeredCorrectly = chosen == correctAnswer

    if (answered && answeredCorrectly) {
        LaunchedEffect(card.key) {
            delay(700)
            onNext()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ContextBigCard {
            CardTagRow(
                direction = card.direction,
                formatLabel = if (card.direction == ContextDirection.UkToEn) "вживання" else "впізнавання",
            )
            PeekableSentence(
                sentence = card.pair.sentence,
                targetWord = card.pair.word.source,
                blankTarget = card.direction == ContextDirection.UkToEn,
                cardKey = card.key,
                activePeek = activePeek,
                freePeeksLeft = freePeeksLeft,
                onPeek = onPeek,
                onBookmark = onBookmark,
                onBookmarkFromPeek = onBookmarkFromPeek,
                modifier = Modifier.padding(top = 18.dp),
            )
            if (card.direction == ContextDirection.UkToEn) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Підказка:",
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Surface(shape = RoundedCornerShape(7.dp), color = PrototypeColor.Yellow) {
                        Text(
                            text = card.pair.word.translation,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                            color = PrototypeColor.YellowText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            Text(
                text = if (card.direction == ContextDirection.UkToEn) "Яке слово тут пасує?" else "Що означає тут?",
                modifier = Modifier.padding(top = 10.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            card.options.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowOptions.forEach { option ->
                        val state = when {
                            !answered -> ChipState.Idle
                            option == correctAnswer -> ChipState.Correct
                            option == chosen -> ChipState.Wrong
                            else -> ChipState.Dim
                        }
                        AnswerChip(
                            text = option,
                            state = state,
                            modifier = Modifier.weight(1f),
                            onClick = if (!answered) {
                                {
                                    chosen = option
                                    onAnswered(option == correctAnswer, option.takeIf { it != correctAnswer })
                                }
                            } else {
                                null
                            },
                        )
                    }
                    if (rowOptions.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (answered && !answeredCorrectly && card.direction == ContextDirection.EnToUk) {
                val confusedSentence = chosen?.let { card.pair.sentenceByTranslation[it] }
                if (confusedSentence != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = PrototypeColor.NotePeach,
                        border = BorderStroke(1.dp, PrototypeColor.Orange.copy(alpha = 0.35f)),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                            Text(
                                text = "ПІДКАЗКА",
                                color = PrototypeColor.OrangeText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                            )
                            Text(
                                text = "«$chosen» — це коли: $confusedSentence",
                                modifier = Modifier.padding(top = 5.dp),
                                color = PrototypeColor.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }
            if (answered && answeredCorrectly) {
                val knowledge = card.pair.word.knowledgePercent
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Surface(shape = CircleShape, color = PrototypeColor.NoteGreen) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Check,
                                modifier = Modifier.size(15.dp),
                                color = PrototypeColor.GreenText,
                                strokeWidth = 2.6f,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${card.pair.word.source} = $correctAnswer · $knowledge% → ${(knowledge + KnowledgeStepPercent).coerceAtMost(100)}%",
                                color = PrototypeColor.GreenText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
        if (answered && !answeredCorrectly) {
            NextButton(
                primary = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
                onClick = onNext,
            )
        }
    }
}

/* ---------- format 2: recall (flip) ---------- */

@Composable
private fun RecallCardView(
    card: ContextCard.Recall,
    activePeek: ActivePeek?,
    freePeeksLeft: Int,
    onPeek: (word: String) -> Unit,
    onBookmark: (word: String) -> Unit,
    onBookmarkFromPeek: (word: String) -> Unit,
    onAnswered: (correct: Boolean) -> Unit,
    onNext: () -> Unit,
) {
    var flipped by remember(card.key) { mutableStateOf(false) }
    var missed by remember(card.key) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ContextBigCard {
            CardTagRow(direction = ContextDirection.EnToUk, formatLabel = "згадування")
            if (!flipped) {
                PeekableSentence(
                    sentence = card.pair.sentence,
                    targetWord = card.pair.word.source,
                    blankTarget = false,
                    cardKey = card.key,
                    activePeek = activePeek,
                    freePeeksLeft = freePeeksLeft,
                    onPeek = onPeek,
                    onBookmark = onBookmark,
                    onBookmarkFromPeek = onBookmarkFromPeek,
                    modifier = Modifier.padding(top = 18.dp),
                )
                Text(
                    text = "Як перекладається тут?",
                    modifier = Modifier.padding(top = 10.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Торкнись, щоб перевернути",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { flipped = true }
                        .padding(top = 34.dp, bottom = 12.dp),
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = highlightedSentence(card.pair.sentence, card.pair.word.source),
                    modifier = Modifier.padding(top = 14.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp,
                )
                Text(
                    text = card.pair.word.translation,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                    letterSpacing = (-0.8).sp,
                    textAlign = TextAlign.Center,
                )
                val others = card.pair.groupTranslations.filter { it != card.pair.word.translation }
                if (others.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        others.take(3).forEach { other ->
                            Surface(
                                modifier = Modifier.padding(horizontal = 3.5.dp),
                                shape = CircleShape,
                                color = PrototypeColor.NeutralSurface,
                            ) {
                                Text(
                                    text = other,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    color = PrototypeColor.Muted3,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                    Text(
                        text = "інші значення ${card.pair.word.source} — тренуються окремо",
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (missed) {
                NextButton(primary = false, modifier = Modifier.weight(1f), onClick = onNext)
            } else {
                PracticeAnswerButton(
                    text = "Не знаю",
                    icon = PrototypeIcon.Close,
                    color = PrototypeColor.OrangeText,
                    background = PrototypeColor.NotePeach,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAnswered(false)
                        flipped = true
                        missed = true
                    },
                )
                PracticeAnswerButton(
                    text = "Знаю",
                    icon = PrototypeIcon.Check,
                    color = PrototypeColor.GreenText,
                    background = PrototypeColor.NoteGreen,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAnswered(true)
                        onNext()
                    },
                )
            }
        }
    }
}

/* ---------- format 3: matching ---------- */

private val MatchingPalette = listOf(
    Color(0xFF4F46E5),
    Color(0xFF0E9FA5),
    Color(0xFF7C5CF6),
    Color(0xFFE0820C),
)

@Composable
private fun MatchingCardView(
    card: ContextCard.Matching,
    onPairResolved: (pair: ContextPair, firstTryCorrect: Boolean, chosenWrong: String?) -> Unit,
    onAllResolved: (allFirstTry: Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val translations = remember(card.key) { card.pairs.map { it.word.translation }.shuffled(Random(card.key.hashCode())) }
    var selectedSentence by remember(card.key) { mutableStateOf<Int?>(null) }
    val connections = remember(card.key) { mutableStateMapOf<Int, Int>() }
    val missedSentences = remember(card.key) { mutableStateMapOf<Int, Boolean>() }
    var wrongFlash by remember(card.key) { mutableStateOf<Int?>(null) }
    var completionReported by remember(card.key) { mutableStateOf(false) }

    val allConnected = connections.size == card.pairs.size
    if (allConnected && !completionReported) {
        completionReported = true
        onAllResolved(missedSentences.values.none { it })
    }
    if (wrongFlash != null) {
        LaunchedEffect(wrongFlash) {
            delay(600)
            wrongFlash = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ContextBigCard {
            CardTagRow(direction = ContextDirection.EnToUk, formatLabel = "метчинг · фінал слова")
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.source,
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                )
                card.ipa?.takeIf { it.isNotBlank() }?.let { ipa ->
                    Text(
                        text = ipa,
                        modifier = Modifier.padding(start = 8.dp),
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
            Text(
                text = "З’єднай речення з перекладом",
                modifier = Modifier.padding(top = 4.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Column(
                    modifier = Modifier.weight(1.4f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    card.pairs.forEachIndexed { index, pair ->
                        val connectionColor = connections[index]?.let { MatchingPalette[it % MatchingPalette.size] }
                        val selected = selectedSentence == index
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = connections[index] == null) { selectedSentence = index },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) PrototypeColor.Tint else PrototypeColor.White,
                            border = BorderStroke(
                                width = if (selected || connectionColor != null) 1.6.dp else 1.dp,
                                color = when {
                                    selected -> PrototypeColor.Purple
                                    connectionColor != null -> connectionColor
                                    else -> PrototypeColor.Line
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(connectionColor ?: PrototypeColor.ProgressTrack),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                    )
                                }
                                Text(
                                    text = pair.sentence,
                                    modifier = Modifier.padding(start = 9.dp),
                                    color = PrototypeColor.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    translations.forEachIndexed { tIndex, translation ->
                        val connectedSentence = connections.entries.firstOrNull { (sentenceIdx, _) ->
                            card.pairs[sentenceIdx].word.translation == translation
                        }?.key
                        val connectionColor = connectedSentence?.let { s ->
                            connections[s]?.let { MatchingPalette[it % MatchingPalette.size] }
                        }
                        val flashWrong = wrongFlash == tIndex
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable(enabled = connectionColor == null && selectedSentence != null) {
                                    val sentenceIdx = selectedSentence ?: return@clickable
                                    val pair = card.pairs[sentenceIdx]
                                    if (pair.word.translation == translation) {
                                        val firstTry = missedSentences[sentenceIdx] != true
                                        connections[sentenceIdx] = connections.size
                                        selectedSentence = null
                                        onPairResolved(pair, firstTry, null)
                                    } else {
                                        if (missedSentences[sentenceIdx] != true) {
                                            missedSentences[sentenceIdx] = true
                                            onPairResolved(pair, false, translation)
                                        }
                                        wrongFlash = tIndex
                                    }
                                },
                            shape = CircleShape,
                            color = when {
                                flashWrong -> PrototypeColor.NotePeach
                                connectionColor != null -> connectionColor.copy(alpha = 0.14f)
                                else -> PrototypeColor.NeutralSurface
                            },
                            border = BorderStroke(
                                1.6.dp,
                                when {
                                    flashWrong -> PrototypeColor.Orange
                                    connectionColor != null -> connectionColor
                                    else -> Color.Transparent
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                if (connectionColor != null && connectedSentence != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(connectionColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "${connectedSentence + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.5.sp,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = translation,
                                    color = when {
                                        flashWrong -> PrototypeColor.OrangeText
                                        connectionColor != null -> connectionColor
                                        else -> PrototypeColor.Ink
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = if (allConnected) {
                    "${card.pairs.size} / ${card.pairs.size} з’єднано"
                } else {
                    "${connections.size} / ${card.pairs.size} з’єднано" +
                        (selectedSentence?.let { " · обери переклад для №${it + 1}" }
                            ?: " · обери речення")
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
        }
        if (allConnected) {
            NextButton(
                primary = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
                onClick = onNext,
            )
        }
    }
}

/* ---------- result ---------- */

@Composable
private fun ContextDoneState(
    correctAnswers: Int,
    total: Int,
    confusions: List<ConfusionEntry>,
    bookmarks: List<PeekBookmark>,
    onOpenBookmark: (topicId: String, word: String) -> Unit,
    onRestart: () -> Unit,
    onChooseTopics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = if (total == 0) 0 else (correctAnswers * 100 / total)
    val topConfusion = confusions
        .groupBy { it }
        .maxByOrNull { (_, entries) -> entries.size }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(26.dp))
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = 12.dp.toPx()
                drawCircle(
                    color = PrototypeColor.ProgressRing,
                    radius = size.minDimension / 2f - stroke / 2f,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = PrototypeColor.Purple,
                    startAngle = -90f,
                    sweepAngle = 360f * (percent / 100f),
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                text = "$percent%",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.PurpleText,
                letterSpacing = (-0.7).sp,
            )
        }
        Text(
            text = "Раунд завершено",
            modifier = Modifier.padding(top = 18.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
        )
        Text(
            text = "Правильних відповідей: $correctAnswers із $total.",
            modifier = Modifier.padding(top = 8.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
        )

        if (topConfusion != null) {
            val entry = topConfusion.key
            val count = topConfusion.value.size
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
                shape = RoundedCornerShape(18.dp),
                color = PrototypeColor.White,
                border = BorderStroke(1.dp, PrototypeColor.Orange.copy(alpha = 0.35f)),
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "НАЙПЛУТАНІША ПАРА",
                        color = PrototypeColor.OrangeText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ConfusionCell(
                            title = "${entry.source} = ${entry.correctTranslation}",
                            subtitle = "$count ${ukrainianPlural(count, "помилка", "помилки", "помилок")}",
                            background = PrototypeColor.NotePeach,
                            subtitleColor = PrototypeColor.OrangeText,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "⇄",
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = PrototypeColor.Muted2,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                        )
                        ConfusionCell(
                            title = "${entry.source} = ${entry.chosenTranslation}",
                            subtitle = "обирав замість",
                            background = PrototypeColor.NeutralSurface,
                            subtitleColor = PrototypeColor.Muted,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = "Ти плутаєш «${entry.chosenTranslation}» там, де ${entry.source} означає «${entry.correctTranslation}». Ці пари прийдуть у наступному раунді першими.",
                        modifier = Modifier.padding(top = 11.dp),
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        if (bookmarks.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = PrototypeColor.White,
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "НОВІ СЛОВА З РЕЧЕНЬ (${bookmarks.size})",
                            modifier = Modifier.weight(1f),
                            color = PrototypeColor.Muted2,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.6.sp,
                        )
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Bookmark,
                            modifier = Modifier.size(15.dp),
                            color = PrototypeColor.NoteYellowText,
                            strokeWidth = 2.2f,
                        )
                    }
                    bookmarks.forEachIndexed { bookmarkIndex, bookmark ->
                        if (bookmarkIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(PrototypeColor.DividerLight),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenBookmark(bookmark.topicId, bookmark.word) }
                                .padding(vertical = 12.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = PrototypeColor.Yellow.copy(alpha = 0.25f),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PrototypeLineIcon(
                                        icon = PrototypeIcon.Bookmark,
                                        modifier = Modifier.size(16.dp),
                                        color = PrototypeColor.NoteYellowText,
                                        strokeWidth = 2.2f,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 11.dp, end = 8.dp)) {
                                Text(
                                    text = bookmark.word,
                                    color = PrototypeColor.Ink,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.5.sp,
                                )
                                Text(
                                    text = "з речення: “${bookmark.sentence.take(38)}…”",
                                    modifier = Modifier.padding(top = 1.dp),
                                    color = PrototypeColor.Muted,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            PrototypeLineIcon(
                                icon = PrototypeIcon.ChevronRight,
                                modifier = Modifier.size(17.dp),
                                color = PrototypeColor.Muted3,
                                strokeWidth = 2.2f,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(48.dp)
                            .clickable {
                                bookmarks.firstOrNull()?.let { onOpenBookmark(it.topicId, it.word) }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = PrototypeColor.Tint,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Розібрати всі (${bookmarks.size})",
                                color = PrototypeColor.PurpleText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryPillButton(label = "Ще раунд", onClick = onRestart)
        Surface(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 28.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clickable(onClick = onChooseTopics),
            shape = RoundedCornerShape(16.dp),
            color = PrototypeColor.NeutralSurface,
            border = BorderStroke(1.4.dp, PrototypeColor.Line),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Обрати теми",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun ConfusionCell(
    title: String,
    subtitle: String,
    background: Color,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                color = subtitleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
