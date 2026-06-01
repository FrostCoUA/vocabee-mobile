package com.vocabee.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.TopicUpdatedLabel
import com.vocabee.android.domain.model.WordEntry
import com.vocabee.android.navigation.AppTab
import com.vocabee.android.navigation.VocabeeRoute
import com.vocabee.android.navigation.selectedTabFor
import com.vocabee.android.navigation.vocabeeSavedStateConfiguration
import com.vocabee.android.platform.NoSpeechInputController
import com.vocabee.android.platform.SpeechInputController
import com.vocabee.android.presentation.AddWordOrigin
import com.vocabee.android.presentation.AddWordOverlay
import com.vocabee.android.presentation.AuthScreen
import com.vocabee.android.presentation.CreateDictionarySheet
import com.vocabee.android.presentation.HoneycombWatermark
import com.vocabee.android.presentation.LanguageSelectScreen
import com.vocabee.android.presentation.LanguageSheet
import com.vocabee.android.presentation.OnboardingScreen
import com.vocabee.android.presentation.PrimaryPillButton
import com.vocabee.android.presentation.PrototypeColor
import com.vocabee.android.presentation.PrototypeIcon
import com.vocabee.android.presentation.PrototypeLineIcon
import com.vocabee.android.presentation.PrototypeLogo
import com.vocabee.android.presentation.SplashScreen
import com.vocabee.android.presentation.VocabeeEvent
import com.vocabee.android.presentation.VocabeeStore
import com.vocabee.android.presentation.languageFlag
import com.vocabee.android.presentation.languageName
import com.vocabee.android.presentation.prototypeTopicTheme

internal enum class AppFlow { Splash, Onboarding, Auth, LanguageSelect, Main }

internal sealed interface PrototypeSheet {
    data object CreateDictionary : PrototypeSheet
    data class LanguageForDictionary(val dictionaryId: String) : PrototypeSheet
    data class LanguageForProfile(val target: ProfileLanguageTarget) : PrototypeSheet
}

internal enum class ProfileLanguageTarget { Speaking, Learning }

@Composable
fun VocabeeApp(
    store: VocabeeStore = VocabeeStore(),
    speechInputController: SpeechInputController = NoSpeechInputController,
) {
    VocabeeTheme {
        var flow by remember { mutableStateOf(AppFlow.Splash) }
        val state = store.state

        when (flow) {
            AppFlow.Splash -> SplashScreen(onDone = { flow = AppFlow.Onboarding })
            AppFlow.Onboarding -> OnboardingScreen(onDone = { flow = AppFlow.Auth })
            AppFlow.Auth -> AuthScreen(onDone = { flow = AppFlow.LanguageSelect })
            AppFlow.LanguageSelect -> LanguageSelectScreen(
                supportedLanguages = state.supportedLanguages,
                initialSpeak = state.userLanguage.code,
                initialLearn = state.learningLanguage.code,
                onDone = { speakCode, learnCode ->
                    state.supportedLanguages.firstOrNull { it.code == speakCode }?.let {
                        store.onEvent(VocabeeEvent.SelectSpeakingLanguage(it))
                    }
                    state.supportedLanguages.firstOrNull { it.code == learnCode }?.let {
                        store.onEvent(VocabeeEvent.SelectLearningLanguage(it))
                    }
                    flow = AppFlow.Main
                },
            )
            AppFlow.Main -> MainApp(
                store = store,
                speechInputController = speechInputController,
            )
        }
    }
}

