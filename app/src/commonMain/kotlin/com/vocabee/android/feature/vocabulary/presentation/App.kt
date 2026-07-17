@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.vocabee.android.core.presentation.designsystem.HoneycombWatermark
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.manropeFamily
import com.vocabee.android.core.presentation.designsystem.manropeTypography
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLogo
import com.vocabee.android.core.presentation.designsystem.languageFlag
import com.vocabee.android.core.presentation.designsystem.languageName
import com.vocabee.android.core.presentation.designsystem.prototypeTopicIcon
import com.vocabee.android.core.presentation.designsystem.prototypeTopicTheme
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApiException
import com.vocabee.android.feature.vocabulary.data.api.SyncResponse
import com.vocabee.android.feature.vocabulary.data.api.UpdateProfileRequest
import com.vocabee.android.feature.vocabulary.data.api.UserResponse
import com.vocabee.android.feature.vocabulary.data.preferences.InMemoryPreferencesManager
import com.vocabee.android.feature.vocabulary.data.preferences.PreferencesManager
import com.vocabee.android.feature.vocabulary.data.sync.toApplySyncRequest
import com.vocabee.android.feature.vocabulary.data.sync.toVocabularySyncSnapshot
import com.vocabee.android.feature.vocabulary.domain.model.DictionaryTopic
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import com.vocabee.android.feature.vocabulary.domain.model.TopicUpdatedLabel
import com.vocabee.android.feature.vocabulary.domain.model.TranslationOption
import com.vocabee.android.feature.vocabulary.domain.model.WordDetails
import com.vocabee.android.feature.vocabulary.domain.model.WordEntry
import com.vocabee.android.feature.vocabulary.domain.usecase.RemoteLexiconSearchUseCase
import com.vocabee.android.feature.vocabulary.presentation.navigation.AppTab
import com.vocabee.android.feature.vocabulary.presentation.navigation.VocabeeRoute
import com.vocabee.android.feature.vocabulary.presentation.navigation.selectedTabFor
import com.vocabee.android.feature.vocabulary.presentation.navigation.shouldShowBottomBar
import com.vocabee.android.feature.vocabulary.presentation.navigation.vocabeeSavedStateConfiguration
import com.vocabee.android.feature.vocabulary.presentation.platform.NoSpeechInputController
import com.vocabee.android.feature.vocabulary.presentation.platform.NoSpeechOutputController
import com.vocabee.android.feature.vocabulary.presentation.platform.GoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.GoogleAuthResult
import com.vocabee.android.feature.vocabulary.presentation.platform.NoGoogleAuthController
import com.vocabee.android.feature.vocabulary.presentation.platform.NoRewardedAdController
import com.vocabee.android.feature.vocabulary.presentation.platform.NoShareController
import com.vocabee.android.feature.vocabulary.presentation.platform.ShareController
import com.vocabee.android.feature.vocabulary.presentation.platform.RewardedAdController
import com.vocabee.android.feature.vocabulary.presentation.platform.RewardedAdResult
import com.vocabee.android.feature.vocabulary.presentation.platform.SpeechInputController
import com.vocabee.android.feature.vocabulary.presentation.platform.SpeechOutputController
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val VoicePressStartDelayMillis = 250L

internal enum class AppFlow { Splash, Onboarding, Auth, LanguageSelect, Main }

/**
 * Bridges the Add Word overlay's `searchRemote` callback to the gateway use case.
 * Falls back to local options when no API is plumbed (previews, tests) so the UI
 * still has something to render.
 */
/** Lightweight lookup for the in-sentence peek: top translation only. */
private suspend fun peekTranslateRemotely(
    useCase: RemoteLexiconSearchUseCase?,
    word: String,
    topic: DictionaryTopic,
): String? {
    if (useCase == null) return null
    val result = useCase(
        word,
        topic.targetLanguage.code,
        topic.sourceLanguage.code,
        emptySet(),
    )
    return (result as? RemoteLexiconSearchUseCase.Result.Ok)
        ?.options
        ?.firstOrNull()
        ?.value
        ?.takeIf { it.isNotBlank() }
}

/**
 * Бекфіл sense-збагачення для контекстного тренування: для груп слів із 2+
 * перекладами, де пари ще не мають речення власного значення, перепитуємо
 * лексикон (кеш — безкоштовно) і оновлюємо збережені details+senseIndex.
 * Повертає true, якщо хоч одне слово оновилось.
 */
private suspend fun backfillContextSenseDetails(
    useCase: RemoteLexiconSearchUseCase?,
    topics: List<DictionaryTopic>,
    updateWord: (topicId: String, wordId: String, ipa: String?, details: WordDetails?) -> Unit,
): Boolean {
    if (useCase == null) return false
    var updatedAny = false
    for (topic in topics) {
        val groups = topic.words
            .groupBy { it.source.trim().lowercase() }
            .values
            .filter { group -> group.size >= 2 }
        for (group in groups) {
            val hasAmbiguousSense = group
                .mapNotNull { member -> member.details?.senseIndex }
                .groupingBy { it }
                .eachCount()
                .any { (_, count) -> count > 1 }
            val needsEnrichment = group.any { member ->
                val details = member.details
                val ownSense = details?.senseIndex?.let { details.senses.getOrNull(it) }
                ownSense?.examples?.none { it.isNotBlank() } ?: true
            } || hasAmbiguousSense
            if (!needsEnrichment) continue
            val result = useCase(
                group.first().source.trim(),
                topic.targetLanguage.code,
                topic.sourceLanguage.code,
                emptySet(),
            ) as? RemoteLexiconSearchUseCase.Result.Ok ?: continue
            for (member in group) {
                val option = result.options.firstOrNull {
                    it.value.trim().lowercase() == member.translation.trim().lowercase()
                } ?: continue
                val newDetails = option.details ?: continue
                if (newDetails.senseIndex == null && member.details != null) continue
                updateWord(topic.id, member.id, option.ipa, newDetails)
                updatedAny = true
            }
        }
    }
    return updatedAny
}

private suspend fun searchRemotely(
    useCase: RemoteLexiconSearchUseCase?,
    input: String,
    topic: DictionaryTopic,
    speakLang: String,
    learnLang: String,
    onBeeBalanceChanged: (Int) -> Unit = {},
): AddWordSearchState {
    if (useCase == null) {
        return AddWordSearchState(query = input, isLoading = false, results = emptyList())
    }
    val existing = topic.words.map { it.translation }.toSet()
    return when (val result = useCase(input, speakLang, learnLang, existing)) {
        is RemoteLexiconSearchUseCase.Result.Ok -> {
            result.beeBalance?.let(onBeeBalanceChanged)
            AddWordSearchState(
                query = result.query,
                isLoading = false,
                results = result.options,
                tier = result.tier,
                maxResults = result.maxResults,
            )
        }
        is RemoteLexiconSearchUseCase.Result.Failure -> AddWordSearchState(
            query = result.query,
            isLoading = false,
            errorMessage = result.message,
        )
    }
}

private data class GoogleSignInOutcome(
    val user: UserResponse? = null,
    val errorMessage: String? = null,
)

private data class PendingSyncConflict(
    val user: UserResponse,
    val serverSnapshot: SyncResponse,
    val localWordCount: Int,
    val localProfile: LocalProfileSyncState,
)

private data class LocalProfileSyncState(
    val speakLang: String,
    val learnLang: String,
    val notificationsEnabled: Boolean,
    val darkThemeEnabled: Boolean,
)

private suspend fun signInWithGoogle(
    googleAuthController: GoogleAuthController,
    api: VocabeeApi?,
    store: VocabeeStore,
    speakLang: String,
    learnLang: String,
): GoogleSignInOutcome {
    return when (val result = googleAuthController.requestIdToken()) {
        GoogleAuthResult.NotConfigured -> {
            GoogleSignInOutcome(
                errorMessage = "Google вхід не налаштовано: додай vocabee.google.webClientId у local.properties і GOOGLE_CLIENT_ID на gateway.",
            )
        }
        GoogleAuthResult.Cancelled -> GoogleSignInOutcome(errorMessage = "Вхід через Google скасовано")
        is GoogleAuthResult.Failure -> GoogleSignInOutcome(errorMessage = result.message)
        is GoogleAuthResult.Success -> {
            val backend = api ?: return GoogleSignInOutcome(errorMessage = "API клієнт недоступний")
            try {
                backend.loginWithGoogle(
                    idToken = result.idToken,
                    speakLang = speakLang,
                    learnLang = learnLang,
                )
                val user = backend.currentUser()
                store.onEvent(
                    VocabeeEvent.ApplyAuthenticatedAccount(
                        userId = user.id,
                        displayName = user.displayName ?: user.email ?: "Vocabee user",
                        email = user.email ?: "Google account",
                        speakLang = user.speakLang,
                        learnLang = user.learnLang,
                        notificationsEnabled = user.notificationsEnabled,
                        darkThemeEnabled = user.darkThemeEnabled,
                        beeBalance = user.beeBalance,
                    ),
                )
                GoogleSignInOutcome(user = user)
            } catch (cause: VocabeeApiException) {
                GoogleSignInOutcome(
                    errorMessage = cause.errorMessage ?: "Не вдалося авторизуватись через Google",
                )
            } catch (cause: Exception) {
                GoogleSignInOutcome(
                    errorMessage = cause.message ?: "Не вдалося авторизуватись через Google",
                )
            }
        }
    }
}

internal sealed interface PrototypeSheet {
    data object CreateDictionary : PrototypeSheet
    data class DeleteDictionary(val dictionaryId: String) : PrototypeSheet
    data class LanguageForDictionary(val dictionaryId: String) : PrototypeSheet
    data class LanguageForProfile(val target: ProfileLanguageTarget) : PrototypeSheet
    data class NeedBees(val reason: BeeGateReason) : PrototypeSheet
    data class AuthRequired(val reason: AuthGateReason) : PrototypeSheet
    data object SyncConflict : PrototypeSheet
}

internal enum class BeeGateReason { DictionaryCreate, TranslationSearch }

internal enum class AuthGateReason { DictionaryLimit, WordLimit }

internal enum class ProfileLanguageTarget { Speaking, Learning }

@Composable
fun VocabeeApp(
    store: VocabeeStore = VocabeeStore(),
    speechInputController: SpeechInputController = NoSpeechInputController,
    speechOutputController: SpeechOutputController = NoSpeechOutputController,
    googleAuthController: GoogleAuthController = NoGoogleAuthController,
    rewardedAdController: RewardedAdController = NoRewardedAdController,
    shareController: ShareController = NoShareController,
    remoteLexiconSearch: RemoteLexiconSearchUseCase? = null,
    api: VocabeeApi? = null,
    preferencesManager: PreferencesManager = InMemoryPreferencesManager(),
    onExitApp: () -> Unit = {},
) {
    val state = store.state

    VocabeeTheme(darkTheme = state.darkThemeEnabled) {
        // First-launch gate: once the user finishes the onboarding → auth →
        // language picker chain we set `hasCompletedOnboarding = true` and
        // every subsequent launch goes Splash → Main, skipping all three.
        // The splash still plays on every launch as the visual intro.
        val skipFirstLaunchFlow = remember { preferencesManager.hasCompletedOnboarding }
        var flow by remember {
            mutableStateOf(AppFlow.Splash)
        }

        when (flow) {
            AppFlow.Splash -> SplashScreen(
                onDone = {
                    flow = if (skipFirstLaunchFlow) AppFlow.Main else AppFlow.Onboarding
                },
            )
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
                    // Persist the "we're done with first launch" flag so the
                    // next cold start jumps straight from splash to main.
                    preferencesManager.hasCompletedOnboarding = true
                    flow = AppFlow.Main
                },
            )
            AppFlow.Main -> MainApp(
                store = store,
                speechInputController = speechInputController,
                speechOutputController = speechOutputController,
                googleAuthController = googleAuthController,
                rewardedAdController = rewardedAdController,
                shareController = shareController,
                remoteLexiconSearch = remoteLexiconSearch,
                api = api,
                preferencesManager = preferencesManager,
                onExitApp = onExitApp,
            )
        }
    }
}

