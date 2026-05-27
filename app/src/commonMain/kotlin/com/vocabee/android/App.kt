package com.vocabee.android

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vocabee.android.domain.model.DictionaryTopic
import com.vocabee.android.domain.model.LanguageOption
import com.vocabee.android.domain.model.TranslationOption
import com.vocabee.android.domain.model.TranslationOptionNote
import com.vocabee.android.domain.model.TopicUpdatedLabel
import com.vocabee.android.domain.model.WordEntry
import com.vocabee.android.navigation.AppTab
import com.vocabee.android.navigation.LanguagePickerTarget
import com.vocabee.android.navigation.VocabeeRoute
import com.vocabee.android.navigation.selectedTabFor
import com.vocabee.android.navigation.vocabeeSavedStateConfiguration
import com.vocabee.android.platform.MachineTranslationProvider
import com.vocabee.android.platform.NoMachineTranslationProvider
import com.vocabee.android.platform.NoSpeechInputController
import com.vocabee.android.platform.SpeechInputController
import com.vocabee.android.presentation.VocabeeEvent
import com.vocabee.android.presentation.VocabeeStore
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

private val Honey = Color(0xFFFFC247)
private val Ink = Color(0xFF1D2329)
private val Meadow = Color(0xFF2F8F6B)
private val Sky = Color(0xFF4B7BEC)
private val Paper = Color(0xFFFFFBF3)
private val SoftLine = Color(0xFFE7DED0)

private object VocabeePadding {
    object Horizontal {
        val Small = 12.dp
        val Medium = 20.dp
        val Large = 24.dp
    }