@Composable
private fun MainApp(
    store: VocabeeStore,
    speechInputController: SpeechInputController,
) {
    val state = store.state
    val backStack = rememberNavBackStack(
        vocabeeSavedStateConfiguration,
        VocabeeRoute.DictionaryHome,
    )
    val currentRoute = backStack.lastOrNull() as? VocabeeRoute
    val selectedTab = selectedTabFor(currentRoute)
    val showBottomBar = AppTab.entries.any { tab -> tab.route == currentRoute }

    var sheet by remember { mutableStateOf<PrototypeSheet?>(null) }
    var addWordOrigin by remember { mutableStateOf<AddWordOrigin?>(null) }
    var addWordTopicId by remember { mutableStateOf<String?>(null) }
    val addWordTopic = addWordTopicId?.let { id -> state.topics.firstOrNull { it.id == id } }

    fun openRoot(route: VocabeeRoute) {
        backStack.clear()
        backStack.add(route)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar && addWordOrigin == null) {
                VocabeeBottomBar(
                    selectedTab = selectedTab,
                    onTabClick = { tab -> openRoot(tab.route) },
                )
            }
        },
        containerColor = PrototypeColor.White,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) backStack.removeLastOrNull()
                },
                entryProvider = entryProvider {
                    entry<VocabeeRoute.DictionaryHome> {
                        DictionariesHomeScreen(
                            topics = state.topics,
                            onCreateClick = { sheet = PrototypeSheet.CreateDictionary },
                            onTopicClick = { topicId ->
                                backStack.add(VocabeeRoute.TopicDetail(topicId))
                            },
                        )
                    }

                    entry<VocabeeRoute.TopicDetail> { route ->
                        val topic = state.topics.firstOrNull { it.id == route.topicId }
                        if (topic == null) {
                            MissingTopicScreen(onBack = { backStack.removeLastOrNull() })
                        } else {
                            DictionaryDetailScreen(
                                topic = topic,
                                recentlyAddedWordId = state.recentlyAddedWordId,
                                onBack = { backStack.removeLastOrNull() },
                                onOpenLanguageSheet = {
                                    sheet = PrototypeSheet.LanguageForDictionary(topic.id)
                                },
                                onAddWordPill = { origin ->
                                    addWordOrigin = origin
                                    addWordTopicId = topic.id
                                },
                            )
                        }
                    }

                    entry<VocabeeRoute.Practice> { PracticeScreen(topics = state.topics) }

                    entry<VocabeeRoute.Settings> {
                        ProfileScreen(
                            topics = state.topics,
                            userLanguage = state.userLanguage,
                            learningLanguage = state.learningLanguage,
                            notificationsEnabled = state.notificationsEnabled,
                            darkThemeEnabled = state.darkThemeEnabled,
                            onNotificationsChanged = { store.onEvent(VocabeeEvent.SetNotificationsEnabled(it)) },
                            onDarkThemeChanged = { store.onEvent(VocabeeEvent.SetDarkThemeEnabled(it)) },
                            onSpeakingClick = { sheet = PrototypeSheet.LanguageForProfile(ProfileLanguageTarget.Speaking) },
                            onLearningClick = { sheet = PrototypeSheet.LanguageForProfile(ProfileLanguageTarget.Learning) },
                        )
                    }
                },
            )

            // Add Word full-screen overlay (above tab bar)
            val origin = addWordOrigin
            val topic = addWordTopic
            if (origin != null && topic != null) {
                AddWordOverlay(
                    topic = topic,
                    accent = prototypeTopicTheme(topic.coverIndex).color,
                    origin = origin,
                    speechInputController = speechInputController,
                    suggestionsFor = { input -> store.translationOptionsFor(topic, input) },
                    onRequestMachineTranslation = { input ->
                        store.onEvent(VocabeeEvent.RequestMachineTranslation(topic.id, input))
                    },
                    onAddWord = { source, translation ->
                        store.onEvent(VocabeeEvent.AddWord(topic.id, source, translation))
                    },
                    onOpenLanguageSheet = {
                        sheet = PrototypeSheet.LanguageForDictionary(topic.id)
                    },
                    onClose = {
                        addWordOrigin = null
                        addWordTopicId = null
                    },
                )
            }
        }

        when (val s = sheet) {
            null -> Unit
            PrototypeSheet.CreateDictionary -> CreateDictionarySheet(
                sourceLanguageCode = state.learningLanguage.code,
                targetLanguageCode = state.userLanguage.code,
                existingDictionariesCount = state.topics.size,
                onDismiss = { sheet = null },
                onCreate = { title, coverIndex ->
                    store.onEvent(VocabeeEvent.CreateTopic(title = title, coverIndex = coverIndex))
                    sheet = null
                    val createdTopic = store.state.topics.lastOrNull()
                    if (createdTopic != null) {
                        backStack.add(VocabeeRoute.TopicDetail(createdTopic.id))
                    }
                },
            )
            is PrototypeSheet.LanguageForDictionary -> {
                val dict = state.topics.firstOrNull { it.id == s.dictionaryId }
                if (dict != null) {
                    LanguageSheet(
                        title = "Мова словника",
                        subtitle = "Лише для цього словника. За замовчуванням використовується мова з профілю.",
                        selectedCode = dict.sourceLanguage.code,
                        excludeCode = dict.targetLanguage.code,
                        onDismiss = { sheet = null },
                        onPick = { _ ->
                            // Per-dictionary language override is a future feature;
                            // for now just dismiss — the spec marks this as de-emphasized.
                            sheet = null
                        },
                    )
                }
            }
            is PrototypeSheet.LanguageForProfile -> {
                val target = s.target
                val current = if (target == ProfileLanguageTarget.Speaking) state.userLanguage else state.learningLanguage
                val exclude = if (target == ProfileLanguageTarget.Speaking) state.learningLanguage else state.userLanguage
                LanguageSheet(
                    title = if (target == ProfileLanguageTarget.Speaking) "Я розмовляю" else "Я вивчаю",
                    subtitle = null,
                    selectedCode = current.code,
                    excludeCode = exclude.code,
                    onDismiss = { sheet = null },
                    onPick = { code ->
                        state.supportedLanguages.firstOrNull { it.code == code }?.let { picked ->
                            when (target) {
                                ProfileLanguageTarget.Speaking -> store.onEvent(VocabeeEvent.SelectSpeakingLanguage(picked))
                                ProfileLanguageTarget.Learning -> store.onEvent(VocabeeEvent.SelectLearningLanguage(picked))
                            }
                        }
                        sheet = null
                    },
                )
            }
        }
    }
}