@Composable
private fun MainApp(
    store: VocabeeStore,
    speechInputController: SpeechInputController,
    speechOutputController: SpeechOutputController,
    googleAuthController: GoogleAuthController,
    rewardedAdController: RewardedAdController,
    shareController: ShareController,
    remoteLexiconSearch: RemoteLexiconSearchUseCase?,
    api: VocabeeApi?,
    preferencesManager: PreferencesManager,
    onExitApp: () -> Unit,
) {
    val state = store.state
    val scope = rememberCoroutineScope()
    val backStack = rememberNavBackStack(
        vocabeeSavedStateConfiguration,
        VocabeeRoute.DictionaryHome,
    )
    val currentRoute = backStack.lastOrNull() as? VocabeeRoute
    val selectedTab = selectedTabFor(currentRoute)
    var practiceSessionActive by remember { mutableStateOf(false) }
    val showBottomBar = shouldShowBottomBar(currentRoute, practiceSessionActive)

    var sheet by remember { mutableStateOf<PrototypeSheet?>(null) }
    var exitSheetVisible by remember { mutableStateOf(false) }
    var profileAuthNotice by remember { mutableStateOf<String?>(null) }
    var googleAuthLoading by remember { mutableStateOf(false) }
    var rewardAdLoading by remember { mutableStateOf(false) }
    var pendingSyncConflict by remember { mutableStateOf<PendingSyncConflict?>(null) }
    var practiceBottomPanelVisible by remember { mutableStateOf(false) }
    val appSnackbarHostState = remember { SnackbarHostState() }

    fun openRoot(route: VocabeeRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun applyServerSnapshot(snapshot: SyncResponse) {
        store.replaceCurrentSyncSnapshot(
            snapshot.toVocabularySyncSnapshot(store.state.supportedLanguages),
        )
        store.markCurrentVocabularySynced(snapshot.serverTime)
    }

    fun syncVocabularyNow(
        replaceServerState: Boolean = false,
        onDone: ((SyncResponse) -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
    ) {
        val backend = api ?: return
        if (store.state.account !is VocabeeAccountState.Authenticated) return
        scope.launch {
            try {
                val snapshot = store.exportCurrentSyncSnapshot(includeDeleted = true)
                val response = backend.applySync(snapshot.toApplySyncRequest(replaceServerState))
                applyServerSnapshot(response)
                onDone?.invoke(response)
            } catch (cause: VocabeeApiException) {
                onError?.invoke(cause.errorMessage ?: "Не вдалося синхронізувати словники")
            } catch (cause: Exception) {
                onError?.invoke(cause.message ?: "Не вдалося синхронізувати словники")
            }
        }
    }

    fun pushProfileSettings(profile: LocalProfileSyncState? = null) {
        val backend = api ?: return
        if (store.state.account !is VocabeeAccountState.Authenticated) return
        val profileState = profile ?: LocalProfileSyncState(
            speakLang = store.state.userLanguage.code,
            learnLang = store.state.learningLanguage.code,
            notificationsEnabled = store.state.notificationsEnabled,
            darkThemeEnabled = store.state.darkThemeEnabled,
        )
        scope.launch {
            try {
                val user = backend.updateCurrentUser(
                    UpdateProfileRequest(
                        speakLang = profileState.speakLang,
                        learnLang = profileState.learnLang,
                        notificationsEnabled = profileState.notificationsEnabled,
                        darkThemeEnabled = profileState.darkThemeEnabled,
                    ),
                )
                store.onEvent(
                    VocabeeEvent.ApplyAuthenticatedAccount(
                        userId = user.id,
                        displayName = user.displayName ?: user.email ?: "Vocabee user",
                        email = user.email ?: "Google account",
                        speakLang = user.speakLang,
                        learnLang = user.learnLang,
                        notificationsEnabled = user.notificationsEnabled,
                        darkThemeEnabled = user.darkThemeEnabled,
                        beeBalance = user.beeBalance,
                    ),
                )
            } catch (_: Exception) {
                // Keep the local revision dirty; startup sync will retry.
            }
        }
    }

    fun clearAuthenticatedSessionForAnotherEmail() {
        preferencesManager.accessToken = null
        preferencesManager.refreshToken = null
        preferencesManager.currentUserId = null
        pendingSyncConflict = null
        sheet = null
        store.signOutKeepLastUserState()
    }

    fun runStartupSync() {
        val backend = api ?: return
        if (preferencesManager.accessToken == null && preferencesManager.refreshToken == null) return
        scope.launch {
            try {
                val refreshToken = preferencesManager.refreshToken
                if (refreshToken != null) {
                    backend.refreshSession(refreshToken)
                }
                val user = backend.currentUser()
                store.onEvent(
                    VocabeeEvent.ApplyAuthenticatedAccount(
                        userId = user.id,
                        displayName = user.displayName ?: user.email ?: "Vocabee user",
                        email = user.email ?: "Google account",
                        speakLang = user.speakLang,
                        learnLang = user.learnLang,
                        notificationsEnabled = user.notificationsEnabled,
                        darkThemeEnabled = user.darkThemeEnabled,
                        beeBalance = user.beeBalance,
                    ),
                )
                if (preferencesManager.localRevisionEpochMillis > 0L) {
                    syncVocabularyNow()
                } else {
                    val since = preferencesManager.lastSyncAt
                    val delta = backend.syncTopics(since)
                    val hasRemoteChanges = since == null ||
                        delta.topics.isNotEmpty() ||
                        delta.words.isNotEmpty() ||
                        delta.deletedTopicIds.isNotEmpty() ||
                        delta.deletedWordIds.isNotEmpty()
                    if (hasRemoteChanges) {
                        applyServerSnapshot(if (since == null) delta else backend.syncTopics(null))
                    }
                }
            } catch (_: Exception) {
                // Stay local/offline; explicit login will surface errors.
            }
        }
    }

    fun startGoogleSignIn() {
        if (googleAuthLoading) return
        scope.launch {
            googleAuthLoading = true
            profileAuthNotice = null
            val hadLocalAnonymousVocabulary = store.hasLocalAnonymousVocabulary()
            val localAnonymousWordCount = store.localAnonymousWordCount()
            val localProfileBeforeSignIn = LocalProfileSyncState(
                speakLang = store.state.userLanguage.code,
                learnLang = store.state.learningLanguage.code,
                notificationsEnabled = store.state.notificationsEnabled,
                darkThemeEnabled = store.state.darkThemeEnabled,
            )
            val outcome = signInWithGoogle(
                googleAuthController = googleAuthController,
                api = api,
                store = store,
                speakLang = state.userLanguage.code,
                learnLang = state.learningLanguage.code,
            )
            profileAuthNotice = outcome.errorMessage
            googleAuthLoading = false
            val signedInUser = outcome.user
            if (signedInUser != null && api != null) {
                try {
                    val serverSnapshot = api.syncTopics(null)
                    val serverHasVocabulary = serverSnapshot.topics.isNotEmpty() ||
                        serverSnapshot.words.isNotEmpty()
                    if (hadLocalAnonymousVocabulary && serverHasVocabulary) {
                        pendingSyncConflict = PendingSyncConflict(
                            user = signedInUser,
                            serverSnapshot = serverSnapshot,
                            localWordCount = localAnonymousWordCount,
                            localProfile = localProfileBeforeSignIn,
                        )
                        sheet = PrototypeSheet.SyncConflict
                        appSnackbarHostState.showSnackbar("Потрібно обрати стан синхронізації")
                    } else {
                        if (hadLocalAnonymousVocabulary) {
                            store.moveAnonymousVocabularyToCurrentUser()
                            pushProfileSettings(localProfileBeforeSignIn)
                            syncVocabularyNow(replaceServerState = false) {
                                scope.launch {
                                    appSnackbarHostState.showSnackbar("Акаунт синхронізовано")
                                }
                            }
                        } else {
                            applyServerSnapshot(serverSnapshot)
                            appSnackbarHostState.showSnackbar("Акаунт синхронізовано")
                        }
                        sheet = null
                    }
                } catch (cause: Exception) {
                    val message = cause.message ?: "Не вдалося синхронізувати акаунт"
                    profileAuthNotice = message
                    appSnackbarHostState.showSnackbar(message)
                }
            } else {
                outcome.errorMessage?.let { appSnackbarHostState.showSnackbar(it) }
            }
        }
    }

    LaunchedEffect(Unit) {
        runStartupSync()
    }

    fun showRewardedAdForBees() {
        if (rewardAdLoading) return
        if (state.account !is VocabeeAccountState.Authenticated) {
            sheet = PrototypeSheet.AuthRequired(AuthGateReason.WordLimit)
            return
        }
        scope.launch {
            rewardAdLoading = true
            when (val result = rewardedAdController.showRewardedAd()) {
                RewardedAdResult.RewardEarned -> {
                    val backend = api
                    if (backend == null) {
                        appSnackbarHostState.showSnackbar("API клієнт недоступний")
                    } else {
                        try {
                            val user = backend.claimRewardedAdBees()
                            store.onEvent(VocabeeEvent.SetBeeBalance(user.beeBalance))
                            sheet = null
                            appSnackbarHostState.showSnackbar("+$RewardBeeAmount монеток на рахунку")
                        } catch (cause: VocabeeApiException) {
                            appSnackbarHostState.showSnackbar(
                                cause.errorMessage ?: "Не вдалося зарахувати монетки",
                            )
                        } catch (cause: Exception) {
                            appSnackbarHostState.showSnackbar(
                                cause.message ?: "Не вдалося зарахувати монетки",
                            )
                        }
                    }
                }
                RewardedAdResult.Dismissed -> {
                    appSnackbarHostState.showSnackbar("Рекламу закрито без винагороди")
                }
                is RewardedAdResult.Failed -> {
                    appSnackbarHostState.showSnackbar(result.message)
                }
            }
            rewardAdLoading = false
        }
    }

    // Centralised back handling: cascade through every dismissible layer before
    // the user can leave the app. Order = priority.
    //   1. A bottom sheet open       → close it
    //   2. Nav stack deeper than home → pop one entry
    //   3. At home (stack size = 1)  → show "exit Vocabee?" sheet
    // ModalBottomSheet (used by sheet/exitSheetVisible) installs its own
    // BackHandler internally which takes priority when visible, so this
    // BackHandler is only consulted when no sheet is open. We still handle
    // `sheet != null` here as a safety net.
    BackHandler {
        when {
            sheet != null -> sheet = null
            backStack.size > 1 -> backStack.removeLastOrNull()
            else -> exitSheetVisible = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                VocabeeBottomBar(
                    selectedTab = selectedTab,
                    backdropColor = if (
                        currentRoute == VocabeeRoute.Practice && practiceBottomPanelVisible
                    ) {
                        PrototypeColor.White
                    } else {
                        PrototypeColor.Background
                    },
                    onTabClick = { tab -> openRoot(tab.route) },
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = appSnackbarHostState) { data ->
                VocabeeSnackbar(data)
            }
        },
        containerColor = PrototypeColor.Background,
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
                            account = state.account,
                            beeBalance = state.beeBalance,
                            onBeeBannerClick = ::showRewardedAdForBees,
                            onAuthBannerClick = {
                                sheet = PrototypeSheet.AuthRequired(AuthGateReason.WordLimit)
                            },
                            onCreateClick = {
                                sheet = when {
                                    store.anonymousDictionaryLimitReached() ->
                                        PrototypeSheet.AuthRequired(AuthGateReason.DictionaryLimit)
                                    store.canCreateTopic() ->
                                        PrototypeSheet.CreateDictionary
                                    else ->
                                        PrototypeSheet.NeedBees(BeeGateReason.DictionaryCreate)
                                }
                            },
                            onTopicClick = { topicId ->
                                backStack.add(VocabeeRoute.TopicDetail(topicId))
                            },
                            onTopicDeleteRequest = { topic ->
                                sheet = PrototypeSheet.DeleteDictionary(topic.id)
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
                                initialQuery = route.initialQuery,
                                recentlyAddedWordId = state.recentlyAddedWordId,
                                onBack = { backStack.removeLastOrNull() },
                                onOpenLanguageSheet = {
                                    sheet = PrototypeSheet.LanguageForDictionary(topic.id)
                                },
                                speechInputController = speechInputController,
                                canUseTranslationSearch = store.canSearchTranslation(),
                                translationGateMessage = if (state.account is VocabeeAccountState.Authenticated) {
                                    "Потрібна 1 монетка для пошуку перекладу."
                                } else {
                                    "Гостьовий ліміт 50 слів вичерпано."
                                },
                                onSpendSearchBee = { store.spendTranslationBee() },
                                onTranslationSearchBlocked = {
                                    sheet = if (store.anonymousWordLimitReached()) {
                                        PrototypeSheet.AuthRequired(AuthGateReason.WordLimit)
                                    } else {
                                        PrototypeSheet.NeedBees(BeeGateReason.TranslationSearch)
                                    }
                                },
                                searchRemote = { input ->
                                    searchRemotely(
                                        useCase = remoteLexiconSearch,
                                        input = input,
                                        topic = topic,
                                        speakLang = state.userLanguage.code,
                                        learnLang = state.learningLanguage.code,
                                        onBeeBalanceChanged = { balance ->
                                            store.onEvent(VocabeeEvent.SetBeeBalance(balance))
                                        },
                                    )
                                },
                                onAddWord = { source, translation, ipa, details ->
                                    if (store.canAddWordToDictionary()) {
                                        store.onEvent(VocabeeEvent.AddWord(topic.id, source, translation, ipa, details))
                                        syncVocabularyNow()
                                    } else {
                                        sheet = PrototypeSheet.AuthRequired(AuthGateReason.WordLimit)
                                    }
                                },
                                onRemoveWord = { translation ->
                                    store.onEvent(VocabeeEvent.RemoveWord(topic.id, translation))
                                    syncVocabularyNow()
                                },
                                onSpeak = { text, languageTag ->
                                    speechOutputController.speak(text, languageTag)
                                },
                            )
                        }
                    }

                    entry<VocabeeRoute.Practice> {
                        PracticeScreen(
                            topics = state.topics,
                            onSessionActiveChanged = { active ->
                                practiceSessionActive = active
                            },
                            onBottomPanelVisibilityChanged = { visible ->
                                practiceBottomPanelVisible = visible
                            },
                            onSpeakWord = { word, languageTag ->
                                speechOutputController.speak(word, languageTag)
                            },
                            onAnswerWord = { topicId, wordId, deltaPercent ->
                                store.onEvent(
                                    VocabeeEvent.AdjustWordKnowledge(
                                        topicId = topicId,
                                        wordId = wordId,
                                        deltaPercent = deltaPercent,
                                    ),
                                )
                                syncVocabularyNow()
                            },
                            onRoundCompleted = { store.recordPracticeRoundCompleted() },
                        )
                    }

                    entry<VocabeeRoute.InviteFriends> {
                        InviteFriendsScreen(
                            api = api,
                            isAuthenticated = state.account is VocabeeAccountState.Authenticated,
                            refreshTokenProvider = { preferencesManager.refreshToken },
                            shareController = shareController,
                            onBack = { backStack.removeLastOrNull() },
                            onShowSnackbar = { message ->
                                scope.launch { appSnackbarHostState.showSnackbar(message) }
                            },
                        )
                    }

                    entry<VocabeeRoute.HelpSupport> {
                        HelpSupportScreen(
                            api = api,
                            isAuthenticated = state.account is VocabeeAccountState.Authenticated,
                            accountEmail = (state.account as? VocabeeAccountState.Authenticated)?.email,
                            onBack = { backStack.removeLastOrNull() },
                            onShowSnackbar = { message ->
                                scope.launch { appSnackbarHostState.showSnackbar(message) }
                            },
                        )
                    }

                    entry<VocabeeRoute.Settings> {
                        ProfileScreen(
                            topics = state.topics,
                            account = state.account,
                            userLanguage = state.userLanguage,
                            learningLanguage = state.learningLanguage,
                            notificationsEnabled = state.notificationsEnabled,
                            darkThemeEnabled = state.darkThemeEnabled,
                            streakDays = state.streakDays,
                            practiceRounds = state.practiceRounds,
                            authNotice = profileAuthNotice,
                            isGoogleAuthLoading = googleAuthLoading,
                            onGoogleSignInClick = ::startGoogleSignIn,
                            onNotificationsChanged = {
                                store.onEvent(VocabeeEvent.SetNotificationsEnabled(it))
                                pushProfileSettings()
                            },
                            onDarkThemeChanged = {
                                store.onEvent(VocabeeEvent.SetDarkThemeEnabled(it))
                                pushProfileSettings()
                            },
                            onLogoutClick = {
                                preferencesManager.accessToken = null
                                preferencesManager.refreshToken = null
                                store.signOutKeepLastUserState()
                            },
                            onSpeakingClick = { sheet = PrototypeSheet.LanguageForProfile(ProfileLanguageTarget.Speaking) },
                            onLearningClick = { sheet = PrototypeSheet.LanguageForProfile(ProfileLanguageTarget.Learning) },
                            onInviteClick = { backStack.add(VocabeeRoute.InviteFriends) },
                            onHelpClick = { backStack.add(VocabeeRoute.HelpSupport) },
                        )
                    }
                },
            )

            if (
                state.account is VocabeeAccountState.Authenticated &&
                state.beeBalance <= CriticalBeeThreshold &&
                currentRoute != VocabeeRoute.DictionaryHome
            ) {
                CriticalBeeBanner(
                    beeBalance = state.beeBalance,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    onClick = ::showRewardedAdForBees,
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
                onCreate = { title, coverIndex, iconIndex ->
                    store.onEvent(VocabeeEvent.CreateTopic(title = title, coverIndex = coverIndex, iconIndex = iconIndex))
                    syncVocabularyNow()
                    sheet = null
                    val createdTopic = store.state.topics.lastOrNull()
                    if (createdTopic != null) {
                        backStack.add(VocabeeRoute.TopicDetail(createdTopic.id))
                    }
                },
            )
            is PrototypeSheet.DeleteDictionary -> {
                val dict = state.topics.firstOrNull { it.id == s.dictionaryId }
                if (dict != null) {
                    DeleteDictionaryConfirmationSheet(
                        topic = dict,
                        onDismiss = { sheet = null },
                        onConfirm = {
                            store.onEvent(VocabeeEvent.RemoveTopic(dict.id))
                            syncVocabularyNow()
                            sheet = null
                            if ((backStack.lastOrNull() as? VocabeeRoute.TopicDetail)?.topicId == dict.id) {
                                backStack.removeLastOrNull()
                            }
                        },
                    )
                } else {
                    LaunchedEffect(s.dictionaryId) {
                        sheet = null
                    }
                }
            }
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
                            pushProfileSettings()
                        }
                        sheet = null
                    },
                )
            }
            is PrototypeSheet.NeedBees -> {
                BeeRewardSheet(
                    reason = s.reason,
                    beeBalance = state.beeBalance,
                    isLoading = rewardAdLoading,
                    onDismiss = { sheet = null },
                    onWatchAd = ::showRewardedAdForBees,
                )
            }
            is PrototypeSheet.AuthRequired -> {
                AuthRequiredSheet(
                    reason = s.reason,
                    topicCount = state.topics.size,
                    totalWords = store.totalWordCount(),
                    isLoading = googleAuthLoading,
                    onDismiss = { sheet = null },
                    onSignIn = ::startGoogleSignIn,
                )
            }
            PrototypeSheet.SyncConflict -> {
                val conflict = pendingSyncConflict
                if (conflict == null) {
                    LaunchedEffect(Unit) { sheet = null }
                } else {
                    SyncConflictSheet(
                        localWordCount = conflict.localWordCount,
                        serverWordCount = conflict.serverSnapshot.words.size,
                        isLoading = googleAuthLoading,
                        onDismiss = { },
                        onUseServer = {
                            applyServerSnapshot(conflict.serverSnapshot)
                            store.discardAnonymousVocabulary()
                            pendingSyncConflict = null
                            sheet = null
                            scope.launch { appSnackbarHostState.showSnackbar("Взято стан з бекенда") }
                        },
                        onUseLocal = {
                            googleAuthLoading = true
                            store.moveAnonymousVocabularyToCurrentUser()
                            pushProfileSettings(conflict.localProfile)
                            syncVocabularyNow(
                                replaceServerState = true,
                                onDone = {
                                    googleAuthLoading = false
                                    pendingSyncConflict = null
                                    sheet = null
                                    scope.launch { appSnackbarHostState.showSnackbar("Локальний стан залито на бекенд") }
                                },
                                onError = { message ->
                                    googleAuthLoading = false
                                    scope.launch { appSnackbarHostState.showSnackbar(message) }
                                },
                            )
                        },
                        onOtherEmail = {
                            clearAuthenticatedSessionForAnotherEmail()
                            scope.launch { appSnackbarHostState.showSnackbar("Увійди іншим Google акаунтом") }
                        },
                    )
                }
            }
        }

        if (exitSheetVisible) {
            ExitConfirmationSheet(
                onDismiss = { exitSheetVisible = false },
                onConfirm = {
                    exitSheetVisible = false
                    onExitApp()
                },
            )
        }
    }
}