    object Vertical {
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 28.dp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabeeApp(
    speechInputController: SpeechInputController = NoSpeechInputController,
    machineTranslationProvider: MachineTranslationProvider = NoMachineTranslationProvider,
) {
    VocabeeTheme {
        val store = remember(machineTranslationProvider) {
            VocabeeStore(machineTranslationProvider = machineTranslationProvider)
        }
        val state = store.state
        var showNewTopicDialog by remember { mutableStateOf(false) }

        val backStack = rememberNavBackStack(
            vocabeeSavedStateConfiguration,
            VocabeeRoute.DictionaryHome,
        )
        val currentRoute = backStack.lastOrNull() as? VocabeeRoute
        val selectedTab = selectedTabFor(currentRoute)

        fun openRoot(route: VocabeeRoute) {
            backStack.clear()
            backStack.add(route)
        }

        if (showNewTopicDialog) {
            NewTopicSheet(
                sourceLanguage = state.userLanguage,
                targetLanguage = state.learningLanguage,
                onDismiss = { showNewTopicDialog = false },
                onCreate = { title, coverIndex ->
                    store.onEvent(
                        VocabeeEvent.CreateTopic(
                            title = title,
                            coverIndex = coverIndex,
                        )
                    )
                    showNewTopicDialog = false
                },
            )
        }

        Scaffold(
            bottomBar = {
                if (currentRoute !is VocabeeRoute.VoiceInput && currentRoute !is VocabeeRoute.KeyboardInput) {
                    VocabeeBottomBar(
                        selectedTab = selectedTab,
                        onTabClick = { tab -> openRoot(tab.route) },
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == AppTab.Dictionary && currentRoute == VocabeeRoute.DictionaryHome) {
                    NewDictionaryFloatingButton(
                        compact = state.topics.isEmpty(),
                        onClick = { showNewTopicDialog = true },
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            containerColor = Paper,
        ) { innerPadding ->
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                backStack = backStack,
                onBack = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    }
                },
                entryProvider = entryProvider {
                    entry<VocabeeRoute.DictionaryHome> {
                        DictionaryHomeScreen(
                            topics = state.topics,
                            userLanguage = state.userLanguage,
                            learningLanguage = state.learningLanguage,
                            onCreateClick = { showNewTopicDialog = true },
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
                            TopicDetailScreen(
                                topic = topic,
                                recentlyAddedWordId = state.recentlyAddedWordId,
                                onBack = { backStack.removeLastOrNull() },
                                onKeyboardInputClick = {
                                    backStack.add(VocabeeRoute.KeyboardInput(topic.id))
                                },
                                onVoiceInputClick = {
                                    backStack.add(VocabeeRoute.VoiceInput(topic.id))
                                },
                            )
                        }
                    }

                    entry<VocabeeRoute.KeyboardInput> { route ->
                        val topic = state.topics.firstOrNull { it.id == route.topicId }
                        if (topic == null) {
                            MissingTopicScreen(onBack = { backStack.removeLastOrNull() })
                        } else {
                            KeyboardInputScreen(
                                topic = topic,
                                translationOptionsFor = store::translationOptionsFor,
                                onRequestMachineTranslation = { input ->
                                    store.onEvent(
                                        VocabeeEvent.RequestMachineTranslation(
                                            topicId = topic.id,
                                            input = input,
                                        )
                                    )
                                },
                                onBack = { backStack.removeLastOrNull() },
                                onAddWord = { source, translation ->
                                    store.onEvent(
                                        VocabeeEvent.AddWord(
                                            topicId = topic.id,
                                            source = source,
                                            translation = translation,
                                        )
                                    )
                                    backStack.removeLastOrNull()
                                },
                            )
                        }
                    }

                    entry<VocabeeRoute.VoiceInput> { route ->
                        val topic = state.topics.firstOrNull { it.id == route.topicId }
                        if (topic == null) {
                            MissingTopicScreen(onBack = { backStack.removeLastOrNull() })
                        } else {
                            VoiceInputScreen(
                                topic = topic,
                                speechInputController = speechInputController,
                                translationOptionsFor = store::translationOptionsFor,
                                onRequestMachineTranslation = { input ->
                                    store.onEvent(
                                        VocabeeEvent.RequestMachineTranslation(
                                            topicId = topic.id,
                                            input = input,
                                        )
                                    )
                                },
                                onBack = { backStack.removeLastOrNull() },
                                onAddWord = { source, translation ->
                                    store.onEvent(
                                        VocabeeEvent.AddWord(
                                            topicId = topic.id,
                                            source = source,
                                            translation = translation,
                                        )
                                    )
                                    backStack.removeLastOrNull()
                                },
                            )
                        }
                    }

                    entry<VocabeeRoute.Practice> {
                        PracticeScreen(topics = state.topics)
                    }

                    entry<VocabeeRoute.Settings> {
                        ProfileScreen(
                            topics = state.topics,
                            userLanguage = state.userLanguage,
                            learningLanguage = state.learningLanguage,
                            notificationsEnabled = state.notificationsEnabled,
                            darkThemeEnabled = state.darkThemeEnabled,
                            onNotificationsChanged = { enabled ->
                                store.onEvent(VocabeeEvent.SetNotificationsEnabled(enabled))
                            },
                            onDarkThemeChanged = { enabled ->
                                store.onEvent(VocabeeEvent.SetDarkThemeEnabled(enabled))
                            },
                            onSpeakingClick = {
                                backStack.add(VocabeeRoute.LanguagePicker(LanguagePickerTarget.Speaking))
                            },
                            onLearningClick = {
                                backStack.add(VocabeeRoute.LanguagePicker(LanguagePickerTarget.Learning))
                            },
                        )
                    }

                    entry<VocabeeRoute.LanguagePicker> { route ->
                        val selectedLanguage = when (route.target) {
                            LanguagePickerTarget.Speaking -> state.userLanguage
                            LanguagePickerTarget.Learning -> state.learningLanguage
                        }
                        LanguagePickerScreen(
                            target = route.target,
                            supportedLanguages = state.supportedLanguages,
                            selectedLanguage = selectedLanguage,
                            onBack = { backStack.removeLastOrNull() },
                            onDone = { option ->
                                val event = when (route.target) {
                                    LanguagePickerTarget.Speaking -> VocabeeEvent.SelectSpeakingLanguage(option)
                                    LanguagePickerTarget.Learning -> VocabeeEvent.SelectLearningLanguage(option)
                                }
                                store.onEvent(event)
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun VocabeeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Meadow,
            onPrimary = Color.White,
            secondary = Sky,
            onSecondary = Color.White,
            tertiary = Honey,
            onTertiary = Ink,
            background = Paper,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFF3EDE2),
            onSurfaceVariant = Color(0xFF5C645E),
            outline = SoftLine,
        ),
        content = content,
    )
}

private data class TopicVisual(
    val background: Color,
    val bubble: Color,
    val accent: Color,
    val glyph: String,
)

private val topicVisuals = listOf(
    TopicVisual(Color(0xFFFFC9A8), Color(0xFFFFE1CD), Color(0xFF8A4829), "◐"),
    TopicVisual(Color(0xFFD4E8C0), Color(0xFFE5F2D8), Color(0xFF486E3A), "▣"),
    TopicVisual(Color(0xFFC4DDF0), Color(0xFFD9EAF8), Color(0xFF294D73), "✈"),
    TopicVisual(Color(0xFFEFC4D0), Color(0xFFF7D8E2), Color(0xFF91405A), "✦"),
    TopicVisual(Color(0xFFF8E197), Color(0xFFFFEEC0), Color(0xFF806425), "◆"),
    TopicVisual(Color(0xFFABDCCF), Color(0xFFC7ECE3), Color(0xFF246F61), "⌂"),
    TopicVisual(Color(0xFFD9CEF2), Color(0xFFEAE1FA), Color(0xFF554281), "+"),
    TopicVisual(Color(0xFFFFBBA6), Color(0xFFFFD7CB), Color(0xFF9A4B32), "●"),
    TopicVisual(Color(0xFFE4D4AA), Color(0xFFF0E6C7), Color(0xFF705B2B), "◇"),
    TopicVisual(Color(0xFFC8B8E5), Color(0xFFE0D6F1), Color(0xFF5A4A82), "◎"),
)

private fun topicVisual(index: Int): TopicVisual {
    return topicVisuals[index % topicVisuals.size]
}

@Composable
private fun DictionaryHomeScreen(
    topics: List<DictionaryTopic>,
    userLanguage: LanguageOption,
    learningLanguage: LanguageOption,
    onCreateClick: () -> Unit,
    onTopicClick: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VocabeePadding.Horizontal.Large,
            top = VocabeePadding.Vertical.Large,
            end = VocabeePadding.Horizontal.Large,
            bottom = 14.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeTopControls(
                userLanguage = userLanguage,
                learningLanguage = learningLanguage,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(top = 26.dp, bottom = if (topics.isEmpty()) 0.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = vocabeeString(
                        VocabeeString.HomeSummary,
                        vocabeeQuantityString(VocabeeQuantityString.TopicCount, topics.size, topics.size).uppercase(),
                        vocabeeQuantityString(
                            VocabeeQuantityString.WordCount,
                            topics.sumOf { it.words.size },
                            topics.sumOf { it.words.size },
                        ).uppercase(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF948A7D),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = vocabeeString(VocabeeString.HomeTitle),
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (topics.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyDictionaryState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    onCreateClick = onCreateClick,
                )
            }
        }

        itemsIndexed(
            items = topics,
            key = { _, topic -> topic.id },
        ) { index, topic ->
            TopicTile(
                topic = topic,
                visual = topicVisual(topic.coverIndex),
                onClick = { onTopicClick(topic.id) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun EmptyDictionaryState(
    modifier: Modifier = Modifier,
    onCreateClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFDCC6)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 5.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFF8A4829),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = vocabeeString(VocabeeString.EmptyDictionaryTitleLine1),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = Color(0xFF363029),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = vocabeeString(VocabeeString.EmptyDictionaryTitleLine2),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = Color(0xFF363029),
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = vocabeeString(VocabeeString.EmptyDictionarySubtitle),
            modifier = Modifier.widthIn(max = 250.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF756B60),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(22.dp))
        Button(
            onClick = onCreateClick,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF26C2F),
                contentColor = Color.White,
            ),
            contentPadding = PaddingValues(
                horizontal = VocabeePadding.Horizontal.Medium,
                vertical = VocabeePadding.Vertical.Medium,
            ),
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = vocabeeString(VocabeeString.EmptyDictionaryCreateFirst),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HomeTopControls(
    userLanguage: LanguageOption,
    learningLanguage: LanguageOption,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = Color(0xFFF4EFE8),
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = userLanguage.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9D9286),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = learningLanguage.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE96C35),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier.size(44.dp),
            color = Color(0xFFF4EFE8),
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "⌕",
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TopicTile(
    topic: DictionaryTopic,
    visual: TopicVisual,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.88f)
            .clip(RoundedCornerShape(20.dp))
            .background(visual.background)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(78.dp)
                .clip(CircleShape)
                .background(visual.bubble),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = visual.glyph,
                style = MaterialTheme.typography.titleLarge,
                color = visual.accent,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = wordCountLabel(topic.words.size),
            modifier = Modifier.align(Alignment.TopStart),
            style = MaterialTheme.typography.labelMedium,
            color = visual.accent.copy(alpha = 0.74f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = visual.accent,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = topicUpdatedLabel(topic.updatedLabel),
                style = MaterialTheme.typography.bodySmall,
                color = visual.accent.copy(alpha = 0.58f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun VocabeeBottomBar(
    selectedTab: AppTab,
    onTabClick: (AppTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
            .height(74.dp),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VocabeePadding.Horizontal.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
}

@Composable
private fun NewDictionaryFloatingButton(
    compact: Boolean,
    onClick: () -> Unit,
) {
    if (compact) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            containerColor = Color(0xFFF26C2F),
            contentColor = Color.White,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
            )
        }
        return
    }

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.height(56.dp),
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFFF26C2F),
        contentColor = Color.White,
        icon = {
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
            )
        },
        text = {
            Text(
                text = vocabeeString(VocabeeString.NewDictionary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    )
}

@Composable
private fun BottomTabButton(
    tab: AppTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) Color(0xFFE96C35) else Color(0xFF8E857B)
    val backgroundColor = if (selected) Color(0xFFFFF0E7) else Color.Transparent

    Row(
        modifier = Modifier
            .then(modifier)
            .height(42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tab.icon,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = vocabeeString(tab.labelKey),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TopicDetailScreen(
    topic: DictionaryTopic,
    recentlyAddedWordId: String?,
    onBack: () -> Unit,
    onKeyboardInputClick: () -> Unit,
    onVoiceInputClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TopicDetailHeader(
                    topic = topic,
                    onBack = onBack,
                )
            }

            if (topic.words.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = vocabeeString(VocabeeString.EmptyTopicTitle),
                        subtitle = vocabeeString(VocabeeString.EmptyTopicSubtitle),
                    )
                }
            } else {
                items(topic.words, key = { it.id }) { word ->
                    DictionaryWordCard(
                        word = word,
                        highlighted = word.id == recentlyAddedWordId,
                modifier = Modifier.padding(horizontal = VocabeePadding.Horizontal.Medium),
                    )
                }
            }
        }

        TopicInputDock(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            onKeyboardInputClick = onKeyboardInputClick,
            onVoiceInputClick = onVoiceInputClick,
        )
    }
}

@Composable
private fun TopicDetailHeader(
    topic: DictionaryTopic,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFC4DDF0),
        shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(
                start = VocabeePadding.Horizontal.Medium,
                top = 16.dp,
                end = VocabeePadding.Horizontal.Medium,
                bottom = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VocabeeTopBar(
                navigationContainerColor = Color.White.copy(alpha = 0.8f),
                navigationContentColor = Color(0xFF294D73),
                onNavigateBack = onBack,
                actions = listOf(
                    VocabeeTopBarAction.Custom {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.62f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⌁", style = MaterialTheme.typography.titleMedium, color = Color(0xFF294D73))
                            }
                        }
                    },
                ),
            )
            Text(
                text = vocabeeString(
                    VocabeeString.HomeSummary,
                    wordCountLabel(topic.words.size),
                    topicUpdatedLabel(topic.updatedLabel),
                ).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF4D7296),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = topic.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF294D73),
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopicActionPill(text = vocabeeString(VocabeeString.TopicActionPractice))
                TopicActionPill(text = vocabeeString(VocabeeString.TopicActionShuffle))
            }
        }
    }
}

