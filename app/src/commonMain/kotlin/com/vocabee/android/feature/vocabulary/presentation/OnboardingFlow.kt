package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguage
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguages
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLogo
import com.vocabee.android.core.presentation.designsystem.prototypeLanguage
import com.vocabee.android.feature.vocabulary.domain.model.LanguageOption
import kotlinx.coroutines.delay

/** Reusable Manrope-leaning typography for the prototype screens. */
private val TitleStyle = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 30.sp,
    letterSpacing = (-0.6).sp,
    color = PrototypeColor.Ink,
    fontFamily = FontFamily.SansSerif,
)

private val SplashGradient = Brush.radialGradient(
    colors = listOf(
        PrototypeColor.PurpleSoft,
        PrototypeColor.Purple,
        PrototypeColor.PurpleDeep,
    ),
    radius = 1100f,
)

private val OnboardingGradient = Brush.radialGradient(
    colors = listOf(
        PrototypeColor.PurpleSoft,
        PrototypeColor.Purple,
        PrototypeColor.PurpleDeep,
    ),
    radius = 1200f,
)

@Composable
internal fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1900)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashGradient)
            .clickable(onClick = onDone),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrototypeLogo(
                modifier = Modifier.size(96.dp),
                color = Color.White,
                accent = PrototypeColor.Yellow,
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("voca") }
                    withStyle(SpanStyle(color = PrototypeColor.Yellow)) { append("bee") }
                },
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.6).sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Збирай слова. Будуй словники.",
                color = Color.White.copy(alpha = 0.78f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = "MADE WITH CARE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 54.dp),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
        )
    }
}

private data class OnboardingSlide(
    val art: OnboardingArt,
    val title: String,
    val subtitle: String,
)

private enum class OnboardingArt { Read, Organize, Practice }

private val onboardingSlides = listOf(
    OnboardingSlide(
        art = OnboardingArt.Read,
        title = "Зберігай слова під час читання",
        subtitle = "Натрапив на незнайоме слово в книзі? Збережи його одним дотиком — переклад підкаже AI.",
    ),
    OnboardingSlide(
        art = OnboardingArt.Organize,
        title = "Створюй тематичні словники",
        subtitle = "Групуй слова за темами: подорожі, робота, улюблена книга. Кожен словник — свій колір.",
    ),
    OnboardingSlide(
        art = OnboardingArt.Practice,
        title = "Повторюй, коли зручно",
        subtitle = "Картки для тренування з прикладами вживання. Кілька хвилин на день — і слова залишаються.",
    ),
)

