package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.core.presentation.designsystem.HoneycombWatermark
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.manropeFamily
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLogo
import com.vocabee.android.feature.vocabulary.data.api.SupportRequestBody
import com.vocabee.android.feature.vocabulary.data.api.VocabeeApi
import com.vocabee.android.feature.vocabulary.presentation.platform.ShareController
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image

/* ============================================================
 * Design board 5: «Запросити друзів» (QR + share) і «Допомога»
 * (форма на бек). Обидва — пуш-екрани з Профілю.
 * ============================================================ */

private const val FallbackInviteLink = "https://vocabee.app"

@Composable
internal fun PushedScreenTopBar(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 8.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(PrototypeColor.NeutralSurface)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            PrototypeLineIcon(
                icon = PrototypeIcon.ChevronLeft,
                modifier = Modifier.size(20.dp),
                color = PrototypeColor.Ink,
                strokeWidth = 2.2f,
            )
        }
        Text(
            text = title,
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = (-0.4).sp,
        )
    }
}

/* ---------------- Запросити друзів ---------------- */

@Composable
internal fun InviteFriendsScreen(
    api: VocabeeApi?,
    isAuthenticated: Boolean,
    refreshTokenProvider: () -> String?,
    shareController: ShareController,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    var link by remember { mutableStateOf(FallbackInviteLink) }
    LaunchedEffect(isAuthenticated) {
        if (api != null && isAuthenticated) {
            val referral = runCatching { api.fetchReferral() }.getOrElse {
                // The access token lives ~15 min — refresh once and retry.
                runCatching {
                    refreshTokenProvider()?.let { token -> api.refreshSession(token) }
                    api.fetchReferral()
                }.getOrNull()
            }
            referral?.let { link = it.link }
        }
    }
    val clipboard = LocalClipboardManager.current
    val shareMessage =
        "Вчу слова у Vocabee — приєднуйся! Обом впаде по +10 монеток: $link"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background)
            .statusBarsPadding(),
    ) {
        PushedScreenTopBar(title = "Запросити друзів", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, top = 16.dp, end = 22.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // hero + QR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrototypeColor.PurpleSoft,
                                PrototypeColor.Purple,
                                PrototypeColor.PurpleDeep,
                            ),
                            radius = 900f,
                        ),
                    ),
            ) {
                HoneycombWatermark(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(120.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(178.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 14.dp,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = rememberQrCodePainter(link),
                                    contentDescription = "QR-код запрошення",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PrototypeLogo(
                                    modifier = Modifier.size(26.dp),
                                    color = PrototypeColor.Purple,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Скануй — і вчімося разом",
                        modifier = Modifier.padding(top = 14.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    Surface(
                        modifier = Modifier.padding(top = 9.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = "🍯 +10 монеток тобі й другові",
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                            color = PrototypeColor.Yellow,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            // link + copy
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(15.dp),
                color = PrototypeColor.FieldBg,
                border = BorderStroke(1.5.dp, PrototypeColor.Line),
            ) {
                Row(
                    modifier = Modifier.padding(start = 15.dp, end = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PrototypeLineIcon(
                        icon = PrototypeIcon.Globe,
                        modifier = Modifier.size(17.dp),
                        color = PrototypeColor.Muted2,
                        strokeWidth = 1.9f,
                    )
                    Text(
                        text = link.removePrefix("https://"),
                        modifier = Modifier.weight(1f),
                        color = PrototypeColor.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                clipboard.setText(AnnotatedString(link))
                                onShowSnackbar("Лінк скопійовано")
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = PrototypeColor.White,
                        border = BorderStroke(1.dp, PrototypeColor.Line),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Copy,
                                modifier = Modifier.size(18.dp),
                                color = PrototypeColor.PurpleText,
                                strokeWidth = 2f,
                            )
                        }
                    }
                }
            }

            PrimaryPillButton(
                label = "Поділитися",
                onClick = { shareController.shareText(shareMessage) },
            )

            if (!isAuthenticated) {
                Text(
                    text = "Увійди через Google, щоб отримати персональну лінку і бонус +10 монеток за друга.",
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/* ---------------- Допомога та підтримка ---------------- */

private data class SupportTopicOption(val key: String, val label: String)

private val SupportTopics = listOf(
    SupportTopicOption("bug", "Помилка"),
    SupportTopicOption("idea", "Ідея"),
    SupportTopicOption("billing", "Оплата"),
    SupportTopicOption("other", "Інше"),
)

@Composable
internal fun HelpSupportScreen(
    api: VocabeeApi?,
    isAuthenticated: Boolean,
    accountEmail: String?,
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    var topic by remember { mutableStateOf("other") }
    var message by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val emailOk = isAuthenticated || (email.contains("@") && email.contains("."))
    val canSend = !sending && api != null && message.trim().length >= 10 && emailOk

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.Background)
            .statusBarsPadding(),
    ) {
        PushedScreenTopBar(title = "Допомога та підтримка", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Text(
                text = "Опиши проблему чи ідею — відповімо на пошту протягом доби.",
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
            )

            Column {
                EngagementFieldLabel("Тема")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SupportTopics.forEach { option ->
                        val selected = topic == option.key
                        Surface(
                            shape = CircleShape,
                            color = if (selected) PrototypeColor.Purple else PrototypeColor.NeutralSurface,
                            modifier = Modifier.clickable { topic = option.key },
                        ) {
                            Text(
                                text = option.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                color = if (selected) Color.White else PrototypeColor.Muted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                            )
                        }
                    }
                }
            }

            Column {
                EngagementFieldLabel("Повідомлення")
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 126.dp),
                    shape = RoundedCornerShape(15.dp),
                    color = if (message.isNotEmpty()) PrototypeColor.White else PrototypeColor.FieldBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (message.isNotEmpty()) PrototypeColor.Purple else PrototypeColor.Line,
                    ),
                ) {
                    Box(modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
                        if (message.isEmpty()) {
                            Text(
                                text = "Опиши, що сталося або що хочеться покращити…",
                                color = PrototypeColor.Muted2,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                            )
                        }
                        BasicTextField(
                            value = message,
                            onValueChange = { message = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 98.dp),
                            textStyle = TextStyle(
                fontFamily = manropeFamily(),
                                color = PrototypeColor.Ink,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                            ),
                            cursorBrush = SolidColor(PrototypeColor.Purple),
                        )
                    }
                }
            }

            if (isAuthenticated && !accountEmail.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = PrototypeColor.NeutralSurface,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PrototypeLineIcon(
                            icon = PrototypeIcon.Globe,
                            modifier = Modifier.size(16.dp),
                            color = PrototypeColor.Muted2,
                            strokeWidth = 1.8f,
                        )
                        Text(
                            text = "Відповімо на $accountEmail",
                            color = PrototypeColor.Muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        )
                    }
                }
            } else {
                Column {
                    EngagementFieldLabel("Email для відповіді")
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = if (email.isNotEmpty()) PrototypeColor.White else PrototypeColor.FieldBg,
                        border = BorderStroke(
                            1.5.dp,
                            if (email.isNotEmpty()) PrototypeColor.Purple else PrototypeColor.Line,
                        ),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 17.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (email.isEmpty()) {
                                Text(
                                    text = "you@email.com",
                                    color = PrototypeColor.Muted2,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                )
                            }
                            BasicTextField(
                                value = email,
                                onValueChange = { email = it.trim() },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(
                fontFamily = manropeFamily(),
                                    color = PrototypeColor.Ink,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                ),
                                cursorBrush = SolidColor(PrototypeColor.Purple),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            PrimaryPillButton(
                label = if (sending) "Надсилаю…" else "Надіслати",
                enabled = canSend,
                onClick = {
                    val request = SupportRequestBody(
                        topic = topic,
                        message = message.trim(),
                        email = email.takeIf { it.isNotBlank() },
                    )
                    sending = true
                    scope.launch {
                        val result = runCatching { api!!.submitSupport(request) }
                        sending = false
                        result
                            .onSuccess {
                                onShowSnackbar("Надіслано! Відповімо протягом доби")
                                onBack()
                            }
                            .onFailure {
                                onShowSnackbar("Не вдалося надіслати — спробуй ще раз")
                            }
                    }
                },
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun EngagementFieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        color = PrototypeColor.Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
    )
}