@Composable
private fun TopicActionPill(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE6F2FB),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF294D73),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DictionaryWordCard(
    word: WordEntry,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
        border = BorderStroke(
            width = if (highlighted) 1.dp else 0.dp,
            color = if (highlighted) Color(0xFFF26C2F) else Color.Transparent,
        ),
    ) {
        Box {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = VocabeePadding.Vertical.Medium),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = wordPronunciation(word.translation),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8F857B),
                )
                Text(
                    text = word.source,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "\"${sampleSentence(word.translation)}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB0A69A),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                )
            }
            if (highlighted) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF26C2F),
                ) {
                    Text(
                        text = vocabeeString(VocabeeString.JustAdded),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicInputDock(
    modifier: Modifier = Modifier,
    onKeyboardInputClick: () -> Unit,
    onVoiceInputClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                start = VocabeePadding.Horizontal.Small,
                top = VocabeePadding.Vertical.Small,
                end = 8.dp,
                bottom = VocabeePadding.Vertical.Small,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF8F2EA))
                    .clickable(onClick = onKeyboardInputClick)
                    .padding(horizontal = VocabeePadding.Horizontal.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("⌨", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8F857B))
                Text(
                    text = vocabeeString(VocabeeString.KeyboardInputPlaceholder),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8F857B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onVoiceInputClick),
                shape = CircleShape,
                color = Color(0xFFF26C2F),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = vocabeeString(VocabeeString.Mic),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardInputScreen(
    topic: DictionaryTopic,
    translationOptionsFor: (DictionaryTopic, String) -> List<TranslationOption>,
    onRequestMachineTranslation: (String) -> Unit,
    onBack: () -> Unit,
    onAddWord: (String, String) -> Unit,
) {
    var input by remember(topic.id) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val cleanedInput = input.trim()
    val suggestions = translationOptionsFor(topic, cleanedInput)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(topic.id, cleanedInput) {
        if (cleanedInput.isNotBlank()) {
            onRequestMachineTranslation(cleanedInput)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = VocabeePadding.Horizontal.Medium, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VocabeeTopBar(
            title = {
                Text(
                    text = topic.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF948A7D),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = VocabeeNavigationIcon.Close,
            navigationContainerColor = Color.White,
            navigationContentColor = Color(0xFF8F857B),
            onNavigateBack = onBack,
            actions = listOf(
                VocabeeTopBarAction.Text(
                    text = vocabeeString(VocabeeString.Done),
                    textColor = Color(0xFFE96C35),
                    action = onBack,
                ),
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (cleanedInput.isBlank()) {
                    vocabeeString(VocabeeString.KeyboardEnterWord)
                } else {
                    vocabeeString(VocabeeString.KeyboardSearchTranslations)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE96C35),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = vocabeeString(
                    VocabeeString.TranslationHint,
                    topic.sourceLanguage.shortName.lowercase(),
                    topic.targetLanguage.shortName.lowercase(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F857B),
            )
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text(vocabeeString(VocabeeString.KeyboardInputPlaceholder)) },
            singleLine = true,
            trailingIcon = {
                if (input.isNotBlank()) {
                    Text(
                        text = "×",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { input = "" }
                            .padding(8.dp),
                        color = Color(0xFF8F857B),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(12.dp),
        )

        if (cleanedInput.isBlank()) {
            KeyboardPromptList(
                suggestions = listOf(
                    vocabeeString(VocabeeString.KeyboardSuggestionAirplane),
                    vocabeeString(VocabeeString.KeyboardSuggestionDepart),
                    vocabeeString(VocabeeString.KeyboardSuggestionLuggage),
                    vocabeeString(VocabeeString.KeyboardSuggestionTicket),
                ),
                onSuggestionClick = { input = it },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = vocabeeString(VocabeeString.KeyboardFoundCount, suggestions.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF948A7D),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = vocabeeString(VocabeeString.KeyboardTapToAdd),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE96C35),
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { option ->
                    KeyboardTranslationOptionRow(
                        source = cleanedInput,
                        option = option,
                        onClick = {
                            if (!option.alreadyAdded) {
                                onAddWord(cleanedInput, option.value)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardPromptList(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        suggestions.forEach { suggestion ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) },
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = VocabeePadding.Horizontal.Small, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = RoundedCornerShape(7.dp),
                        color = Color(0xFFF4EFE8),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("→", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8F857B))
                        }
                    }
                    Text(
                        text = vocabeeString(VocabeeString.KeyboardSuggestion, suggestion),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF756B60),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardTranslationOptionRow(
    source: String,
    option: TranslationOption,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VocabeePadding.Horizontal.Small, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.value,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = vocabeeString(VocabeeString.TranslationOptionSource, source),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF756B60),
                )
                Text(
                    text = "\"${sampleSentence(option.value)}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB0A69A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .then(
                        if (option.alreadyAdded) {
                            Modifier
                        } else {
                            Modifier.clickable(onClick = onClick)
                        }
                    ),
                shape = CircleShape,
                color = if (option.alreadyAdded) Color(0xFFEDE1D0) else Color(0xFFF26C2F),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (option.alreadyAdded) "✓" else "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (option.alreadyAdded) Color(0xFF8F806F) else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceInputScreen(
    topic: DictionaryTopic,
    speechInputController: SpeechInputController,
    translationOptionsFor: (DictionaryTopic, String) -> List<TranslationOption>,
    onRequestMachineTranslation: (String) -> Unit,
    onBack: () -> Unit,
    onAddWord: (String, String) -> Unit,
) {
    var isListening by remember { mutableStateOf(false) }
    var partialText by remember { mutableStateOf("") }
    var heardText by remember { mutableStateOf("") }
    var speechError by remember { mutableStateOf<String?>(null) }
    val displayText = when {
        isListening -> partialText.ifBlank { heardText }.ifBlank { vocabeeString(VocabeeString.VoiceSpeakPrompt) }
        heardText.isNotBlank() -> heardText
        else -> vocabeeString(VocabeeString.VoiceHoldMic)
    }
    val suggestions = translationOptionsFor(topic, heardText)

    DisposableEffect(speechInputController) {
        onDispose { speechInputController.stopListening() }
    }

    LaunchedEffect(topic.id, heardText) {
        if (heardText.isNotBlank()) {
            onRequestMachineTranslation(heardText)
        }
    }

    fun startHoldToTalk() {
        partialText = ""
        heardText = ""
        speechError = null
        speechInputController.startListening(
            languageTag = topic.sourceLanguage.speechTag,
            onPartialResult = { recognizedText ->
                partialText = recognizedText
            },
            onResult = { recognizedText ->
                heardText = recognizedText.trim()
                partialText = ""
                isListening = false
            },
            onError = { message ->
                speechError = message
                partialText = ""
                isListening = false
            },
            onListeningChanged = { listening ->
                isListening = listening
            },
        )
    }

    fun stopHoldToTalk() {
        speechInputController.stopListening()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        VocabeeTopBar(
            title = {
                Text(
                    text = topic.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF948A7D),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = VocabeeNavigationIcon.Close,
            navigationContainerColor = Color.White,
            navigationContentColor = Color(0xFF8F857B),
            onNavigateBack = {
                speechInputController.stopListening()
                onBack()
            },
        )

        if (isListening) {
            ListeningTakeoverContent(
                displayText = displayText,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            VoiceResultsContent(
                heardText = heardText,
                speechError = speechError,
                suggestions = suggestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onAddWord = { translation ->
                    onAddWord(heardText, translation)
                },
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isListening) {
                    vocabeeString(VocabeeString.VoiceReleaseToStop)
                } else {
                    vocabeeString(VocabeeString.VoiceHoldAndSpeak)
                },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF948A7D),
                fontWeight = FontWeight.Bold,
            )
            HoldToTalkButton(
                enabled = speechInputController.isSupported,
                isListening = isListening,
                onHoldStart = ::startHoldToTalk,
                onHoldEnd = ::stopHoldToTalk,
            )
        }
    }
}

@Composable
private fun ListeningTakeoverContent(
    displayText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = vocabeeString(VocabeeString.VoiceListening),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE96C35),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = compactSpokenText(displayText),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = Color(0xFF363029),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(14.dp))
        VoiceWaveform()
    }
}

@Composable
private fun VoiceResultsContent(
    heardText: String,
    speechError: String?,
    suggestions: List<TranslationOption>,
    modifier: Modifier = Modifier,
    onAddWord: (String) -> Unit,
) {
    Column(
        modifier = modifier.padding(top = VocabeePadding.Vertical.Large),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (heardText.isBlank()) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = speechError ?: vocabeeString(VocabeeString.VoiceInitialInstruction),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (speechError == null) Color(0xFF756B60) else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Text(
                text = vocabeeString(VocabeeString.VoiceHeard),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF948A7D),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = heardText,
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF363029),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = vocabeeString(VocabeeString.VoiceRetry),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F857B),
            )
            Spacer(modifier = Modifier.height(8.dp))
            suggestions.forEach { option ->
                VoiceTranslationOptionRow(
                    source = heardText,
                    option = option,
                    onClick = {
                        if (!option.alreadyAdded) {
                            onAddWord(option.value)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun VoiceTranslationOptionRow(
    source: String,
    option: TranslationOption,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = VocabeePadding.Horizontal.Small, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.value,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = source.lowercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8F857B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = translationOptionNote(option.note),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB0A69A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .then(
                        if (option.alreadyAdded) {
                            Modifier
                        } else {
                            Modifier.clickable(onClick = onClick)
                        }
                    ),
                shape = CircleShape,
                color = if (option.alreadyAdded) Color(0xFFEDE1D0) else Color(0xFFF26C2F),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (option.alreadyAdded) "✓" else "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (option.alreadyAdded) Color(0xFF8F806F) else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldToTalkButton(
    enabled: Boolean,
    isListening: Boolean,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val outerColor = if (isListening) Color(0xFFFFDEC8) else Color(0xFFFFEEE3)
    val innerColor = if (enabled) Color(0xFFF26C2F) else Color(0xFFCDBFB1)

    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(outerColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(innerColor)
                .then(
                    if (enabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onHoldStart()
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        onHoldEnd()
                                    }
                                },
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = vocabeeString(VocabeeString.Mic),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun VoiceWaveform() {
    val heights = listOf(14, 22, 10, 26, 18, 34, 20, 38, 18, 30, 14, 28, 22, 36, 16, 24, 12, 32, 20)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        heights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF26C2F)),
            )
        }
    }
}

private fun compactSpokenText(value: String): String {
    val cleaned = value.trim()
    return if (cleaned.length > 10) {
        "${cleaned.take(9)}..."
    } else {
        cleaned
    }
}

private fun wordPronunciation(word: String): String {
    val normalized = word.lowercase()
    return when (normalized) {
        "depart" -> "v. /dɪˈpɑːrt/"
        "boarding pass" -> "n. /ˈbɔːrdɪŋ pæs/"
        "overhead bin" -> "n. /ˈoʊvərhed bɪn/"
        "turbulence" -> "n. /ˈtɜːrbjələns/"
        "luggage" -> "n. /ˈlʌɡɪdʒ/"
        "airport" -> "n. /ˈerpɔːrt/"
        else -> "/${normalized.take(12)}/"
    }
}

@Composable
private fun sampleSentence(word: String): String {
    return when (word.lowercase()) {
        "depart" -> vocabeeString(VocabeeString.SampleSentenceDepart)
        "boarding pass" -> vocabeeString(VocabeeString.SampleSentenceBoardingPass)
        "overhead bin" -> vocabeeString(VocabeeString.SampleSentenceOverheadBin)
        "turbulence" -> vocabeeString(VocabeeString.SampleSentenceTurbulence)
        "luggage" -> vocabeeString(VocabeeString.SampleSentenceLuggage)
        "airport" -> vocabeeString(VocabeeString.SampleSentenceAirport)
        else -> vocabeeString(VocabeeString.SampleSentenceDefault, word)
    }
}

@Composable
private fun TranslationOptionRow(
    option: TranslationOption,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFFF6FAF8),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9E8E0)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = VocabeePadding.Horizontal.Small,
                vertical = VocabeePadding.Vertical.Medium,
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = translationOptionNote(option.note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "+",
                style = MaterialTheme.typography.titleLarge,
                color = Meadow,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WordCard(word: WordEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftLine),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Meadow),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.source,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = word.translation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PracticeScreen(topics: List<DictionaryTopic>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = VocabeePadding.Horizontal.Large, vertical = VocabeePadding.Vertical.Large),
    ) {
        Text(
            text = vocabeeString(VocabeeString.PracticeEyebrow),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF948A7D),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = vocabeeString(VocabeeString.PracticeTitle),
            style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
            color = Color(0xFF363029),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFD9EBC2)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "ϟ",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF486E3A),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = vocabeeString(VocabeeString.PracticeComingSoonLine1),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF363029),
                textAlign = TextAlign.Center,
            )
            Text(
                text = vocabeeString(VocabeeString.PracticeComingSoonLine2),
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF363029),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = vocabeeString(VocabeeString.PracticeSubtitle),
                modifier = Modifier.widthIn(max = 260.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF756B60),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFEDE1D0),
            ) {
                Text(
                    text = vocabeeString(VocabeeString.InDevelopment),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8F806F),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VocabeePadding.Horizontal.Large,
            top = VocabeePadding.Vertical.Large,
            end = VocabeePadding.Horizontal.Large,
            bottom = 14.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = vocabeeString(VocabeeString.SettingsEyebrow),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF948A7D),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = vocabeeString(VocabeeString.ProfileTitle),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFF363029),
                    )
                }
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = Color(0xFFF4EFE8),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⌁",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFE96C35),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        item {
            ProfileIdentityCard()
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileStatCard(
                    title = "14",
                    label = vocabeeString(VocabeeString.ProfileStreakLabel),
                    icon = "♨",
                    background = Color(0xFFFFDCC6),
                    accent = Color(0xFFE96C35),
                    modifier = Modifier.weight(1f),
                )
                ProfileStatCard(
                    title = topics.sumOf { it.words.size }.toString(),
                    label = vocabeeString(VocabeeString.ProfileWordsLabel),
                    icon = "▢",
                    background = Color(0xFFC4DDF0),
                    accent = Color(0xFF294D73),
                    modifier = Modifier.weight(1f),
                )
                ProfileStatCard(
                    title = "48",
                    label = vocabeeString(VocabeeString.ProfileTestsLabel),
                    icon = "ϟ",
                    background = Color(0xFFD4E8C0),
                    accent = Color(0xFF486E3A),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = vocabeeString(VocabeeString.LanguageSectionTitle),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF948A7D),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column {
                    ProfileLanguageRow(
                        title = vocabeeString(VocabeeString.SpeakingLanguageTitle),
                        subtitle = vocabeeString(VocabeeString.SpeakingLanguageSubtitle),
                        language = userLanguage,
                        onClick = onSpeakingClick,
                    )
                    ProfileDivider()
                    ProfileLanguageRow(
                        title = vocabeeString(VocabeeString.LearningLanguageTitle),
                        subtitle = vocabeeString(VocabeeString.LearningLanguageSubtitle),
                        language = learningLanguage,
                        onClick = onLearningClick,
                    )
                }
            }
        }

        item {
            Text(
                text = vocabeeString(VocabeeString.SettingsEyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF948A7D),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Column {
                    SettingToggleRow(
                        title = vocabeeString(VocabeeString.NotificationsTitle),
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsChanged,
                    )
                    ProfileDivider()
                    SettingToggleRow(
                        title = vocabeeString(VocabeeString.DarkThemeTitle),
                        checked = darkThemeEnabled,
                        onCheckedChange = onDarkThemeChanged,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProfileIdentityCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFDCC6),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                        color = Color(0xFF8A4829),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vocabeeString(VocabeeString.ProfileName),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "anna@vocabee.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8F857B),
                )
            }
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = Color(0xFFF4EFE8),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF8F857B),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    label: String,
    icon: String,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(10.dp),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = accent,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProfileLanguageRow(
    title: String,
    subtitle: String,
    language: LanguageOption,
    onClick: () -> Unit,
) {
    val languageName = localizedLanguageName(language)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VocabeePadding.Horizontal.Small, vertical = VocabeePadding.Vertical.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF363029),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F857B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LanguageBadge(language = language, languageName = languageName)
        Text(
            text = "›",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFC0B7AC),
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = VocabeePadding.Horizontal.Small,
                end = 8.dp,
                top = VocabeePadding.Vertical.Small,
                bottom = VocabeePadding.Vertical.Small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF363029),
            fontWeight = FontWeight.Bold,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ProfileDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = VocabeePadding.Horizontal.Small)
            .background(Color(0xFFF1E8DC)),
    )
}