@Composable
internal fun OnboardingScreen(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val slide = onboardingSlides[index]
    val isLast = index == onboardingSlides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingGradient)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, end = 18.dp, start = 18.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Пропустити",
                modifier = Modifier
                    .clickable(onClick = onDone)
                    .padding(8.dp),
                color = PrototypeColor.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            OnboardingArtwork(art = slide.art)
        }

        Column(
            modifier = Modifier.padding(horizontal = 34.dp),
        ) {
            Text(
                text = slide.title,
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.White,
                lineHeight = 34.sp,
                letterSpacing = (-0.58).sp,
            )
            Text(
                text = slide.subtitle,
                modifier = Modifier.padding(top = 14.dp),
                color = PrototypeColor.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 25.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 30.dp, end = 30.dp, top = 34.dp, bottom = 46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onboardingSlides.forEachIndexed { i, _ ->
                    val active = i == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 26.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) PrototypeColor.Yellow
                                else PrototypeColor.White.copy(alpha = 0.32f),
                            ),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .height(54.dp)
                    .clickable {
                        if (isLast) onDone() else index += 1
                    },
                shape = RoundedCornerShape(16.dp),
                color = PrototypeColor.White,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Text(
                        text = if (isLast) "Почати" else "Далі",
                        color = PrototypeColor.Purple,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                    )
                    PrototypeLineIcon(
                        icon = PrototypeIcon.ArrowRight,
                        modifier = Modifier.size(19.dp),
                        color = PrototypeColor.Purple,
                        strokeWidth = 2.2f,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingArtwork(art: OnboardingArt) {
    Canvas(modifier = Modifier.size(width = 220.dp, height = 200.dp)) {
        val sx = size.width / 220f
        val sy = size.height / 200f
        fun x(value: Float) = value * sx
        fun y(value: Float) = value * sy
        fun rect(left: Float, top: Float, w: Float, h: Float, r: Float, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = Offset(x(left), y(top)),
                size = Size(x(w), y(h)),
                cornerRadius = CornerRadius(x(r), y(r)),
            )
        }

        when (art) {
            OnboardingArt.Read -> {
                rect(34f, 30f, 152f, 140f, 16f, PrototypeColor.White.copy(alpha = 0.10f))
                rect(52f, 52f, 116f, 96f, 12f, PrototypeColor.White)
                rect(66f, 70f, 64f, 8f, 4f, PrototypeColor.EmptyCardHex)
                rect(66f, 86f, 88f, 8f, 4f, PrototypeColor.Tint)
                rect(64f, 100f, 56f, 14f, 5f, PrototypeColor.Yellow)
                rect(66f, 124f, 80f, 8f, 4f, PrototypeColor.Tint)
                rect(134f, 92f, 40f, 50f, 9f, PrototypeColor.PurpleDeep)
                drawPath(
                    path = Path().apply {
                        moveTo(x(146f), y(114f))
                        lineTo(x(150f), y(118f))
                        lineTo(x(158f), y(109f))
                    },
                    color = PrototypeColor.Yellow,
                    style = Stroke(width = x(3.4f)),
                )
                // sparkle
                drawPath(
                    path = Path().apply {
                        moveTo(x(150f), y(40f))
                        lineTo(x(151.6f), y(44.4f))
                        lineTo(x(156f), y(46f))
                        lineTo(x(151.6f), y(47.6f))
                        lineTo(x(150f), y(52f))
                        lineTo(x(148.4f), y(47.6f))
                        lineTo(x(144f), y(46f))
                        lineTo(x(148.4f), y(44.4f))
                        close()
                    },
                    color = PrototypeColor.Yellow,
                )
            }
            OnboardingArt.Organize -> {
                rect(48f, 118f, 124f, 44f, 14f, PrototypeColor.White.copy(alpha = 0.16f))
                rect(40f, 86f, 140f, 44f, 14f, Color(0xFF7C5CF6))
                rect(32f, 52f, 156f, 48f, 15f, PrototypeColor.White)
                val hexPath = Path().apply {
                    val cx = x(54f)
                    val cy = y(76f)
                    val pts = listOf(
                        Offset(13f, 0f), Offset(6.5f, 11f), Offset(-6.5f, 11f),
                        Offset(-13f, 0f), Offset(-6.5f, -11f), Offset(6.5f, -11f),
                    )
                    moveTo(cx + x(pts.first().x), cy + y(pts.first().y))
                    pts.drop(1).forEach { p -> lineTo(cx + x(p.x), cy + y(p.y)) }
                    close()
                }
                drawPath(hexPath, PrototypeColor.Purple)
                rect(78f, 68f, 70f, 8f, 4f, PrototypeColor.EmptyCardHex)
                rect(78f, 82f, 44f, 7f, 3.5f, PrototypeColor.Tint)
                drawCircle(PrototypeColor.Yellow, radius = x(9f), center = Offset(x(160f), y(76f)))
            }
            OnboardingArt.Practice -> {
                rect(56f, 40f, 108f, 132f, 18f, PrototypeColor.White)
                rect(78f, 64f, 64f, 11f, 5.5f, PrototypeColor.Purple)
                rect(84f, 86f, 52f, 7f, 3.5f, PrototypeColor.Tint)
                rect(92f, 100f, 36f, 7f, 3.5f, PrototypeColor.Tint)
                drawCircle(PrototypeColor.Green, radius = x(15f), center = Offset(x(86f), y(138f)))
                drawPath(
                    path = Path().apply {
                        moveTo(x(81f), y(138f))
                        lineTo(x(84.6f), y(141.6f))
                        lineTo(x(93f), y(134f))
                    },
                    color = PrototypeColor.White,
                    style = Stroke(width = x(3f)),
                )
                drawCircle(
                    PrototypeColor.Orange.copy(alpha = 0.16f),
                    radius = x(15f),
                    center = Offset(x(134f), y(138f)),
                )
                drawPath(
                    path = Path().apply {
                        moveTo(x(129.5f), y(133.5f))
                        lineTo(x(138.5f), y(142.5f))
                        moveTo(x(138.5f), y(133.5f))
                        lineTo(x(129.5f), y(142.5f))
                    },
                    color = PrototypeColor.Orange,
                    style = Stroke(width = x(3f)),
                )
            }
        }
    }
}

@Composable
internal fun AuthScreen(onDone: () -> Unit) {
    var isSignup by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.White)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, end = 18.dp, start = 18.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Пропустити",
                modifier = Modifier
                    .clickable(onClick = onDone)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = 6.dp),
        ) {
            PrototypeLogo(modifier = Modifier.size(46.dp))
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = if (isSignup) "Створи акаунт" else "З поверненням",
                style = TitleStyle,
                letterSpacing = (-0.6).sp,
            )
            Text(
                text = if (isSignup) "Кілька секунд — і починаємо збирати слова."
                else "Раді бачити тебе знову.",
                modifier = Modifier.padding(top = 7.dp, bottom = 26.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 15.5.sp,
            )

            VocabeeFieldLabel("Електронна пошта")
            VocabeeInputField(
                value = email,
                onValueChange = { email = it },
                placeholder = "you@email.com",
                keyboardType = KeyboardType.Email,
            )

            Spacer(modifier = Modifier.height(14.dp))
            VocabeeFieldLabel("Пароль")
            VocabeeInputField(
                value = password,
                onValueChange = { password = it },
                placeholder = "••••••••",
                keyboardType = KeyboardType.Password,
                hidden = true,
            )

            Spacer(modifier = Modifier.height(18.dp))
            PrimaryPillButton(
                label = if (isSignup) "Зареєструватися" else "Увійти",
                onClick = onDone,
            )

            DividerWithLabel(label = "або")

            SocialAuthButton(onClick = onDone) {
                GoogleGlyph(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    "Продовжити з Google",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(11.dp))
            SocialAuthButton(onClick = onDone) {
                FacebookGlyph(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    "Продовжити з Facebook",
                    color = PrototypeColor.Ink,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, PrototypeColor.DividerLight), RoundedCornerShape(0.dp))
                .navigationBarsPadding()
                .padding(start = 30.dp, end = 30.dp, top = 18.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isSignup) "Вже маєш акаунт?" else "Ще немає акаунта?",
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
            )
            Text(
                text = if (isSignup) "Увійти" else "Зареєструватися",
                modifier = Modifier
                    .clickable { isSignup = !isSignup }
                    .padding(start = 6.dp),
                color = PrototypeColor.PurpleText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
            )
        }
    }
}