@Composable
private fun VocabeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrototypeColor.Purple,
            onPrimary = PrototypeColor.White,
            secondary = PrototypeColor.Blue,
            onSecondary = PrototypeColor.White,
            tertiary = PrototypeColor.Yellow,
            onTertiary = PrototypeColor.Ink,
            background = PrototypeColor.Background,
            onBackground = PrototypeColor.Ink,
            surface = PrototypeColor.White,
            onSurface = PrototypeColor.Ink,
            surfaceVariant = PrototypeColor.Line2,
            onSurfaceVariant = PrototypeColor.Muted,
            outline = PrototypeColor.Line,
        ),
        content = content,
    )
}

/* ============================================================
 * Dictionaries home
 * ============================================================ */

@Composable
private fun DictionariesHomeScreen(
    topics: List<DictionaryTopic>,
    onCreateClick: () -> Unit,
    onTopicClick: (String) -> Unit,
) {
    val totalWords = topics.sumOf { it.words.size }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.White),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHeader(topicCount = topics.size, totalWords = totalWords)
            }

            if (topics.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyHomeState(
                        modifier = Modifier.fillMaxWidth().height(470.dp),
                        onCreateClick = onCreateClick,
                    )
                }
            } else {
                itemsIndexed(
                    items = topics.reversed(),
                    key = { _, topic -> topic.id },
                ) { _, topic ->
                    DictionaryCard(
                        topic = topic,
                        onClick = { onTopicClick(topic.id) },
                    )
                }
            }
        }

        if (topics.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(62.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PrototypeColor.Purple)
                    .clickable(onClick = onCreateClick),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Plus,
                    modifier = Modifier.size(26.dp),
                    color = PrototypeColor.White,
                    strokeWidth = 2.4f,
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(topicCount: Int, totalWords: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "Привіт, Надіє 👋",
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                Text(
                    text = "Словники",
                    modifier = Modifier.padding(top = 3.dp),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrototypeColor.Ink,
                    letterSpacing = (-1.02).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PrototypeLogo(modifier = Modifier.size(30.dp))
        }

        if (topicCount > 0) {
            Row(
                modifier = Modifier.padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                MetricText(value = topicCount.toString(), unit = if (topicCount == 1) "словник" else "словники")
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(PrototypeColor.Muted2))
                MetricText(value = totalWords.toString(), unit = "слів зібрано")
            }
        }
    }
}