@Composable
private fun VocabeeSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PrototypeColor.Snack,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = data.visuals.message,
                modifier = Modifier.weight(1f),
                color = PrototypeColor.SnackText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                lineHeight = 19.sp,
            )
            data.visuals.actionLabel?.let { actionLabel ->
                Text(
                    text = actionLabel,
                    modifier = Modifier.clickable { data.performAction() },
                    color = PrototypeColor.Yellow,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                )
            }
        }
    }
}

@Composable
private fun ExitConfirmationSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    PrototypeBottomSheet(
        title = "Закрити Vocabee?",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Прогрес збережено локально — нічого не загубиться.",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.NeutralSurface)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Закрити",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.Purple)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Залишитися",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun DeleteDictionaryConfirmationSheet(
    topic: DictionaryTopic,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val wordsCount = topic.words.size
    val wordsLabel = ukrainianPlural(wordsCount, "слово", "слова", "слів")
    PrototypeBottomSheet(
        title = "Видалити словник?",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "Словник «${topic.title}» і $wordsCount $wordsLabel у ньому буде видалено.",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Цю дію не можна скасувати.",
            color = PrototypeColor.RedText,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.NeutralSurface)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Скасувати",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.Red)
                    .clickable(onClick = onConfirm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Видалити",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun BeeRewardSheet(
    reason: BeeGateReason,
    beeBalance: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onWatchAd: () -> Unit,
) {
    val title = when (reason) {
        BeeGateReason.DictionaryCreate -> "Потрібні монетки"
        BeeGateReason.TranslationSearch -> "Отримай монетки"
    }
    val message = when (reason) {
        BeeGateReason.DictionaryCreate ->
            "Перші $FreeDictionaryLimit словники безкоштовні. Новий словник коштує $DictionaryCreationBeeCost монеток."
        BeeGateReason.TranslationSearch ->
            "Щоб отримувати переклади, потрібні монетки. 1 пошук слова або фрази коштує 1 монетку."
    }
    PrototypeBottomSheet(
        title = title,
        onDismiss = onDismiss,
    ) {
        BeeWalletBanner(
            beeBalance = beeBalance,
            critical = beeBalance <= CriticalBeeThreshold,
            onClick = onWatchAd,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = message,
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.NeutralSurface)
                    .clickable(enabled = !isLoading, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Пізніше",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.35f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.Purple)
                    .clickable(enabled = !isLoading, onClick = onWatchAd),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLoading) "Готую рекламу..." else "Відео за +$RewardBeeAmount",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AuthRequiredSheet(
    reason: AuthGateReason,
    topicCount: Int,
    totalWords: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
) {
    val message = when (reason) {
        AuthGateReason.DictionaryLimit ->
            "Без акаунта можна створити $FreeDictionaryLimit словники. Далі монетки і ліміти контролює бекенд, тому потрібно увійти через Google."
        AuthGateReason.WordLimit ->
            "Без акаунта можна зібрати $AnonymousFreeWordLimit слів. Щоб продовжити додавати слова і отримувати переклади за монетки, увійди через Google."
    }
    PrototypeBottomSheet(
        title = "Потрібен акаунт",
        onDismiss = onDismiss,
    ) {
        AnonymousFreeLimitBanner(
            topicCount = topicCount,
            totalWords = totalWords,
            critical = true,
            onClick = onSignIn,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = message,
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.NeutralSurface)
                    .clickable(enabled = !isLoading, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Пізніше",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1.35f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrototypeColor.Purple)
                    .clickable(enabled = !isLoading, onClick = onSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLoading) "Входимо..." else "Увійти Google",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SyncConflictSheet(
    localWordCount: Int,
    serverWordCount: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUseServer: () -> Unit,
    onUseLocal: () -> Unit,
    onOtherEmail: () -> Unit,
) {
    PrototypeBottomSheet(
        title = "Є два стани акаунта",
        onDismiss = onDismiss,
    ) {
        Text(
            text = "На телефоні є локальні слова, а на цьому Google акаунті вже є дані. Обери, який стан лишити.",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SyncStateCard(
                modifier = Modifier.weight(1f),
                icon = PrototypeIcon.Book,
                label = "На телефоні",
                wordCount = localWordCount,
            )
            PrototypeLineIcon(
                icon = PrototypeIcon.ArrowRight,
                modifier = Modifier.size(18.dp),
                color = PrototypeColor.Muted3,
                strokeWidth = 2f,
            )
            SyncStateCard(
                modifier = Modifier.weight(1f),
                icon = PrototypeIcon.Globe,
                label = "На бекенді",
                wordCount = serverWordCount,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SyncConflictAction(
                text = if (isLoading) "Заливаю..." else "Залити локальний стан",
                subtitle = "серверні дані буде перезаписано",
                kind = SyncActionKind.Primary,
                enabled = !isLoading,
                onClick = onUseLocal,
            )
            SyncConflictAction(
                text = "Взяти стан з бекенда",
                subtitle = "локальні $localWordCount ${ukrainianPlural(localWordCount, "слово", "слова", "слів")} буде видалено",
                kind = SyncActionKind.Neutral,
                enabled = !isLoading,
                onClick = onUseServer,
            )
            SyncConflictAction(
                text = "Увійти іншим email",
                subtitle = "нічого не зміниться",
                kind = SyncActionKind.Ghost,
                enabled = !isLoading,
                onClick = onOtherEmail,
            )
        }
    }
}

@Composable
private fun SyncStateCard(
    icon: PrototypeIcon,
    label: String,
    wordCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(PrototypeColor.Background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            PrototypeLineIcon(
                icon = icon,
                modifier = Modifier.size(15.dp),
                color = PrototypeColor.PurpleText,
                strokeWidth = 2f,
            )
            Text(
                text = label,
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.5.sp,
            )
        }
        Text(
            text = "$wordCount ${ukrainianPlural(wordCount, "слово", "слова", "слів")}",
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

private enum class SyncActionKind { Primary, Neutral, Ghost }

@Composable
private fun SyncConflictAction(
    text: String,
    subtitle: String,
    kind: SyncActionKind,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val background: Color
    val titleColor: Color
    val subColor: Color
    val border: BorderStroke?
    when (kind) {
        SyncActionKind.Primary -> {
            background = PrototypeColor.Purple
            titleColor = Color.White
            subColor = Color.White.copy(alpha = 0.75f)
            border = null
        }
        SyncActionKind.Neutral -> {
            background = PrototypeColor.NeutralSurface
            titleColor = PrototypeColor.Ink
            subColor = PrototypeColor.Muted
            border = null
        }
        SyncActionKind.Ghost -> {
            background = Color.Transparent
            titleColor = PrototypeColor.Muted
            subColor = PrototypeColor.Muted
            border = BorderStroke(1.5.dp, PrototypeColor.Line)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .let { if (border != null) it.border(border, shape) else it }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = text,
            color = titleColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.5.sp,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            color = subColor,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun VocabeeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    PrototypeColor.useDarkTheme(darkTheme)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = PrototypeColor.Purple,
            onPrimary = Color.White,
            secondary = PrototypeColor.Blue,
            onSecondary = Color.White,
            tertiary = PrototypeColor.Yellow,
            onTertiary = PrototypeColor.YellowText,
            background = PrototypeColor.Background,
            onBackground = PrototypeColor.Ink,
            surface = PrototypeColor.White,
            onSurface = PrototypeColor.Ink,
            surfaceVariant = PrototypeColor.Line2,
            onSurfaceVariant = PrototypeColor.Muted,
            outline = PrototypeColor.Line,
        )
    } else {
        lightColorScheme(
            primary = PrototypeColor.Purple,
            onPrimary = Color.White,
            secondary = PrototypeColor.Blue,
            onSecondary = Color.White,
            tertiary = PrototypeColor.Yellow,
            onTertiary = PrototypeColor.YellowText,
            background = PrototypeColor.Background,
            onBackground = PrototypeColor.Ink,
            surface = PrototypeColor.White,
            onSurface = PrototypeColor.Ink,
            surfaceVariant = PrototypeColor.Line2,
            onSurfaceVariant = PrototypeColor.Muted,
            outline = PrototypeColor.Line,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = manropeTypography(),
        content = content,
    )
}

/* ============================================================
 * Dictionaries home
 * ============================================================ */

@Composable
private fun DictionariesHomeScreen(
    topics: List<DictionaryTopic>,
    account: VocabeeAccountState,
    beeBalance: Int,
    onBeeBannerClick: () -> Unit,
    onAuthBannerClick: () -> Unit,
    onCreateClick: () -> Unit,
    onTopicClick: (String) -> Unit,
    onTopicDeleteRequest: (DictionaryTopic) -> Unit,
) {
    val totalWords = topics.sumOf { it.words.size }
    val isAnonymous = account is VocabeeAccountState.Anonymous
    val anonymousLimitCritical = topics.size >= FreeDictionaryLimit || totalWords >= AnonymousFreeWordLimit
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background),
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

            item(span = { GridItemSpan(maxLineSpan) }) {
                if (isAnonymous) {
                    AnonymousFreeLimitBanner(
                        topicCount = topics.size,
                        totalWords = totalWords,
                        critical = anonymousLimitCritical,
                        onClick = onAuthBannerClick,
                    )
                } else {
                    BeeWalletBanner(
                        beeBalance = beeBalance,
                        critical = beeBalance <= CriticalBeeThreshold,
                        onClick = onBeeBannerClick,
                    )
                }
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
                        onDeleteRequest = { onTopicDeleteRequest(topic) },
                    )
                }
            }
        }

        if (topics.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 14.dp)
                    .size(62.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(PrototypeColor.Purple)
                    .clickable(onClick = onCreateClick),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Plus,
                    modifier = Modifier.size(26.dp),
                    color = Color.White,
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
                    text = "Словники",
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
                MetricText(value = topicCount.toString(), unit = ukPlural(topicCount, "словник", "словники", "словників"))
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(PrototypeColor.Muted2))
                MetricText(value = totalWords.toString(), unit = "${ukPlural(totalWords, "слово", "слова", "слів")} зібрано")
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
private fun AnonymousFreeLimitBanner(
    topicCount: Int,
    totalWords: Int,
    critical: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (critical) PrototypeColor.OrangeText else PrototypeColor.PurpleText
    val background = if (critical) PrototypeColor.NotePeach else PrototypeColor.Tint
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(1.4.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = 0.14f),
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.User,
                    modifier = Modifier.padding(10.dp).size(22.dp),
                    color = accent,
                    strokeWidth = 2.1f,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (critical) "Ліміт гостя вичерпано" else "Гостьовий режим",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Словники ${topicCount.coerceAtMost(FreeDictionaryLimit)}/$FreeDictionaryLimit · слова ${totalWords.coerceAtMost(AnonymousFreeWordLimit)}/$AnonymousFreeWordLimit. Далі потрібен Google акаунт.",
                    modifier = Modifier.padding(top = 3.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                )
            }
            PrototypeLineIcon(
                icon = PrototypeIcon.ChevronRight,
                modifier = Modifier.size(22.dp),
                color = accent,
                strokeWidth = 2.2f,
            )
        }
    }
}

@Composable
private fun BeeWalletBanner(
    beeBalance: Int,
    critical: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (critical) PrototypeColor.NotePeach else PrototypeColor.Tint
    val accent = if (critical) PrototypeColor.OrangeText else PrototypeColor.PurpleText
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(1.4.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            BeeBalanceBadge(beeBalance = beeBalance, accent = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (critical) "Монетки майже закінчились" else "Переклади за монетки",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "1 пошук коштує 1 монетку. Подивись відео і отримай +$RewardBeeAmount.",
                    modifier = Modifier.padding(top = 3.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                )
            }
            PrototypeLineIcon(
                icon = PrototypeIcon.ChevronRight,
                modifier = Modifier.size(22.dp),
                color = accent,
                strokeWidth = 2.2f,
            )
        }
    }
}

@Composable
private fun CriticalBeeBanner(
    beeBalance: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = PrototypeColor.NotePeach,
        border = BorderStroke(1.4.dp, PrototypeColor.OrangeText.copy(alpha = 0.38f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            BeeBalanceBadge(beeBalance = beeBalance, accent = PrototypeColor.OrangeText)
            Text(
                text = "Лишилось $beeBalance ${ukrainianPlural(beeBalance, "монетка", "монетки", "монеток")}. Відео дасть +$RewardBeeAmount.",
                modifier = Modifier.weight(1f),
                color = PrototypeColor.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun BeeBalanceBadge(
    beeBalance: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = accent.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Sparkle,
                modifier = Modifier.size(16.dp),
                color = accent,
                strokeWidth = 2.1f,
            )
            Text(
                text = beeBalance.toString(),
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun SwipeRevealDeleteContainer(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    deleteButtonWidth: Dp,
    onDeleteClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val deleteButtonWidthPx = with(density) { deleteButtonWidth.toPx() }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun settleTo(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            val animatable = Animatable(offsetPx)
            animatable.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 170),
            ) {
                offsetPx = value.coerceIn(-deleteButtonWidthPx, 0f)
            }
            offsetPx = target
        }
    }

    LaunchedEffect(deleteButtonWidthPx) {
        offsetPx = offsetPx.coerceIn(-deleteButtonWidthPx, 0f)
    }

    Box(
        modifier = modifier.clip(shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(PrototypeColor.Red.copy(alpha = 0.96f)),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(deleteButtonWidth)
                    .fillMaxHeight()
                    .clickable {
                        settleJob?.cancel()
                        offsetPx = 0f
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Close,
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.4f,
                    )
                    Text(
                        text = "Видалити",
                        modifier = Modifier.padding(top = 5.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer { translationX = offsetPx }
                .pointerInput(deleteButtonWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                        val dragStart = awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                            val shouldStartReveal = overSlop < 0f || offsetPx < 0f
                            if (shouldStartReveal) {
                                settleJob?.cancel()
                                offsetPx = (offsetPx + overSlop).coerceIn(-deleteButtonWidthPx, 0f)
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            }
                        }

                        if (dragStart != null) {
                            horizontalDrag(dragStart.id) { change ->
                                val delta = change.positionChange().x
                                offsetPx = (offsetPx + delta).coerceIn(-deleteButtonWidthPx, 0f)
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                change.consume()
                            }
                            val velocity = velocityTracker.calculateVelocity().x
                            val target = when {
                                velocity < -320f -> -deleteButtonWidthPx
                                velocity > 320f -> 0f
                                offsetPx <= -deleteButtonWidthPx * 0.42f -> -deleteButtonWidthPx
                                else -> 0f
                            }
                            settleTo(target)
                        }
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
private fun DictionaryCard(
    topic: DictionaryTopic,
    onClick: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    val theme = prototypeTopicTheme(topic.coverIndex)
    val knowledgePercent = topic.words.averageKnowledgePercent()
    SwipeRevealDeleteContainer(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp),
        shape = RoundedCornerShape(24.dp),
        deleteButtonWidth = 76.dp,
        onDeleteClick = onDeleteRequest,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.color)
                .clickable(onClick = onClick),
        ) {
            KnowledgeBackgroundFill(
                percent = knowledgePercent,
                color = PrototypeColor.CardKnowledgeFill,
            )
            HoneycombWatermark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 26.dp, y = (-26).dp)
                    .size(120.dp),
                // Літерали до кінця карти: заливка — константний колір словника,
                // а токен White у темній темі фліпає в темну поверхню.
                color = Color.White.copy(alpha = 0.16f),
            )

            Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).size(32.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = Color.White.copy(alpha = 0.22f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLineIcon(
                            icon = prototypeTopicIcon(topic.iconIndex),
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2f,
                        )
                    }
                }
                if (topic.updatedLabel is TopicUpdatedLabel.Today) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
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
                        color = Color.White,
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
                            color = Color.White.copy(alpha = 0.22f),
                        ) {
                            Text(
                                text = "${topic.words.size} слів",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            )
                        }
                        if (topic.updatedLabel !is TopicUpdatedLabel.Today) {
                            Text(
                                text = updatedLabelText(topic.updatedLabel),
                                color = Color.White.copy(alpha = 0.82f),
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
}

private fun updatedLabelText(label: TopicUpdatedLabel): String = when (label) {
    TopicUpdatedLabel.Today -> "сьогодні"
    TopicUpdatedLabel.Yesterday -> "вчора"
    is TopicUpdatedLabel.DaysAgo -> "${label.count} ${ukrainianPlural(label.count, "день", "дні", "днів")} тому"
    is TopicUpdatedLabel.WeeksAgo -> "${label.count} ${ukrainianPlural(label.count, "тиждень", "тижні", "тижнів")} тому"
}

internal fun ukrainianPlural(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    if (mod100 in 11..14) return many
    return when (count % 10) {
        1 -> one
        2, 3, 4 -> few
        else -> many
    }
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
                color = Color.White,
                strokeWidth = 2.2f,
            )
            Text(
                text = "Створити словник",
                color = Color.White,
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
    initialQuery: String? = null,
    recentlyAddedWordId: String?,
    onBack: () -> Unit,
    onOpenLanguageSheet: () -> Unit,
    speechInputController: SpeechInputController,
    canUseTranslationSearch: Boolean,
    translationGateMessage: String,
    onSpendSearchBee: () -> Boolean,
    onTranslationSearchBlocked: () -> Unit,
    searchRemote: suspend (query: String) -> AddWordSearchState,
    onAddWord: (source: String, translation: String, ipa: String?, details: com.vocabee.android.feature.vocabulary.domain.model.WordDetails?) -> Unit,
    onRemoveWord: (translation: String) -> Unit,
    onSpeak: (text: String, languageTag: String) -> Unit,
) {
    val accent = prototypeTopicTheme(topic.coverIndex).color
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by remember(topic.id) { mutableStateOf(initialQuery.orEmpty()) }
    val cleanedQuery = query.trim()
    var searchState by remember(topic.id) { mutableStateOf(AddWordSearchState()) }
    var partialText by remember(topic.id) { mutableStateOf("") }
    var isListening by remember(topic.id) { mutableStateOf(false) }
    var speechError by remember(topic.id) { mutableStateOf<String?>(null) }
    var ignoreSpeechCallbacks by remember(topic.id) { mutableStateOf(false) }
    var inputFocused by remember(topic.id) { mutableStateOf(false) }
    var inputBarHeight by remember(topic.id) { mutableStateOf(58.dp) }
    var speechDirectionReversed by remember(topic.id, topic.sourceLanguage.code, topic.targetLanguage.code) {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()
    val voiceSnackbarHostState = remember { SnackbarHostState() }
    // Compute groups at the screen scope so the `remember` lives in a
    // @Composable context (LazyListScope.else { … } below is not Composable).
    val wordGroups = remember(topic.words) { topic.words.groupBySourceWord() }
    val existingTranslations = remember(topic.words) {
        topic.words.map { it.translation.trim().lowercase() }.toSet()
    }
    val showTranslationPanel =
        cleanedQuery.isNotBlank() ||
            searchState.isLoading ||
            searchState.errorMessage != null ||
            speechError != null
    val speechInputLanguage = if (speechDirectionReversed) topic.targetLanguage else topic.sourceLanguage
    val speechOutputLanguage = if (speechDirectionReversed) topic.sourceLanguage else topic.targetLanguage
    val panelContentTopPadding = with(density) { WindowInsets.statusBars.getTop(this).toDp() } + 12.dp
    val panelTopPadding = 0.dp
    val imeBottom = with(density) { WindowInsets.ime.getBottom(this).toDp() }
    val navigationBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val keyboardVisible = imeBottom > navigationBottom + 24.dp
    val inputToPanelBottomGap = if (keyboardVisible) 10.dp else 16.dp
    val inputBottomPadding = if (keyboardVisible) {
        imeBottom + inputToPanelBottomGap
    } else {
        navigationBottom + inputToPanelBottomGap
    }
    val panelBottomPadding = if (keyboardVisible) imeBottom else navigationBottom
    val wordListBottomPadding = inputBottomPadding + inputBarHeight + 8.dp
    val inputReservedHeight = inputBarHeight + inputToPanelBottomGap + 12.dp
    val inputBackdropHeight = inputBottomPadding + inputBarHeight + 60.dp
    val showCancelButton = !isListening && (showTranslationPanel || keyboardVisible || inputFocused)
    val detailListState = rememberLazyListState()
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val detailHeaderExpandedHeight = statusBarTop + 148.dp
    val detailHeaderCompactHeight = statusBarTop + 64.dp
    val detailHeaderCollapseDistancePx = with(density) {
        (detailHeaderExpandedHeight - detailHeaderCompactHeight).toPx().coerceAtLeast(1f)
    }
    val detailHeaderCollapseFraction by remember(detailHeaderCollapseDistancePx) {
        derivedStateOf {
            val rawOffset = if (detailListState.firstVisibleItemIndex > 0) {
                detailHeaderCollapseDistancePx
            } else {
                detailListState.firstVisibleItemScrollOffset.toFloat()
            }
            (rawOffset / detailHeaderCollapseDistancePx).coerceIn(0f, 1f)
        }
    }

    DisposableEffect(speechInputController) {
        onDispose { speechInputController.stopListening() }
    }

    LaunchedEffect(cleanedQuery) {
        if (cleanedQuery.isEmpty()) {
            searchState = AddWordSearchState()
            return@LaunchedEffect
        }
        speechError = null
        searchState = searchState.copy(query = cleanedQuery, isLoading = true, errorMessage = null)
        val requestQuery = cleanedQuery
        delay(700)
        if (query.trim() != requestQuery) return@LaunchedEffect
        if (!onSpendSearchBee()) {
            searchState = AddWordSearchState(
                query = requestQuery,
                errorMessage = translationGateMessage,
            )
            onTranslationSearchBlocked()
            return@LaunchedEffect
        }
        val result = searchRemote(requestQuery)
        if (query.trim() == requestQuery) {
            searchState = result
        }
    }

    fun resetSpeech() {
        partialText = ""
        speechError = null
    }

    fun showVoiceInterruptedSnack(reason: String) {
        scope.launch {
            voiceSnackbarHostState.currentSnackbarData?.dismiss()
            voiceSnackbarHostState.showSnackbar(
                message = "Голосове введення перервано: $reason",
            )
        }
    }

    fun startListening() {
        if (!canUseTranslationSearch) {
            onTranslationSearchBlocked()
            return
        }
        ignoreSpeechCallbacks = false
        inputFocused = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        resetSpeech()
        query = ""
        speechInputController.startListening(
            languageTag = speechInputLanguage.speechTag,
            alternativeLanguageTags = listOf(speechOutputLanguage.speechTag),
            onPartialResult = {
                if (!ignoreSpeechCallbacks) partialText = it
            },
            onResult = { recognized ->
                if (ignoreSpeechCallbacks) return@startListening
                val text = recognized.trim()
                partialText = ""
                isListening = false
                if (text.isNotBlank()) query = text
            },
            onError = { message ->
                partialText = ""
                isListening = false
                if (!ignoreSpeechCallbacks) {
                    speechError = message
                    showVoiceInterruptedSnack(message)
                }
            },
            onListeningChanged = { isListening = it },
        )
    }

    suspend fun stopListeningWithGrace() {
        delay(700)
        if (isListening) {
            speechInputController.stopListening()
        }
    }

    fun cancelSearch() {
        if (isListening) {
            showVoiceInterruptedSnack("скасовано користувачем")
        }
        ignoreSpeechCallbacks = true
        query = ""
        searchState = AddWordSearchState()
        isListening = false
        inputFocused = false
        resetSpeech()
        speechInputController.stopListening()
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(enabled = showTranslationPanel || isListening || inputFocused || keyboardVisible) {
        cancelSearch()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = detailListState,
            contentPadding = PaddingValues(
                top = detailHeaderExpandedHeight + 10.dp,
                bottom = wordListBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (topic.words.isEmpty()) {
                item {
                    DetailEmptyState(
                        accent = accent,
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                    )
                }
            } else {
                // Group entries by source word so multiple translations of the
                // same English word collapse into one card. `wordGroups` is
                // computed at the screen scope (above) because LazyListScope
                // isn't a Composable context — `remember` can't run here.
                items(wordGroups, key = { it.anyId }) { group ->
                    WordGroupRow(
                        group = group,
                        accent = accent,
                        highlighted = group.entries.any { it.id == recentlyAddedWordId },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onSpeak = { onSpeak(group.sourceWord, topic.sourceLanguage.speechTag) },
                        onRemove = {
                            group.translations.forEach(onRemoveWord)
                        },
                    )
                }
            }
        }

        DetailHeader(
            topic = topic,
            accent = accent,
            collapseFraction = detailHeaderCollapseFraction,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onBack,
            speechInputLanguage = speechInputLanguage,
            speechOutputLanguage = speechOutputLanguage,
            onToggleSpeechDirection = {
                speechDirectionReversed = !speechDirectionReversed
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = panelTopPadding,
                    bottom = panelBottomPadding,
                ),
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showTranslationPanel,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            ) {
                InlineTranslationPanel(
                    modifier = Modifier.fillMaxSize(),
                    query = cleanedQuery.ifBlank { partialText },
                    searchState = searchState,
                    speechError = speechError,
                    accent = accent,
                    existingTranslations = existingTranslations,
                    inputReservedHeight = inputReservedHeight,
                    contentTopPadding = panelContentTopPadding,
                    onAdd = { option ->
                        onAddWord(option.learningWord, option.value, option.ipa, option.details)
                    },
                    onRemove = { option ->
                        onRemoveWord(option.value)
                    },
                )
            }
        }
        InputDockBackdrop(
            modifier = Modifier.align(Alignment.BottomCenter),
            height = inputBackdropHeight,
        )
        SnackbarHost(
            hostState = voiceSnackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = inputBottomPadding + 76.dp,
                ),
        )
        InlineAddWordBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = inputBottomPadding,
                ),
            value = query,
            isListening = isListening,
            showCancel = showCancelButton,
            accent = accent,
            onValueChange = { value ->
                if (value.isNotBlank() && !canUseTranslationSearch) {
                    onTranslationSearchBlocked()
                    query = ""
                    return@InlineAddWordBar
                }
                query = value
                if (value.isNotBlank()) resetSpeech()
            },
            onFocusChanged = { focused -> inputFocused = focused },
            onHeightChanged = { height ->
                if (height > 0.dp && height != inputBarHeight) {
                    inputBarHeight = height
                }
            },
            onCancel = ::cancelSearch,
            onStartListening = ::startListening,
            onStopListening = { scope.launch { stopListeningWithGrace() } },
        )
    }
}

@Composable
private fun InputDockBackdrop(
    modifier: Modifier = Modifier,
    height: Dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.32f to PrototypeColor.Background.copy(alpha = 0.74f),
                        1f to PrototypeColor.Background.copy(alpha = 0.96f),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(112.dp)
                .background(PrototypeColor.White.copy(alpha = 0.52f))
                .blur(18.dp),
        )
    }
}

@Composable
private fun InlineAddWordBar(
    modifier: Modifier = Modifier,
    value: String,
    isListening: Boolean,
    showCancel: Boolean,
    accent: Color,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onHeightChanged: (Dp) -> Unit,
    onCancel: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onHeightChanged(with(density) { coordinates.size.height.toDp() })
            },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (isListening) {
                        Modifier.height(58.dp)
                    } else {
                        Modifier.heightIn(min = 58.dp, max = 150.dp)
                    },
                )
                .clip(RoundedCornerShape(18.dp))
                .background(PrototypeColor.FieldBg)
                .border(BorderStroke(1.5.dp, PrototypeColor.Line), RoundedCornerShape(18.dp))
                .clickable(enabled = !isListening) { focusRequester.requestFocus() }
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    VoiceWaveform(
                        modifier = Modifier.fillMaxWidth(),
                        height = 32.dp,
                        barCount = 24,
                        barWidth = 4.dp,
                        barSpacing = 3.dp,
                        minBarHeight = 5.dp,
                        maxBarHeight = 28.dp,
                    )
                }
            } else {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Search,
                    modifier = Modifier.size(19.dp),
                    color = PrototypeColor.Muted2,
                    strokeWidth = 2f,
                )
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                        .focusRequester(focusRequester),
                    singleLine = false,
                    maxLines = 5,
                    textStyle = TextStyle(
                fontFamily = manropeFamily(),
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PrototypeColor.PurpleText),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "Введи слово або фразу…",
                                    color = PrototypeColor.Muted2,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(58.dp)
                .then(
                    if (showCancel) {
                        Modifier.clickable(onClick = onCancel)
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    coroutineScope {
                                        var started = false
                                        val startJob = launch {
                                            delay(VoicePressStartDelayMillis)
                                            started = true
                                            onStartListening()
                                        }
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            if (started) {
                                                onStopListening()
                                            } else {
                                                startJob.cancel()
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    },
                ),
            shape = RoundedCornerShape(18.dp),
            color = when {
                showCancel -> PrototypeColor.Ink
                isListening -> PrototypeColor.Orange
                else -> accent
            },
            shadowElevation = if (showCancel || isListening) 10.dp else 5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                PrototypeLineIcon(
                    icon = if (showCancel) PrototypeIcon.Close else PrototypeIcon.Mic,
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2f,
                )
            }
        }
    }
}