@Composable
private fun VocabeeFieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        color = PrototypeColor.Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
    )
}

@Composable
private fun VocabeeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        placeholder = {
            Text(
                placeholder,
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next,
        ),
        visualTransformation = if (hidden) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        textStyle = TextStyle(
            color = PrototypeColor.Ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        ),
        shape = RoundedCornerShape(15.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = PrototypeColor.Line,
            focusedBorderColor = PrototypeColor.Purple,
            unfocusedContainerColor = PrototypeColor.FieldBg,
            focusedContainerColor = PrototypeColor.White,
            focusedTextColor = PrototypeColor.Ink,
            unfocusedTextColor = PrototypeColor.Ink,
            cursorColor = PrototypeColor.Purple,
        ),
    )
}

@Composable
internal fun PrimaryPillButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            // Literal white: on the purple fill the label must stay white in
            // BOTH themes (PrototypeColor.White flips to a dark surface tone
            // in dark mode and made the label unreadable).
            containerColor = PrototypeColor.Purple,
            contentColor = Color.White,
            disabledContainerColor = PrototypeColor.Purple.copy(alpha = 0.45f),
            disabledContentColor = Color.White.copy(alpha = 0.85f),
        ),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

@Composable
private fun DividerWithLabel(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(PrototypeColor.Line))
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp),
            color = PrototypeColor.Muted2,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
        Box(modifier = Modifier
            .weight(1f)
            .height(1.dp)
            .background(PrototypeColor.Line))
    }
}

@Composable
private fun SocialAuthButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PrototypeColor.White,
        border = BorderStroke(1.5.dp, PrototypeColor.Line),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable
private fun GoogleGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Simple 4-quadrant google G
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
        drawCircle(PrototypeColor.White, radius = 4f * unit, center = Offset(cx, cy))
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(cx, cy - 1.5f * unit),
            size = Size(5f * unit, 3f * unit),
        )
    }
}