@Composable
private fun MetricText(value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Text(
            text = " $unit",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun DictionaryCard(topic: DictionaryTopic, onClick: () -> Unit) {
    val theme = prototypeTopicTheme(topic.coverIndex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(theme.color)
            .clickable(onClick = onClick),
    ) {
        HoneycombWatermark(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = (-26).dp)
                .size(120.dp),
        )

        Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
            if (topic.updatedLabel is TopicUpdatedLabel.Today) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart),
                    shape = CircleShape,
                    color = PrototypeColor.Yellow,
                ) {
                    Text(
                        text = "сьогодні",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = PrototypeColor.YellowText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                    )
                }
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                Text(
                    text = topic.title,
                    color = PrototypeColor.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.5.sp,
                    lineHeight = 21.sp,
                    letterSpacing = (-0.175).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PrototypeColor.White.copy(alpha = 0.22f),
                    ) {
                        Text(
                            text = "${topic.words.size} слів",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = PrototypeColor.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        )
                    }
                    if (topic.updatedLabel !is TopicUpdatedLabel.Today) {
                        Text(
                            text = updatedLabelText(topic.updatedLabel),
                            color = PrototypeColor.White.copy(alpha = 0.82f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun updatedLabelText(label: TopicUpdatedLabel): String = when (label) {
    TopicUpdatedLabel.Today -> "сьогодні"
    TopicUpdatedLabel.Yesterday -> "вчора"
    is TopicUpdatedLabel.DaysAgo -> "${label.count} дні тому"
    is TopicUpdatedLabel.WeeksAgo -> "${label.count} тижні тому"
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyDictionariesIllustration(modifier = Modifier.size(width = 190.dp, height = 160.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Поки що порожньо",
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            letterSpacing = (-0.21).sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Створи свій перший тематичний словник — і починай збирати слова.",
            modifier = Modifier.padding(top = 9.dp).widthIn(max = 280.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .height(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PrototypeColor.Purple)
                .clickable(onClick = onCreateClick)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Plus,
                modifier = Modifier.size(19.dp),
                color = PrototypeColor.White,
                strokeWidth = 2.2f,
            )
            Text(
                text = "Створити словник",
                color = PrototypeColor.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun EmptyDictionariesIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 200f
        val sy = size.height / 170f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
        fun rect(left: Float, top: Float, w: Float, h: Float, r: Float, color: Color, stroke: Color? = null) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x(left), y(top)),
                size = Size(x(w), y(h)),
                cornerRadius = CornerRadius(x(r), y(r)),
            )
            if (stroke != null) {
                drawRoundRect(
                    color = stroke,
                    topLeft = Offset(x(left), y(top)),
                    size = Size(x(w), y(h)),
                    cornerRadius = CornerRadius(x(r), y(r)),
                    style = Stroke(width = x(1.5f)),
                )
            }
        }

        rect(44f, 92f, 112f, 40f, 13f, PrototypeColor.EmptyCardLight)
        rect(36f, 60f, 128f, 42f, 13f, PrototypeColor.Tint)
        rect(28f, 26f, 144f, 46f, 14f, PrototypeColor.White, PrototypeColor.EmptyCardStroke)
        drawCircle(PrototypeColor.Yellow, radius = x(8f), center = Offset(x(150f), y(49f)))
        drawRoundRect(
            color = PrototypeColor.EmptyCardTextDark,
            topLeft = Offset(x(74f), y(42f)),
            size = Size(x(64f), y(8f)),
            cornerRadius = CornerRadius(x(4f), y(4f)),
        )
        drawRoundRect(
            color = PrototypeColor.EmptyCardTextLight,
            topLeft = Offset(x(74f), y(55f)),
            size = Size(x(40f), y(7f)),
            cornerRadius = CornerRadius(x(3.5f), y(3.5f)),
        )
        val hexPts = listOf(
            Offset(12f, 0f), Offset(6f, 10f), Offset(-6f, 10f),
            Offset(-12f, 0f), Offset(-6f, -10f), Offset(6f, -10f),
        )
        val hexPath = Path().apply {
            val cx = x(52f)
            val cy = y(49f)
            moveTo(cx + x(hexPts.first().x), cy + y(hexPts.first().y))
            hexPts.drop(1).forEach { p -> lineTo(cx + x(p.x), cy + y(p.y)) }
            close()
        }
        drawPath(hexPath, PrototypeColor.EmptyCardHex)
    }
}

/* ============================================================
 * Dictionary detail
 * ============================================================ */

@Composable
private fun DictionaryDetailScreen(
    topic: DictionaryTopic,
    recentlyAddedWordId: String?,
    onBack: () -> Unit,
    onOpenLanguageSheet: () -> Unit,
    onAddWordPill: (AddWordOrigin) -> Unit,
) {
    val accent = prototypeTopicTheme(topic.coverIndex).color
    var pillOrigin by remember { mutableStateOf<AddWordOrigin?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DetailHeader(
                    topic = topic,
                    accent = accent,
                    onBack = onBack,
                    onOpenLanguage = onOpenLanguageSheet,
                )
            }
            if (topic.words.isEmpty()) {
                item {
                    DetailEmptyState(
                        accent = accent,
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                    )
                }
            } else {
                items(topic.words, key = { it.id }) { word ->
                    WordRow(
                        word = word,
                        accent = accent,
                        highlighted = word.id == recentlyAddedWordId,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        // Add Word pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(accent)
                .clickable {
                    val origin = pillOrigin
                    if (origin != null) onAddWordPill(origin)
                }
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInRoot()
                    val sz = coords.size
                    pillOrigin = AddWordOrigin(
                        left = with(density) { pos.x.toDp() },
                        top = with(density) { pos.y.toDp() },
                        width = with(density) { sz.width.toDp() },
                        height = with(density) { sz.height.toDp() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Plus,
                    modifier = Modifier.size(21.dp),
                    color = PrototypeColor.White,
                    strokeWidth = 2.4f,
                )
                Text(
                    text = "Додати слово",
                    color = PrototypeColor.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(
    topic: DictionaryTopic,
    accent: Color,
    onBack: () -> Unit,
    onOpenLanguage: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accent,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    ) {
        Box {
            HoneycombWatermark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
                    .size(120.dp),
                color = PrototypeColor.White.copy(alpha = 0.18f),
            )
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 2.dp, end = 18.dp, bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                        shape = RoundedCornerShape(13.dp),
                        color = PrototypeColor.White.copy(alpha = 0.18f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.ChevronLeft,
                                modifier = Modifier.size(22.dp),
                                color = PrototypeColor.White,
                                strokeWidth = 2.2f,
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = PrototypeColor.White.copy(alpha = 0.16f),
                        modifier = Modifier.clickable(onClick = onOpenLanguage),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(languageFlag(topic.sourceLanguage.code), fontSize = 15.sp)
                            PrototypeLineIcon(
                                icon = PrototypeIcon.ArrowRight,
                                modifier = Modifier.size(13.dp),
                                color = PrototypeColor.White.copy(alpha = 0.8f),
                                strokeWidth = 2f,
                            )
                            Text(languageFlag(topic.targetLanguage.code), fontSize = 15.sp)
                            PrototypeLineIcon(
                                icon = PrototypeIcon.ChevronDown,
                                modifier = Modifier.size(14.dp),
                                color = PrototypeColor.White.copy(alpha = 0.8f),
                                strokeWidth = 2.2f,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = topic.title,
                    color = PrototypeColor.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.56).sp,
                    lineHeight = 32.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${topic.words.size} слів · ${updatedLabelText(topic.updatedLabel)}",
                    modifier = Modifier.padding(top = 9.dp),
                    color = PrototypeColor.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
            }
        }
    }
}

@Composable
private fun WordRow(
    word: WordEntry,
    accent: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    var open by remember(word.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().clickable { open = !open },
        shape = RoundedCornerShape(18.dp),
        color = PrototypeColor.White,
        shadowElevation = 2.dp,
        border = if (highlighted) BorderStroke(1.dp, PrototypeColor.Yellow) else null,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            text = word.source,
                            color = PrototypeColor.Ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.18).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "/${word.source.lowercase()}/",
                            color = PrototypeColor.Muted2,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = word.translation,
                        modifier = Modifier.padding(top = 3.dp),
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = PrototypeColor.Tint,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Sound,
                            modifier = Modifier.size(17.dp),
                            color = PrototypeColor.Purple,
                            strokeWidth = 1.9f,
                        )
                    }
                }
                Box(
                    modifier = Modifier.width(30.dp).height(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.ChevronDown,
                        modifier = Modifier.size(18.dp),
                        color = accent,
                        strokeWidth = 2f,
                    )
                }
            }
            if (open) {
                Column(
                    modifier = Modifier
                        .padding(start = 13.dp, end = 13.dp, bottom = 13.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            Brush.verticalGradient(listOf(PrototypeColor.ContextCardTop, PrototypeColor.ContextCardBottom))
                        )
                        .border(BorderStroke(1.dp, PrototypeColor.ContextCardBorder), RoundedCornerShape(13.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    Surface(shape = CircleShape, color = PrototypeColor.Tint) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Sparkle,
                                modifier = Modifier.size(12.dp),
                                color = PrototypeColor.Purple,
                                strokeWidth = 1.7f,
                            )
                            Text(
                                text = "AI приклад",
                                color = PrototypeColor.Purple,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                letterSpacing = 0.33.sp,
                            )
                        }
                    }
                    Text(
                        text = aiExample(word.source),
                        modifier = Modifier.padding(top = 9.dp),
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                    )
                    Text(
                        text = "Короткий приклад використання слова «${word.translation}».",
                        modifier = Modifier.padding(top = 4.dp),
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
    }
}

private fun aiExample(word: String): String = "Use $word in a short sentence."

@Composable
private fun DetailEmptyState(accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyWordsIllustration(modifier = Modifier.size(width = 170.dp, height = 130.dp))
        Text(
            text = "Ще немає слів",
            modifier = Modifier.padding(top = 18.dp),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            letterSpacing = (-0.21).sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Натисни «+ Додати слово» нижче — введи або продиктуй слово, а решту підкаже AI.",
            modifier = Modifier.padding(top = 9.dp).widthIn(max = 280.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        PrototypeLineIcon(
            icon = PrototypeIcon.ArrowRight,
            modifier = Modifier.size(34.dp),
            color = accent.copy(alpha = 0.72f),
            strokeWidth = 2.2f,
        )
    }
}

@Composable
private fun EmptyWordsIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val sx = size.width / 200f
        val sy = size.height / 150f
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
        drawRoundRect(
            color = PrototypeColor.EmptyCardSoft,
            topLeft = Offset(x(40f), y(34f)),
            size = Size(x(120f), y(84f)),
            cornerRadius = CornerRadius(x(14f), y(14f)),
        )
        drawRoundRect(
            color = PrototypeColor.EmptyCardStroke2,
            topLeft = Offset(x(40f), y(34f)),
            size = Size(x(120f), y(84f)),
            cornerRadius = CornerRadius(x(14f), y(14f)),
            style = Stroke(width = x(1.5f)),
        )
        listOf(
            Triple(58f, 56f, 62f) to 8f,
            Triple(58f, 72f, 84f) to 8f,
            Triple(58f, 88f, 48f) to 8f,
        ).forEachIndexed { idx, (rect, h) ->
            drawRoundRect(
                color = if (idx == 0) PrototypeColor.EmptyCardWordDark else PrototypeColor.EmptyCardWordLight,
                topLeft = Offset(x(rect.first), y(rect.second)),
                size = Size(x(rect.third), y(h)),
                cornerRadius = CornerRadius(x(4f), y(4f)),
            )
        }
    }
}

/* ============================================================
 * Bottom bar
 * ============================================================ */

@Composable
private fun VocabeeBottomBar(
    selectedTab: AppTab,
    onTabClick: (AppTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrototypeColor.White)
            .border(BorderStroke(1.dp, PrototypeColor.Line), RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .height(66.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTab.entries.forEach { tab ->
                BottomTabButton(
                    tab = tab,
                    selected = selectedTab == tab,
                    modifier = Modifier.weight(1f),
                    onClick = { onTabClick(tab) },
                )
            }
        }
    }
}

@Composable
private fun BottomTabButton(
    tab: AppTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) PrototypeColor.Purple else PrototypeColor.Muted2
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PrototypeLineIcon(
            icon = tab.prototypeIcon,
            modifier = Modifier.size(24.dp),
            color = color,
            strokeWidth = if (selected) 2.1f else 1.8f,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = bottomBarLabel(tab),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

private fun bottomBarLabel(tab: AppTab): String = when (tab) {
    AppTab.Dictionary -> "Словники"
    AppTab.Practice -> "Тренування"
    AppTab.Settings -> "Профіль"
}

private val AppTab.prototypeIcon: PrototypeIcon
    get() = when (this) {
        AppTab.Dictionary -> PrototypeIcon.Book
        AppTab.Practice -> PrototypeIcon.Cards
        AppTab.Settings -> PrototypeIcon.User
    }

/* ============================================================
 * Practice screen
 * ============================================================ */

@Composable
private fun PracticeScreen(topics: List<DictionaryTopic>) {
    val deck = remember(topics) {
        topics.flatMap { topic ->
            topic.words.map { word -> PracticeDeckCard(word, topic.title, prototypeTopicTheme(topic.coverIndex).color) }
        }.take(10)
    }
    var index by remember(deck) { mutableIntStateOf(0) }
    var flipped by remember(deck) { mutableStateOf(false) }
    var known by remember(deck) { mutableIntStateOf(0) }
    var done by remember(deck) { mutableStateOf(false) }

    fun answer(isKnown: Boolean) {
        if (isKnown) known += 1
        if (index + 1 >= deck.size) done = true
        else {
            flipped = false
            index += 1
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PrototypeColor.Background).statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 4.dp)) {
            Text(
                text = "Тренування",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.Ink,
                letterSpacing = (-0.6).sp,
            )
            if (deck.isNotEmpty() && !done) {
                Text(
                    text = "${index + 1} / ${deck.size}",
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(PrototypeColor.ProgressTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((index + 1).toFloat() / deck.size).coerceIn(0f, 1f))
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(PrototypeColor.Purple),
                    )
                }
            }
        }

        when {
            deck.isEmpty() -> PracticeEmptyState(modifier = Modifier.weight(1f))
            done -> PracticeDoneState(
                known = known,
                total = deck.size,
                onRestart = {
                    index = 0; flipped = false; known = 0; done = false
                },
                modifier = Modifier.weight(1f),
            )
            else -> {
                val card = deck[index]
                PracticeFlipCard(
                    card = card,
                    flipped = flipped,
                    onFlip = { flipped = !flipped },
                    modifier = Modifier.weight(1f).padding(horizontal = 26.dp, vertical = 18.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PracticeAnswerButton(
                        text = "Не знаю",
                        icon = PrototypeIcon.Close,
                        color = Color(0xFFC2410C),
                        background = PrototypeColor.NotePeach,
                        modifier = Modifier.weight(1f),
                        onClick = { answer(false) },
                    )
                    PracticeAnswerButton(
                        text = "Знаю",
                        icon = PrototypeIcon.Check,
                        color = Color(0xFF15803D),
                        background = PrototypeColor.NoteGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { answer(true) },
                    )
                }
            }
        }
    }
}

private data class PracticeDeckCard(
    val word: WordEntry,
    val topicTitle: String,
    val accent: Color,
)

@Composable
private fun PracticeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(modifier = Modifier.size(160.dp, 110.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = PrototypeColor.EmptyCardLight,
                topLeft = Offset(w * 0.2f, h * 0.2f),
                size = Size(w * 0.6f, h * 0.6f),
                cornerRadius = CornerRadius(20f, 20f),
            )
        }
        Text(
            text = "Немає слів для повторення",
            modifier = Modifier.padding(top = 18.dp),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            letterSpacing = (-0.21).sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Додай слова у словники — і вони з'являться тут для тренування.",
            modifier = Modifier.padding(top = 9.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PracticeFlipCard(
    card: PracticeDeckCard,
    flipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onFlip),
        shape = RoundedCornerShape(28.dp),
        color = if (flipped) card.accent else PrototypeColor.White,
        border = if (flipped) null else BorderStroke(2.dp, card.accent),
        shadowElevation = 14.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (flipped) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = card.word.translation,
                        color = PrototypeColor.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.64).sp,
                        textAlign = TextAlign.Center,
                    )
                    Column(
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrototypeColor.White.copy(alpha = 0.16f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Use ${card.word.source} in a short sentence.",
                            color = PrototypeColor.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Короткий приклад зі словом «${card.word.translation}».",
                            modifier = Modifier.padding(top = 6.dp),
                            color = PrototypeColor.White.copy(alpha = 0.82f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = card.accent.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape).background(card.accent),
                            )
                            Text(
                                text = card.topicTitle,
                                color = card.accent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = card.word.source,
                        modifier = Modifier.padding(top = 34.dp),
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 40.sp,
                        letterSpacing = (-1.2).sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "/${card.word.source.lowercase()}/",
                        modifier = Modifier.padding(top = 8.dp),
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Surface(
                        modifier = Modifier.padding(top = 18.dp).size(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = PrototypeColor.NeutralSurface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Sound,
                                modifier = Modifier.size(21.dp),
                                color = PrototypeColor.Purple,
                            )
                        }
                    }
                }
                Text(
                    text = "Торкнись, щоб побачити переклад",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PracticeAnswerButton(
    text: String,
    icon: PrototypeIcon,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(62.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(19.dp),
        color = PrototypeColor.White,
        border = BorderStroke(2.dp, PrototypeColor.Line),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(30.dp), shape = CircleShape, color = background) {
                Box(contentAlignment = Alignment.Center) {
                    PrototypeLineIcon(
                        icon = icon,
                        modifier = Modifier.size(20.dp),
                        color = color,
                        strokeWidth = 2.4f,
                    )
                }
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp,
            )
        }
    }
}

@Composable
private fun PracticeDoneState(
    known: Int,
    total: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = if (total == 0) 0 else (known * 100 / total)
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
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
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.Purple,
                letterSpacing = (-0.68).sp,
            )
        }
        Text(
            text = "Чудова робота! 🎉",
            modifier = Modifier.padding(top = 22.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Ти знаєш $known із $total слів цього раунду.",
            modifier = Modifier.padding(top = 9.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(26.dp))
        PrimaryPillButton(label = "Ще раунд", onClick = onRestart)
    }
}