@Composable
private fun InlineTranslationPanel(
    modifier: Modifier = Modifier,
    query: String,
    searchState: AddWordSearchState,
    speechError: String?,
    accent: Color,
    existingTranslations: Set<String>,
    inputReservedHeight: androidx.compose.ui.unit.Dp,
    contentTopPadding: Dp,
    onAdd: (TranslationOption) -> Unit,
    onRemove: (TranslationOption) -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 132.dp),
        shape = RoundedCornerShape(0.dp),
        color = PrototypeColor.White,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = contentTopPadding, end = 24.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (query.isBlank()) "Голосове введення" else query,
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Варіанти перекладу",
                        modifier = Modifier.padding(top = 2.dp),
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.5.sp,
                    )
                }
                PrototypeLineIcon(
                    icon = PrototypeIcon.Sparkle,
                    modifier = Modifier.size(17.dp),
                    color = PrototypeColor.PurpleText,
                    strokeWidth = 1.8f,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(bottom = inputReservedHeight),
            ) {
                when {
                    speechError != null && query.isBlank() -> InlinePanelMessage(
                        title = "Не вдалося розпізнати",
                        message = speechError,
                        accent = accent,
                    )
                    searchState.isLoading -> AddWordLoadingState(accent = accent)
                    searchState.errorMessage != null -> AddWordErrorState(
                        message = searchState.errorMessage,
                        accent = accent,
                    )
                    else -> AddWordResultsList(
                        query = query,
                        results = searchState.results,
                        tier = searchState.tier,
                        maxResults = searchState.maxResults,
                        accent = accent,
                        existingTranslations = existingTranslations,
                        onAdd = onAdd,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlinePanelMessage(
    title: String,
    message: String,
    accent: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PrototypeLineIcon(
            icon = PrototypeIcon.Mic,
            modifier = Modifier.size(24.dp),
            color = accent.copy(alpha = 0.75f),
            strokeWidth = 2f,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = 10.dp),
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 5.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailHeader(
    topic: DictionaryTopic,
    accent: Color,
    collapseFraction: Float,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    speechInputLanguage: LanguageOption,
    speechOutputLanguage: LanguageOption,
    onToggleSpeechDirection: () -> Unit,
) {
    val density = LocalDensity.current
    val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val compactHeight = statusBarTop + 64.dp
    val expandedHeight = statusBarTop + 148.dp
    val progress = collapseFraction.coerceIn(0f, 1f)
    val headerHeight = compactHeight + (expandedHeight - compactHeight) * (1f - progress)
    val subtitleAlpha = (1f - progress * 1.45f).coerceIn(0f, 1f)
    val titleStart = 18.dp + 52.dp * progress
    val titleEnd = 18.dp + 116.dp * progress
    val titleTop = statusBarTop + 62.dp - 51.dp * progress
    val subtitleTop = statusBarTop + 103.dp - 14.dp * progress
    val titleFontSize = (28f - 10f * progress).sp
    val titleLineHeight = (32f - 10f * progress).sp
    val titleLetterSpacing = (-0.56f + 0.20f * progress).sp

    Surface(
        modifier = modifier.fillMaxWidth().height(headerHeight),
        color = accent,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = if (progress > 0.02f) 8.dp else 0.dp,
    ) {
        Box {
            HoneycombWatermark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-10).dp)
                    .size(120.dp),
                // Літерали до кінця хедера: заливка — константний колір словника.
                color = Color.White.copy(alpha = 0.18f),
            )
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 2.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = 0.18f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.ChevronLeft,
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.2f,
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(13.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier.clickable(onClick = onToggleSpeechDirection),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(languageFlag(speechInputLanguage.code), fontSize = 15.sp)
                        PrototypeLineIcon(
                            icon = PrototypeIcon.ArrowRight,
                            modifier = Modifier.size(13.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            strokeWidth = 2f,
                        )
                        Text(languageFlag(speechOutputLanguage.code), fontSize = 15.sp)
                    }
                }
            }
            Text(
                text = topic.title,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = titleTop)
                    .padding(start = titleStart, end = titleEnd)
                    .fillMaxWidth(),
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = titleFontSize,
                letterSpacing = titleLetterSpacing,
                lineHeight = titleLineHeight,
                maxLines = if (progress < 0.45f) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${topic.words.size} слів · ${updatedLabelText(topic.updatedLabel)}",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = subtitleTop)
                    .padding(start = 18.dp, end = 18.dp)
                    .graphicsLayer { alpha = subtitleAlpha },
                color = Color.White.copy(alpha = 0.82f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
            )
        }
    }
}