@Composable
private fun FacebookGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(PrototypeColor.FacebookBlue, radius = 10f * unit, center = Offset(cx, cy))
        val path = Path().apply {
            moveTo(cx + 1.5f * unit, cy - 5f * unit)
            lineTo(cx + 1.5f * unit, cy + 5f * unit)
            lineTo(cx - 1f * unit, cy + 5f * unit)
            lineTo(cx - 1f * unit, cy + 1f * unit)
            lineTo(cx - 3f * unit, cy + 1f * unit)
            lineTo(cx - 3f * unit, cy - 1f * unit)
            lineTo(cx - 1f * unit, cy - 1f * unit)
            lineTo(cx - 1f * unit, cy - 2.5f * unit)
            close()
        }
        drawPath(path = path, color = PrototypeColor.White)
    }
}

@Composable
internal fun LanguageSelectScreen(
    supportedLanguages: List<LanguageOption>,
    initialSpeak: String = "uk",
    initialLearn: String = "en",
    onDone: (speakCode: String, learnCode: String) -> Unit,
) {
    var speak by remember { mutableStateOf(initialSpeak) }
    var learn by remember { mutableStateOf(initialLearn) }

    val availableCodes = remember(supportedLanguages) {
        supportedLanguages.map { it.code }.toSet()
    }
    val pickerLanguages = remember(supportedLanguages) {
        PrototypeLanguages.filter { it.code in availableCodes }
            .ifEmpty { PrototypeLanguages }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrototypeColor.White)
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            PrototypeLogo(modifier = Modifier.size(38.dp))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Налаштуймо мови",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrototypeColor.Ink,
                letterSpacing = (-0.56).sp,
            )
            Text(
                text = "Це встановиться за замовчуванням для всіх нових словників. Змінити можна будь-коли в профілі.",
                modifier = Modifier.padding(top = 8.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguagePill(code = speak, active = false)
                Spacer(modifier = Modifier.width(12.dp))
                PrototypeLineIcon(
                    icon = PrototypeIcon.ArrowRight,
                    modifier = Modifier.size(20.dp),
                    color = PrototypeColor.Muted2,
                )
                Spacer(modifier = Modifier.width(12.dp))
                LanguagePill(code = learn, active = true)
            }

            SectionEyebrow(text = "Я РОЗМОВЛЯЮ")
            LanguageGrid(
                languages = pickerLanguages,
                exclude = learn,
                selected = speak,
                onSelect = { speak = it },
            )

            SectionEyebrow(text = "Я ВИВЧАЮ")
            LanguageGrid(
                languages = pickerLanguages,
                exclude = speak,
                selected = learn,
                onSelect = { learn = it },
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PrototypeColor.White,
            shadowElevation = 6.dp,
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 26.dp, vertical = 16.dp),
            ) {
                PrimaryPillButton(label = "Готово", onClick = { onDone(speak, learn) })
            }
        }
    }
}

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 2.dp),
        color = PrototypeColor.Muted2,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        letterSpacing = 0.65.sp,
    )
}

@Composable
private fun LanguagePill(code: String, active: Boolean) {
    val lang = prototypeLanguage(code)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (active) PrototypeColor.Tint else PrototypeColor.NeutralSurface,
        border = BorderStroke(
            1.5.dp,
            if (active) Color(0xFFC7D2FE) else PrototypeColor.Line,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(lang.flag, fontSize = 20.sp)
            Text(
                lang.name,
                color = if (active) PrototypeColor.Purple else PrototypeColor.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun LanguageGrid(
    languages: List<PrototypeLanguage>,
    exclude: String,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val filtered = languages.filter { it.code != exclude }
    val rows = filtered.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { lang ->
                    LanguageCard(
                        lang = lang,
                        selected = selected == lang.code,
                        onClick = { onSelect(lang.code) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    lang: PrototypeLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PrototypeColor.Tint else PrototypeColor.White)
            .border(
                BorderStroke(
                    1.5.dp,
                    if (selected) PrototypeColor.Purple else PrototypeColor.Line,
                ),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            // Reserve the top-right corner for the check badge so it never
            // sits on top of the label.
            modifier = Modifier.padding(end = if (selected) 14.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Text(lang.flag, fontSize = 22.sp)
            // Long names ("Українська") shrink instead of running under the badge.
            var labelScale by remember(lang.name) { mutableStateOf(1f) }
            Text(
                lang.name,
                color = if (selected) PrototypeColor.Purple else PrototypeColor.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp * labelScale,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (result.hasVisualOverflow && labelScale > 0.72f) {
                        labelScale -= 0.04f
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.Purple),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Check,
                    modifier = Modifier.size(14.dp),
                    color = PrototypeColor.White,
                    strokeWidth = 2.6f,
                )
            }
        }
    }
}