@Composable
private fun LanguagePickerScreen(
    target: LanguagePickerTarget,
    supportedLanguages: List<LanguageOption>,
    selectedLanguage: LanguageOption,
    onBack: () -> Unit,
    onDone: (LanguageOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var pendingLanguage by remember(selectedLanguage.code) { mutableStateOf(selectedLanguage) }
    val languageDisplays = supportedLanguages.associate { option ->
        option.code to LanguageDisplayText(
            name = localizedLanguageName(option),
            nativeName = localizedLanguageNativeName(option),
        )
    }
    val languages = if (query.isBlank()) {
        supportedLanguages
    } else {
        supportedLanguages.filter { option ->
            val display = languageDisplays[option.code]
            display?.name?.contains(query, ignoreCase = true) == true ||
                display?.nativeName?.contains(query, ignoreCase = true) == true ||
                option.code.contains(query, ignoreCase = true)
        }
    }
    val recentLanguages = languages.take(3)
    val otherLanguages = languages.drop(3)
    val title = when (target) {
        LanguagePickerTarget.Speaking -> vocabeeString(VocabeeString.SpeakingLanguageTitle)
        LanguagePickerTarget.Learning -> vocabeeString(VocabeeString.LearningLanguageTitle)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = VocabeePadding.Horizontal.Medium,
            top = VocabeePadding.Vertical.Large,
            end = VocabeePadding.Horizontal.Medium,
            bottom = 14.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            VocabeeTopBar(
                navigationContainerColor = Color(0xFFF4EFE8),
                navigationContentColor = Color(0xFF8F857B),
                onNavigateBack = onBack,
                actions = listOf(
                    VocabeeTopBarAction.Text(
                        text = vocabeeString(VocabeeString.Done),
                        textColor = Color(0xFFE96C35),
                        action = { onDone(pendingLanguage) },
                    ),
                ),
            )
        }

        item {
            Text(
                text = vocabeeString(VocabeeString.LanguagePickerSelectLanguage),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF948A7D),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),
                color = Color(0xFF363029),
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(vocabeeString(VocabeeString.LanguagePickerSearchPlaceholder)) },
                leadingIcon = {
                    Text(
                        text = "⌕",
                        color = Color(0xFF9D9286),
                    )
                },
                shape = RoundedCornerShape(12.dp),
            )
        }

        if (recentLanguages.isNotEmpty()) {
            item {
                LanguageSection(
                    title = vocabeeString(VocabeeString.LanguagePickerRecent),
                    languages = recentLanguages,
                    selectedLanguage = pendingLanguage,
                    onSelect = { pendingLanguage = it },
                )
            }
        }

        if (otherLanguages.isNotEmpty()) {
            item {
                LanguageSection(
                    title = vocabeeString(VocabeeString.LanguagePickerAll),
                    languages = otherLanguages,
                    selectedLanguage = pendingLanguage,
                    onSelect = { pendingLanguage = it },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun LanguageSection(
    title: String,
    languages: List<LanguageOption>,
    selectedLanguage: LanguageOption,
    onSelect: (LanguageOption) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF948A7D),
            fontWeight = FontWeight.Bold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
        ) {
            Column {
                languages.forEachIndexed { index, option ->
                    LanguageOptionRow(
                        language = option,
                        selected = selectedLanguage.code == option.code,
                        onClick = { onSelect(option) },
                    )
                    if (index != languages.lastIndex) {
                        ProfileDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    language: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val languageName = localizedLanguageName(language)
    val nativeName = localizedLanguageNativeName(language)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = VocabeePadding.Horizontal.Small, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LanguageCodeMark(language = language)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = languageName,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF363029),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = nativeName,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8F857B),
            )
        }
        if (selected) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = Color(0xFFE96C35),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageBadge(
    language: LanguageOption,
    languageName: String? = null,
) {
    val resolvedLanguageName = languageName ?: localizedLanguageName(language)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LanguageCodeMark(language = language)
        Text(
            text = resolvedLanguageName,
            style = MaterialTheme.typography.labelMedium,
            color = if (language.code == "en") Color(0xFFE96C35) else Color(0xFF363029),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun LanguageCodeMark(language: LanguageOption) {
    Surface(
        modifier = Modifier
            .width(24.dp)
            .height(16.dp),
        shape = RoundedCornerShape(4.dp),
        color = language.markColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = language.shortName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private data class LanguageDisplayText(
    val name: String,
    val nativeName: String,
)

@Composable
private fun localizedLanguageName(language: LanguageOption): String {
    return when (language.code) {
        "uk" -> vocabeeString(VocabeeString.LanguageNameUkrainian)
        "en" -> vocabeeString(VocabeeString.LanguageNameEnglish)
        "ru" -> vocabeeString(VocabeeString.LanguageNameRussian)
        "pl" -> vocabeeString(VocabeeString.LanguageNamePolish)
        "de" -> vocabeeString(VocabeeString.LanguageNameGerman)
        "es" -> vocabeeString(VocabeeString.LanguageNameSpanish)
        else -> language.name
    }
}

@Composable
private fun localizedLanguageNativeName(language: LanguageOption): String {
    return when (language.code) {
        "uk" -> vocabeeString(VocabeeString.LanguageNativeNameUkrainian)
        "en" -> vocabeeString(VocabeeString.LanguageNativeNameEnglish)
        "ru" -> vocabeeString(VocabeeString.LanguageNativeNameRussian)
        "pl" -> vocabeeString(VocabeeString.LanguageNativeNamePolish)
        "de" -> vocabeeString(VocabeeString.LanguageNativeNameGerman)
        "es" -> vocabeeString(VocabeeString.LanguageNativeNameSpanish)
        else -> language.name
    }
}

private val LanguageOption.markColor: Color
    get() = when (code) {
        "uk" -> Color(0xFF4B7BEC)
        "en" -> Color(0xFFB44E63)
        "ru" -> Color(0xFF6E7B8B)
        "pl" -> Color(0xFFD64B4B)
        "de" -> Color(0xFF2F2D2C)
        "es" -> Color(0xFFE0A11A)
        else -> Color(0xFF8F857B)
    }

@Composable
private fun HeaderBlock(
    title: String,
    subtitle: String,
    accent: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                color = Ink,
                contentColor = Color.White,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = accent,
                    modifier = Modifier.padding(
                        horizontal = VocabeePadding.Horizontal.Small,
                        vertical = VocabeePadding.Vertical.Small,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SoftLine),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MissingTopicScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(VocabeePadding.Horizontal.Medium),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VocabeeTopBar(
            navigationContainerColor = Color(0xFFF4EFE8),
            navigationContentColor = Color(0xFF8F857B),
            onNavigateBack = onBack,
        )
        EmptyStateCard(
            title = vocabeeString(VocabeeString.TopicNotFoundTitle),
            subtitle = vocabeeString(VocabeeString.TopicNotFoundSubtitle),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTopicSheet(
    sourceLanguage: LanguageOption,
    targetLanguage: LanguageOption,
    onDismiss: () -> Unit,
    onCreate: (String, Int) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedCoverIndex by remember { mutableIntStateOf(1) }
    val previewTitle = title.ifBlank { vocabeeString(VocabeeString.NewTopicPreviewTitle) }
    val previewVisual = topicVisual(selectedCoverIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Paper,
        contentColor = Ink,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = VocabeePadding.Horizontal.Medium,
                    end = VocabeePadding.Horizontal.Medium,
                    bottom = VocabeePadding.Vertical.Large,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = vocabeeString(VocabeeString.NewTopicCancel),
                        color = Color(0xFF8F857B),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = { onCreate(title.trim(), selectedCoverIndex) },
                    enabled = title.isNotBlank(),
                ) {
                    Text(
                        text = vocabeeString(VocabeeString.NewTopicCreate),
                        color = if (title.isNotBlank()) Color(0xFFE96C35) else Color(0xFFCDBFB1),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = vocabeeString(VocabeeString.NewTopicTitle),
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                    color = Color(0xFF363029),
                    fontWeight = FontWeight.Normal,
                )
                Text(
                    text = vocabeeString(VocabeeString.NewTopicSubtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8F857B),
                )
            }

            TopicPreviewCard(
                title = previewTitle,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                visual = previewVisual,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(vocabeeString(VocabeeString.NewTopicPlaceholder)) },
                trailingIcon = {
                    if (title.isNotBlank()) {
                        Text(
                            text = "×",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { title = "" }
                                .padding(8.dp),
                            color = Color(0xFF8F857B),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = vocabeeString(VocabeeString.CoverColor),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                CoverColorPicker(
                    selectedIndex = selectedCoverIndex,
                    onSelect = { selectedCoverIndex = it },
                )
            }
        }
    }
}

@Composable
private fun TopicPreviewCard(
    title: String,
    sourceLanguage: LanguageOption,
    targetLanguage: LanguageOption,
    visual: TopicVisual,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(visual.background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(70.dp)
                .clip(CircleShape)
                .background(visual.bubble),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = wordCountLabel(0),
                style = MaterialTheme.typography.labelMedium,
                color = visual.accent.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                color = visual.accent,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${sourceLanguage.shortName} → ${targetLanguage.shortName}",
            modifier = Modifier.align(Alignment.TopEnd),
            style = MaterialTheme.typography.labelSmall,
            color = visual.accent.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CoverColorPicker(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        topicVisuals.chunked(7).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { columnIndex, visual ->
                    val index = rowIndex * 7 + columnIndex
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(visual.background)
                            .then(
                                if (selectedIndex == index) {
                                    Modifier.border(
                                        BorderStroke(2.dp, Color(0xFF4D5D43)),
                                        RoundedCornerShape(10.dp),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun wordCountLabel(count: Int): String {
    return vocabeeQuantityString(VocabeeQuantityString.WordCount, count, count)
}

@Composable
private fun topicUpdatedLabel(label: TopicUpdatedLabel): String {
    return when (label) {
        TopicUpdatedLabel.Today -> vocabeeString(VocabeeString.UpdatedToday)
        TopicUpdatedLabel.Yesterday -> vocabeeString(VocabeeString.UpdatedYesterday)
        is TopicUpdatedLabel.DaysAgo -> vocabeeQuantityString(VocabeeQuantityString.DaysAgo, label.count, label.count)
        is TopicUpdatedLabel.WeeksAgo -> vocabeeQuantityString(VocabeeQuantityString.WeeksAgo, label.count, label.count)
    }
}

@Composable
private fun translationOptionNote(note: TranslationOptionNote): String {
    return when (note) {
        TranslationOptionNote.Primary -> vocabeeString(VocabeeString.TranslationNotePrimary)
        TranslationOptionNote.Alternative -> vocabeeString(VocabeeString.TranslationNoteAlternative)
        TranslationOptionNote.Additional -> vocabeeString(VocabeeString.TranslationNoteAdditional)
        TranslationOptionNote.MlKitOnDevice -> vocabeeString(VocabeeString.TranslationNoteMlKitOnDevice)
        is TranslationOptionNote.AlreadyAdded -> {
            vocabeeString(VocabeeString.TranslationNoteAlreadyAdded, note.source)
        }
    }
}