/**
 * Grouped card for one English source word with its 1+ translations stacked
 * beneath. Replaces the per-row [WordRow] in [DictionaryDetailScreen] so 3
 * translations of "play" render as one expandable card instead of three
 * cards with duplicate dictionary blocks.
 *
 * The IPA and details (senses, examples, syn/ant, forms) are pulled from
 * whichever entry in the group has them (typically the first added).
 */
@Composable
private fun WordGroupRow(
    group: WordGroup,
    accent: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    onSpeak: () -> Unit,
    onRemove: () -> Unit,
) {
    val details = group.details
    val hasDetails = details != null && !details.isEmpty
    var sourceTextOverflows by remember(group.anyId, group.sourceWord) { mutableStateOf(false) }
    var translationTextOverflows by remember(group.anyId, group.translations) { mutableStateOf(false) }
    val canExpand = hasDetails || sourceTextOverflows || translationTextOverflows
    var expanded by remember(group.anyId) { mutableStateOf(false) }
    val expandInteractionSource = remember(group.anyId) { MutableInteractionSource() }

    SwipeRevealDeleteContainer(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        deleteButtonWidth = 88.dp,
        onDeleteClick = onRemove,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = PrototypeColor.White,
            shadowElevation = 2.dp,
            border = if (highlighted) BorderStroke(1.dp, PrototypeColor.Yellow) else null,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clickable(
                            enabled = canExpand,
                            interactionSource = expandInteractionSource,
                            indication = null,
                        ) { expanded = !expanded }
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Text(
                                text = group.sourceWord,
                                modifier = Modifier.weight(1f, fill = false),
                                color = PrototypeColor.Ink,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.18).sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { result ->
                                    if (!expanded) sourceTextOverflows = result.hasVisualOverflow
                                },
                            )
                            val ipa = group.ipa
                            if (!ipa.isNullOrBlank()) {
                                Text(
                                    text = ipa,
                                    color = PrototypeColor.Muted2,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        // Translations comma-joined on one line — keeps the row
                        // compact when there are several. Mobile users will rarely
                        // have more than ~5 translations per word; if they do, the
                        // line truncates with ellipsis and the full list shows in
                        // the expanded details block.
                        Text(
                            text = group.translations.joinToString(", "),
                            modifier = Modifier.padding(top = 3.dp),
                            color = PrototypeColor.Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!expanded) translationTextOverflows = result.hasVisualOverflow
                            },
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onSpeak),
                        shape = RoundedCornerShape(12.dp),
                        color = PrototypeColor.Tint,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Sound,
                                modifier = Modifier.size(17.dp),
                                color = PrototypeColor.PurpleText,
                                strokeWidth = 1.9f,
                            )
                        }
                    }

                    PrototypeLineIcon(
                        icon = PrototypeIcon.ChevronDown,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (expanded && canExpand) 180f else 0f },
                        color = if (canExpand) accent else PrototypeColor.Muted3,
                        strokeWidth = 2f,
                    )
                }

                if (expanded && details != null) {
                    WordDetailsBlock(
                        details = details,
                        accent = accent,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    )
                }
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
    onSpeak: () -> Unit,
    onRemove: () -> Unit = {},
) {
    val hasDetails = word.details != null && !word.details.isEmpty
    var sourceTextOverflows by remember(word.id, word.source) { mutableStateOf(false) }
    var translationTextOverflows by remember(word.id, word.translation) { mutableStateOf(false) }
    val canExpand = hasDetails || sourceTextOverflows || translationTextOverflows
    var expanded by remember(word.id) { mutableStateOf(false) }
    val expandInteractionSource = remember(word.id) { MutableInteractionSource() }

    SwipeRevealDeleteContainer(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        deleteButtonWidth = 88.dp,
        onDeleteClick = onRemove,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = PrototypeColor.White,
            shadowElevation = 2.dp,
            border = if (highlighted) BorderStroke(1.dp, PrototypeColor.Yellow) else null,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clickable(
                            enabled = canExpand,
                            interactionSource = expandInteractionSource,
                            indication = null,
                        ) { expanded = !expanded }
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Text(
                                text = word.source,
                                modifier = Modifier.weight(1f, fill = false),
                                color = PrototypeColor.Ink,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.18).sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { result ->
                                    if (!expanded) sourceTextOverflows = result.hasVisualOverflow
                                },
                            )
                            if (!word.ipa.isNullOrBlank()) {
                                Text(
                                    text = word.ipa,
                                    color = PrototypeColor.Muted2,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            text = word.translation,
                            modifier = Modifier.padding(top = 3.dp),
                            color = PrototypeColor.Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result ->
                                if (!expanded) translationTextOverflows = result.hasVisualOverflow
                            },
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onSpeak),
                        shape = RoundedCornerShape(12.dp),
                        color = PrototypeColor.Tint,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Sound,
                                modifier = Modifier.size(17.dp),
                                color = PrototypeColor.PurpleText,
                                strokeWidth = 1.9f,
                            )
                        }
                    }

                    PrototypeLineIcon(
                        icon = PrototypeIcon.ChevronDown,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = if (expanded && canExpand) 180f else 0f },
                        color = if (canExpand) accent else PrototypeColor.Muted3,
                        strokeWidth = 2f,
                    )
                }

                if (expanded && word.details != null) {
                    WordDetailsBlock(
                        details = word.details,
                        accent = accent,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun KnowledgeBackgroundFill(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val clampedPercent = percent.coerceIn(0, 100)
    if (clampedPercent == 0) return

    Box(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(clampedPercent / 100f)
            .background(color),
    )
}

@Composable
internal fun WordDetailsBlock(
    details: com.vocabee.android.feature.vocabulary.domain.model.WordDetails,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.verticalGradient(
                    listOf(PrototypeColor.ContextCardTop, PrototypeColor.ContextCardBottom),
                ),
            )
            .border(BorderStroke(1.dp, PrototypeColor.ContextCardBorder), RoundedCornerShape(13.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (details.senses.isNotEmpty()) {
            details.senses.take(3).forEachIndexed { index, sense ->
                WordSenseBlock(index = index, sense = sense, accent = accent)
            }
        }
        if (details.synonyms.isNotEmpty()) {
            WordChipsRow(label = "Синоніми", values = details.synonyms.take(12), accent = accent)
        }
        if (details.antonyms.isNotEmpty()) {
            WordChipsRow(label = "Антоніми", values = details.antonyms.take(12), accent = PrototypeColor.OrangeText)
        }
        if (details.forms.isNotEmpty()) {
            WordChipsRow(
                label = "Форми",
                values = details.forms.map { it.text }.distinct().take(10),
                accent = PrototypeColor.Muted,
            )
        }
    }
}

@Composable
private fun WordSenseBlock(
    index: Int,
    sense: com.vocabee.android.feature.vocabulary.domain.model.WordSense,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
            ) {
                Text(
                    text = (index + 1).toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = accent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                )
            }
            if (!sense.partOfSpeech.isNullOrBlank()) {
                Text(
                    text = sense.partOfSpeech,
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = sense.definition,
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        sense.examples.take(2).forEach { example ->
            Text(
                text = "“$example”",
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun WordChipsRow(label: String, values: List<String>, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = PrototypeColor.Muted2,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            values.forEach { value ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.10f),
                ) {
                    Text(
                        text = value,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

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
            text = "Введи або продиктуй слово в полі нижче, а варіанти перекладу підкаже AI.",
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
    backdropColor: Color,
    onTabClick: (AppTab) -> Unit,
) {
    val barShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backdropColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(barShape)
                .background(PrototypeColor.White)
                .border(BorderStroke(1.dp, PrototypeColor.Line), barShape)
                .navigationBarsPadding()
                .height(72.dp),
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
}

@Composable
private fun BottomTabButton(
    tab: AppTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val labelColor = if (selected) PrototypeColor.Purple else PrototypeColor.Muted2
    val iconColor = if (selected) Color.White else PrototypeColor.Muted2
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(86.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (selected) PrototypeColor.Purple else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            PrototypeLineIcon(
                icon = tab.prototypeIcon,
                modifier = Modifier.size(if (selected) 25.dp else 24.dp),
                color = iconColor,
                strokeWidth = if (selected) 2.35f else 2f,
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = bottomBarLabel(tab),
            color = labelColor,
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
        AppTab.Practice -> PrototypeIcon.Dumbbell
        AppTab.Settings -> PrototypeIcon.User
    }

/* ============================================================
 * Practice screen
 * ============================================================ */

internal const val KnowledgeStepPercent = 20

@Composable
private fun PracticeScreen(
    topics: List<DictionaryTopic>,
    onSessionActiveChanged: (Boolean) -> Unit,
    onBottomPanelVisibilityChanged: (Boolean) -> Unit,
    onSpeakWord: (word: String, languageTag: String) -> Unit,
    onAnswerWord: (topicId: String, wordId: String, deltaPercent: Int) -> Unit,
    onRoundCompleted: () -> Unit,
) {
    val trainableTopics = topics.filter { topic -> topic.words.isNotEmpty() }
    val trainableTopicIds = trainableTopics.map { topic -> topic.id }
    val trainableTopicIdentity = trainableTopicIds.toSet()
    var selectedTopicIds by remember(trainableTopicIdentity) { mutableStateOf(emptySet<String>()) }
    var practiceStarted by remember(trainableTopicIdentity) { mutableStateOf(false) }
    DisposableEffect(practiceStarted) {
        onSessionActiveChanged(practiceStarted)
        onDispose { onSessionActiveChanged(false) }
    }
    val hasSetupFooter = !practiceStarted && trainableTopics.isNotEmpty()
    DisposableEffect(hasSetupFooter) {
        onBottomPanelVisibilityChanged(hasSetupFooter)
        onDispose { onBottomPanelVisibilityChanged(false) }
    }

    if (!practiceStarted) {
        PracticeSetupScreen(
            topics = trainableTopics,
            selectedTopicIds = selectedTopicIds,
            onToggleTopic = { topicId ->
                selectedTopicIds = if (topicId in selectedTopicIds) {
                    selectedTopicIds - topicId
                } else {
                    selectedTopicIds + topicId
                }
            },
            onSelectAllToggle = {
                selectedTopicIds = if (selectedTopicIds.size == trainableTopicIds.size) {
                    emptySet()
                } else {
                    trainableTopicIds.toSet()
                }
            },
            onStart = {
                if (selectedTopicIds.isNotEmpty()) {
                    practiceStarted = true
                }
            },
        )
        return
    }

    val selectedTopics = trainableTopics.filter { topic -> topic.id in selectedTopicIds }
    val availableCards = selectedTopics.flatMap { topic ->
        topic.words.map { word ->
            val key = "${topic.id}:${word.id}"
            key to PracticeDeckCard(
                word = word,
                topicId = topic.id,
                topicTitle = topic.title,
                sourceLanguageTag = topic.sourceLanguage.speechTag,
                accent = prototypeTopicTheme(topic.coverIndex).color,
            )
        }
    }
    val availableCardKeys = availableCards.map { it.first }
    val availableCardIdentity = availableCardKeys.toSet()
    var shuffleSeed by remember(availableCardIdentity) { mutableIntStateOf(Random.nextInt()) }
    val deckKeys = remember(availableCardIdentity, shuffleSeed) {
        buildPracticeDeckKeys(
            candidates = availableCards.map { (key, card) -> key to card.word.knowledgePercent },
            seed = shuffleSeed,
        )
    }
    val cardsByKey = availableCards.toMap()
    val deck = deckKeys.mapNotNull { key -> cardsByKey[key] }
    var index by remember(deckKeys) { mutableIntStateOf(0) }
    var flipped by remember(deckKeys) { mutableStateOf(false) }
    var correctAnswers by remember(deckKeys) { mutableIntStateOf(0) }
    var waitingForNextAfterMiss by remember(deckKeys) { mutableStateOf(false) }
    var done by remember(deckKeys) { mutableStateOf(false) }
    var interruptionSheetVisible by remember(deckKeys) { mutableStateOf(false) }
    var cardFlipEnabled by remember(deckKeys) { mutableStateOf(false) }
    LaunchedEffect(deckKeys) {
        cardFlipEnabled = false
        delay(300)
        cardFlipEnabled = true
    }

    fun moveNext() {
        waitingForNextAfterMiss = false
        if (index + 1 >= deck.size) {
            done = true
            onRoundCompleted()
        } else {
            flipped = false
            index += 1
        }
    }

    fun answerKnown() {
        val card = deck.getOrNull(index) ?: return
        onAnswerWord(
            card.topicId,
            card.word.id,
            KnowledgeStepPercent,
        )
        correctAnswers += 1
        moveNext()
    }

    fun answerUnknown() {
        if (waitingForNextAfterMiss) return
        val card = deck.getOrNull(index) ?: return
        onAnswerWord(
            card.topicId,
            card.word.id,
            -KnowledgeStepPercent,
        )
        flipped = true
        waitingForNextAfterMiss = true
    }

    fun requestExit() {
        if (done) {
            practiceStarted = false
        } else {
            interruptionSheetVisible = true
        }
    }

    BackHandler {
        requestExit()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PrototypeColor.Background).statusBarsPadding(),
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Тренування",
                    modifier = Modifier.weight(1f),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrototypeColor.Ink,
                    letterSpacing = (-0.6).sp,
                )
                Surface(
                    onClick = ::requestExit,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = PrototypeColor.NeutralSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Close,
                            modifier = Modifier.size(23.dp),
                            color = PrototypeColor.Muted2,
                            strokeWidth = 2.2f,
                        )
                    }
                }
            }
            if (deck.isNotEmpty() && !done) {
                Text(
                    text = "${index + 1} / ${deck.size} · правильно $correctAnswers",
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
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
                correctAnswers = correctAnswers,
                total = deck.size,
                onRestart = {
                    shuffleSeed = Random.nextInt()
                    index = 0; flipped = false; correctAnswers = 0; waitingForNextAfterMiss = false; done = false
                },
                onChooseTopics = {
                    practiceStarted = false
                },
                modifier = Modifier.weight(1f),
            )
            else -> {
                val card = deck[index]
                PracticeFlipCard(
                    card = card,
                    flipped = flipped,
                    flipEnabled = cardFlipEnabled,
                    onFlip = { flipped = !flipped },
                    onSpeak = {
                        onSpeakWord(card.word.source, card.sourceLanguageTag)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (waitingForNextAfterMiss) {
                        PracticeAnswerButton(
                            text = "Далі",
                            icon = PrototypeIcon.ChevronRight,
                            color = PrototypeColor.PurpleText,
                            background = PrototypeColor.Tint,
                            iconAfterText = true,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = ::moveNext,
                        )
                    } else {
                        PracticeAnswerButton(
                            text = "Не знаю",
                            icon = PrototypeIcon.Close,
                            color = PrototypeColor.OrangeText,
                            background = PrototypeColor.NotePeach,
                            modifier = Modifier.weight(1f),
                            onClick = ::answerUnknown,
                        )
                        PracticeAnswerButton(
                            text = "Знаю",
                            icon = PrototypeIcon.Check,
                            color = PrototypeColor.GreenText,
                            background = PrototypeColor.NoteGreen,
                            modifier = Modifier.weight(1f),
                            onClick = ::answerKnown,
                        )
                    }
                }
            }
        }
    }

    if (interruptionSheetVisible) {
        TrainingInterruptionSheet(
            onContinue = { interruptionSheetVisible = false },
            onInterrupt = {
                interruptionSheetVisible = false
                practiceStarted = false
            },
        )
    }
}

@Composable
private fun TrainingInterruptionSheet(
    onContinue: () -> Unit,
    onInterrupt: () -> Unit,
) {
    PrototypeBottomSheet(
        title = "Перервати тренування?",
        onDismiss = onContinue,
    ) {
        Text(
            text = "Раунд ще не завершено. Уже збережені відповіді залишаться в прогресі, але поточну сесію буде завершено.",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 22.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = onInterrupt,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrototypeColor.NeutralSurface,
                border = BorderStroke(1.4.dp, PrototypeColor.Line),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Перервати",
                        color = PrototypeColor.RedText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                }
            }
            Surface(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                color = PrototypeColor.Purple,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Продовжити",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeSetupScreen(
    topics: List<DictionaryTopic>,
    selectedTopicIds: Set<String>,
    onToggleTopic: (String) -> Unit,
    onSelectAllToggle: () -> Unit,
    onStart: () -> Unit,
) {
    val selectedTopics = topics.filter { topic -> topic.id in selectedTopicIds }
    val selectedWords = selectedTopics.sumOf { topic -> topic.words.size }
    val allSelected = topics.isNotEmpty() && selectedTopicIds.size == topics.size

    if (topics.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PrototypeColor.Background)
                .statusBarsPadding(),
        ) {
            PracticeSetupHeader(
                title = "Почати практику",
                subtitle = "Обери словники, які хочеш повторити сьогодні.",
                modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp),
            )
            PracticeEmptyState(modifier = Modifier.weight(1f))
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, top = 8.dp, end = 22.dp, bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PracticeSetupHeader(
                    title = "Почати практику",
                    subtitle = "Вибери словники для короткого раунду повторення.",
                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel(
                        text = "Словники",
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (allSelected) "Очистити" else "Вибрати всі",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onSelectAllToggle)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        color = PrototypeColor.PurpleText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                    )
                }
            }
            items(topics, key = { topic -> topic.id }) { topic ->
                PracticeTopicPickerRow(
                    topic = topic,
                    selected = topic.id in selectedTopicIds,
                    subtitle = null,
                    onClick = { onToggleTopic(topic.id) },
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = PrototypeColor.White,
            shadowElevation = 14.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Вибрано: ${selectedTopics.size} ${ukrainianPlural(selectedTopics.size, "тема", "теми", "тем")}",
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                    )
                    Box(modifier = Modifier.padding(horizontal = 8.dp).size(4.dp).clip(CircleShape).background(PrototypeColor.Muted3))
                    Text(
                        text = "$selectedWords ${ukrainianPlural(selectedWords, "слово", "слова", "слів")}",
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                PrimaryPillButton(
                    label = "Почати тренування",
                    enabled = selectedWords > 0,
                    onClick = onStart,
                )
            }
        }
    }
}

@Composable
private fun PracticeSetupHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            letterSpacing = (-0.6).sp,
        )
        Text(
            text = subtitle,
            modifier = Modifier.padding(top = 8.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun PracticeTopicPickerRow(
    topic: DictionaryTopic,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    val theme = prototypeTopicTheme(topic.coverIndex)
    val wordsCount = topic.words.size
    val knowledgePercent = topic.words.averageKnowledgePercent()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = PrototypeColor.White,
        border = BorderStroke(
            width = if (selected) 1.6.dp else 1.dp,
            color = if (selected) theme.color.copy(alpha = 0.72f) else PrototypeColor.Line,
        ),
        shadowElevation = if (selected) 8.dp else 4.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(16.dp),
                color = theme.color.copy(alpha = if (selected) 0.20f else 0.13f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Book,
                        modifier = Modifier.size(25.dp),
                        color = theme.color,
                        strokeWidth = 2.2f,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle
                        ?: "$wordsCount ${ukrainianPlural(wordsCount, "слово", "слова", "слів")} · $knowledgePercent% знаю",
                    modifier = Modifier.padding(top = 3.dp),
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) PrototypeColor.Purple else Color.Transparent)
                    .border(
                        BorderStroke(2.dp, if (selected) PrototypeColor.Purple else PrototypeColor.Muted3),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Check,
                        modifier = Modifier.size(17.dp),
                        color = Color.White,
                        strokeWidth = 2.5f,
                    )
                }
            }
        }
    }
}