/* ============================================================
 * Profile
 * ============================================================ */

@Composable
private fun ProfileScreen(
    topics: List<DictionaryTopic>,
    userLanguage: LanguageOption,
    learningLanguage: LanguageOption,
    notificationsEnabled: Boolean,
    darkThemeEnabled: Boolean,
    onNotificationsChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onSpeakingClick: () -> Unit,
    onLearningClick: () -> Unit,
) {
    val totalWords = topics.sumOf { it.words.size }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PrototypeColor.Background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Профіль",
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.Ink,
                letterSpacing = (-0.6).sp,
            )
        }
        item { ProfileIdentityCard() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                ProfileStat(
                    icon = PrototypeIcon.Flame,
                    value = "7",
                    label = "днів поспіль",
                    tint = PrototypeColor.StatFlameBg,
                    color = PrototypeColor.StatFlameText,
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    icon = PrototypeIcon.Bookmark,
                    value = totalWords.toString(),
                    label = "слів збережено",
                    tint = PrototypeColor.Tint,
                    color = PrototypeColor.Purple,
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    icon = PrototypeIcon.Cards,
                    value = "12",
                    label = "тренувань",
                    tint = PrototypeColor.StatTrainBg,
                    color = PrototypeColor.StatTrainText,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            SectionLabel("Мови за замовчуванням")
            SettingsGroup {
                SettingRow(
                    leading = { Text(languageFlag(userLanguage.code), fontSize = 20.sp) },
                    label = "Я розмовляю",
                    sub = languageName(userLanguage.code),
                    onClick = onSpeakingClick,
                )
                ProfileSettingsDivider()
                SettingRow(
                    leading = { Text(languageFlag(learningLanguage.code), fontSize = 20.sp) },
                    label = "Я вивчаю",
                    sub = languageName(learningLanguage.code),
                    onClick = onLearningClick,
                )
            }
            Text(
                text = "Нові словники створюються з цією парою мов автоматично.",
                modifier = Modifier.padding(start = 4.dp, top = 9.dp, end = 4.dp),
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }
        item {
            SectionLabel("Налаштування")
            SettingsGroup {
                SettingRow(
                    leading = {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Bell,
                            modifier = Modifier.size(19.dp),
                            color = PrototypeColor.Muted,
                            strokeWidth = 1.8f,
                        )
                    },
                    label = "Сповіщення",
                    sub = "Нагадування про тренування",
                    right = {
                        Toggle(
                            on = notificationsEnabled,
                            onToggle = { onNotificationsChanged(!notificationsEnabled) },
                        )
                    },
                    onClick = { onNotificationsChanged(!notificationsEnabled) },
                )
                ProfileSettingsDivider()
                SettingRow(
                    leading = {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Moon,
                            modifier = Modifier.size(19.dp),
                            color = PrototypeColor.Muted,
                            strokeWidth = 1.8f,
                        )
                    },
                    label = "Темна тема",
                    sub = null,
                    right = {
                        Toggle(
                            on = darkThemeEnabled,
                            onToggle = { onDarkThemeChanged(!darkThemeEnabled) },
                        )
                    },
                    onClick = { onDarkThemeChanged(!darkThemeEnabled) },
                )
            }
        }
        item {
            SettingsGroup {
                SettingRow(
                    leading = {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Invite,
                            modifier = Modifier.size(19.dp),
                            color = PrototypeColor.Muted,
                            strokeWidth = 1.8f,
                        )
                    },
                    label = "Запросити друзів",
                    sub = "Поділись Vocabee",
                    onClick = {},
                )
                ProfileSettingsDivider()
                SettingRow(
                    leading = {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Help,
                            modifier = Modifier.size(19.dp),
                            color = PrototypeColor.Muted,
                            strokeWidth = 1.8f,
                        )
                    },
                    label = "Допомога та підтримка",
                    sub = null,
                    onClick = {},
                )
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                color = PrototypeColor.White,
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Вийти",
                        color = PrototypeColor.Red,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                    )
                }
            }
            Text(
                text = "Vocabee · v1.0.0",
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ProfileIdentityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PrototypeColor.White,
        shadowElevation = 7.dp,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PrototypeColor.AvatarStart, PrototypeColor.AvatarEnd))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "НК",
                    color = PrototypeColor.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Надія Кобилінська",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "nadiia@vocabee.app",
                    modifier = Modifier.padding(top = 2.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(13.dp),
                color = PrototypeColor.Tint,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Edit,
                        modifier = Modifier.size(18.dp),
                        color = PrototypeColor.Purple,
                        strokeWidth = 1.9f,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(
    icon: PrototypeIcon,
    value: String,
    label: String,
    tint: Color,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(18.dp),
        color = PrototypeColor.White,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(12.dp), color = tint) {
                Box(contentAlignment = Alignment.Center) {
                    PrototypeLineIcon(
                        icon = icon,
                        modifier = Modifier.size(20.dp),
                        color = color,
                        strokeWidth = 1.9f,
                    )
                }
            }
            Text(
                text = value,
                modifier = Modifier.padding(top = 9.dp),
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = (-0.44).sp,
                maxLines = 1,
            )
            Text(
                text = label,
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        color = PrototypeColor.Muted2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.5.sp,
        letterSpacing = 0.63.sp,
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = PrototypeColor.White,
        shadowElevation = 5.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingRow(
    leading: @Composable () -> Unit,
    label: String,
    sub: String?,
    right: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(11.dp),
            color = PrototypeColor.NeutralSurface,
        ) {
            Box(contentAlignment = Alignment.Center) { leading() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (sub != null) {
                Text(
                    text = sub,
                    modifier = Modifier.padding(top = 2.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (right == null) {
            PrototypeLineIcon(
                icon = PrototypeIcon.ChevronRight,
                modifier = Modifier.size(18.dp),
                color = PrototypeColor.Muted3,
                strokeWidth = 2f,
            )
        } else {
            right()
        }
    }
}

@Composable
private fun ProfileSettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 63.dp)
            .height(1.dp)
            .background(PrototypeColor.Line2),
    )
}

@Composable
private fun Toggle(on: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(if (on) PrototypeColor.Purple else PrototypeColor.SwitchTrack)
            .clickable(onClick = onToggle)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(PrototypeColor.White),
        )
    }
}

@Composable
private fun MissingTopicScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(PrototypeColor.White).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.size(40.dp).clickable(onClick = onBack),
            shape = RoundedCornerShape(13.dp),
            color = PrototypeColor.NeutralSurface,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.ChevronLeft,
                    modifier = Modifier.size(22.dp),
                    color = PrototypeColor.Muted,
                )
            }
        }
        Text(
            text = "Словник не знайдено",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
        )
        Text(
            text = "Поверніться до списку словників.",
            color = PrototypeColor.Muted,
            fontSize = 15.sp,
        )
    }
}