private data class PracticeDeckCard(
    val word: WordEntry,
    val topicId: String,
    val topicTitle: String,
    val sourceLanguageTag: String,
    val accent: Color,
)

internal fun buildPracticeDeckKeys(
    candidates: List<Pair<String, Int>>,
    seed: Int,
): List<String> {
    return candidates
        .sortedBy { (key, _) -> key }
        .shuffled(Random(seed))
        .sortedBy { (_, knowledgePercent) -> knowledgePercent.coerceIn(0, 100) }
        .take(10)
        .map { (key, _) -> key }
}

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
    flipEnabled: Boolean,
    onFlip: () -> Unit,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 460),
        label = "practice-card-flip",
    )
    val showBack = flipped && rotation > 90f

    Surface(
        onClick = onFlip,
        enabled = flipEnabled,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density.density
            },
        shape = RoundedCornerShape(28.dp),
        color = if (showBack) card.accent else PrototypeColor.White,
        border = if (showBack) null else BorderStroke(2.dp, card.accent),
        shadowElevation = 14.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            KnowledgeBackgroundFill(
                percent = card.word.knowledgePercent,
                color = if (showBack) {
                    PrototypeColor.CardKnowledgeFill
                } else {
                    card.accent.copy(alpha = 0.08f)
                },
            )
            Box(
                modifier = Modifier.fillMaxSize().padding(30.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (showBack) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PracticeCardMainText(
                            text = card.word.translation,
                            color = Color.White,
                            baseFontSize = 32,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                        PracticeCardMainText(
                            text = card.word.source,
                            modifier = Modifier.padding(top = 34.dp),
                            color = PrototypeColor.Ink,
                            baseFontSize = 40,
                        )
                        val ipa = card.word.ipa?.takeIf { it.isNotBlank() }
                        if (ipa != null) {
                            Text(
                                text = ipa,
                                modifier = Modifier.padding(top = 8.dp),
                                color = PrototypeColor.Muted2,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                            )
                        }
                        PracticeKnowledgeLevel(
                            percent = card.word.knowledgePercent,
                            accent = card.accent,
                            modifier = Modifier.padding(top = 18.dp),
                        )
                        Surface(
                            onClick = onSpeak,
                            modifier = Modifier.padding(top = 18.dp).size(48.dp),
                            shape = RoundedCornerShape(15.dp),
                            color = PrototypeColor.NeutralSurface,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PrototypeLineIcon(
                                    icon = PrototypeIcon.Sound,
                                    modifier = Modifier.size(21.dp),
                                    color = PrototypeColor.PurpleText,
                                )
                            }
                        }
                    }
                    card.word.contextSentence()?.let { sentence ->
                        Text(
                            text = sentence,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                            color = PrototypeColor.Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeCardMainText(
    text: String,
    color: Color,
    baseFontSize: Int,
    modifier: Modifier = Modifier,
) {
    val wordCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val fontSize = when {
        text.length > 28 -> (baseFontSize - 12).sp
        wordCount >= 3 -> (baseFontSize - 8).sp
        wordCount == 2 -> (baseFontSize - 4).sp
        else -> baseFontSize.sp
    }
    val lineHeight = when {
        text.length > 28 -> (baseFontSize - 6).sp
        wordCount >= 2 -> (baseFontSize + 2).sp
        else -> (baseFontSize + 6).sp
    }

    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = color,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PracticeKnowledgeLevel(
    percent: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val level = (percent.coerceIn(0, 100) / KnowledgeStepPercent).coerceIn(0, 5)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(if (index < level) accent else PrototypeColor.ProgressTrack),
                )
            }
        }
        Text(
            text = "Рівень засвоєння: $level/5",
            modifier = Modifier.padding(top = 8.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun PracticeAnswerButton(
    text: String,
    icon: PrototypeIcon,
    color: Color,
    background: Color,
    iconAfterText: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(19.dp),
        color = PrototypeColor.White,
        border = BorderStroke(2.dp, PrototypeColor.Line),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!iconAfterText) {
                PracticeAnswerButtonIcon(icon = icon, color = color, background = background)
                Spacer(modifier = Modifier.width(9.dp))
            }
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.5.sp,
            )
            if (iconAfterText) {
                Spacer(modifier = Modifier.width(9.dp))
                PracticeAnswerButtonIcon(icon = icon, color = color, background = background)
            }
        }
    }
}

@Composable
private fun PracticeAnswerButtonIcon(
    icon: PrototypeIcon,
    color: Color,
    background: Color,
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
}

@Composable
private fun PracticeDoneState(
    correctAnswers: Int,
    total: Int,
    onRestart: () -> Unit,
    onChooseTopics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val percent = if (total == 0) 0 else (correctAnswers * 100 / total)
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
                color = PrototypeColor.PurpleText,
                letterSpacing = (-0.68).sp,
            )
        }
        Text(
            text = "Раунд завершено",
            modifier = Modifier.padding(top = 22.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrototypeColor.Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Правильних відповідей: $correctAnswers із $total.",
            modifier = Modifier.padding(top = 9.dp),
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.Medium,
            fontSize = 15.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(26.dp))
        PrimaryPillButton(label = "Ще раунд", onClick = onRestart)
        Surface(
            modifier = Modifier
                .padding(top = 10.dp)
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

/* ============================================================
 * Profile
 * ============================================================ */

@Composable
private fun ProfileScreen(
    topics: List<DictionaryTopic>,
    account: VocabeeAccountState,
    userLanguage: LanguageOption,
    learningLanguage: LanguageOption,
    notificationsEnabled: Boolean,
    darkThemeEnabled: Boolean,
    authNotice: String?,
    isGoogleAuthLoading: Boolean,
    onGoogleSignInClick: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onSpeakingClick: () -> Unit,
    onLearningClick: () -> Unit,
    onInviteClick: () -> Unit,
    onHelpClick: () -> Unit,
    streakDays: Int,
    practiceRounds: Int,
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
        item {
            when (account) {
                VocabeeAccountState.Anonymous -> ProfileSignInCard(
                    notice = authNotice,
                    isLoading = isGoogleAuthLoading,
                    onGoogleClick = onGoogleSignInClick,
                )
                is VocabeeAccountState.Authenticated -> ProfileIdentityCard(account)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                ProfileStat(
                    icon = PrototypeIcon.Flame,
                    value = streakDays.toString(),
                    label = "${ukPlural(streakDays, "день", "дні", "днів")} поспіль",
                    tint = PrototypeColor.StatFlameBg,
                    color = PrototypeColor.StatFlameText,
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    icon = PrototypeIcon.Bookmark,
                    value = totalWords.toString(),
                    label = "${ukPlural(totalWords, "слово", "слова", "слів")} збережено",
                    tint = PrototypeColor.Tint,
                    color = PrototypeColor.PurpleText,
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    icon = PrototypeIcon.Cards,
                    value = practiceRounds.toString(),
                    label = ukPlural(practiceRounds, "тренування", "тренування", "тренувань"),
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
                    onClick = onInviteClick,
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
                    onClick = onHelpClick,
                )
            }
        }
        if (account is VocabeeAccountState.Authenticated) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .clickable(onClick = onLogoutClick),
                    shape = RoundedCornerShape(15.dp),
                    color = PrototypeColor.White,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Вийти",
                            color = PrototypeColor.RedText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                        )
                    }
                }
            }
        }
        item {
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
private fun ProfileSignInCard(
    notice: String?,
    isLoading: Boolean,
    onGoogleClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = PrototypeColor.White,
        shadowElevation = 7.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = PrototypeColor.Tint,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PrototypeLogo(modifier = Modifier.size(30.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Локальний профіль",
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                    )
                    Text(
                        text = "Увійди, щоб синхронізувати словники.",
                        modifier = Modifier.padding(top = 2.dp),
                        color = PrototypeColor.Muted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable(enabled = !isLoading, onClick = onGoogleClick),
                shape = RoundedCornerShape(15.dp),
                color = PrototypeColor.NeutralSurface,
                border = BorderStroke(1.5.dp, PrototypeColor.Line),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoogleGlyph(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = if (isLoading) "Підключаю Google..." else "Продовжити з Google",
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                    )
                }
            }
            if (notice != null) {
                Text(
                    text = notice,
                    color = PrototypeColor.Muted,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(account: VocabeeAccountState.Authenticated) {
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
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName.ifBlank { "Vocabee user" },
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = account.email,
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
                        color = PrototypeColor.PurpleText,
                        strokeWidth = 1.9f,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - 10f * unit, cy - 10f * unit),
            size = Size(20f * unit, 20f * unit),
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - 10f * unit, cy - 10f * unit),
            size = Size(20f * unit, 20f * unit),
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - 10f * unit, cy - 10f * unit),
            size = Size(20f * unit, 20f * unit),
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(cx - 10f * unit, cy - 10f * unit),
            size = Size(20f * unit, 20f * unit),
        )
        drawCircle(Color.White, radius = 4f * unit, center = Offset(cx, cy))
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - 1.5f * unit),
            size = Size(5f * unit, 3f * unit),
        )
    }
}

/** Українська форма множини: 1 день / 2 дні / 5 днів (з поправкою на 11–14). */
internal fun ukPlural(count: Int, one: String, few: String, many: String): String {
    val n = if (count < 0) -count else count
    val d10 = n % 10
    val d100 = n % 100
    return when {
        d10 == 1 && d100 != 11 -> one
        d10 in 2..4 && d100 !in 12..14 -> few
        else -> many
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
            // Tight vertical rhythm so the two-line label ("слів збережено" on
            // iOS wraps) still fits inside the fixed 118dp card.
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
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
                modifier = Modifier.padding(top = 4.dp),
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
                fontSize = 11.sp,
                lineHeight = 13.sp,
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
