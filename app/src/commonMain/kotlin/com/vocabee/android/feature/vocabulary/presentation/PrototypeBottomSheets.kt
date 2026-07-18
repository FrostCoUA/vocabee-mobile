package com.vocabee.android.feature.vocabulary.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.manropeFamily
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguage
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguages
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeTopicIcons
import com.vocabee.android.core.presentation.designsystem.PrototypeTopicIconsPickerOrder
import com.vocabee.android.core.presentation.designsystem.PrototypeTopicThemes
import com.vocabee.android.core.presentation.designsystem.languageFlag
import com.vocabee.android.core.presentation.designsystem.prototypeTopicIconStorageIndex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrototypeBottomSheet(
    title: String?,
    onDismiss: () -> Unit,
    foot: @Composable (ColumnScope.() -> Unit)? = null,
    /** Слот ліворуч від ✕ у хедері шита — наприклад, пара прапорів «🇬🇧 → 🇺🇦». */
    titleAccessory: (@Composable () -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Інсети читаємо ТУТ, у вікні застосунку: всередині ModalBottomSheet (окреме
    // вікно діалога) WindowInsets.navigationBars повертає 0, і кнопка дії високого
    // шита ховається під системною навігацією.
    val navigationBarBottom = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrototypeColor.SheetSurface,
        contentColor = PrototypeColor.Ink,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Status-bar inset kicks in only when the sheet stretches to full
        // height (keyboard open) — keeps the title clear of the notch/island.
        contentWindowInsets = { WindowInsets.statusBars },
        scrimColor = PrototypeColor.Scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 12.dp)
                    .size(width = 42.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.SheetHandle),
            )
        },
    ) {
        // Скрол на всьому вмісті шита: високі шити (створення словника — 20 іконок
        // і 12 кольорів) інакше виштовхують кнопку дії під системну навігацію.
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = PrototypeColor.Ink,
                        letterSpacing = (-0.44).sp,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        titleAccessory?.invoke()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(PrototypeColor.SheetControlSurface)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            PrototypeLineIcon(
                                icon = PrototypeIcon.Close,
                                modifier = Modifier.size(20.dp),
                                color = PrototypeColor.Muted2,
                                strokeWidth = 2.1f,
                            )
                        }
                    }
                }
            }

            body()

            if (foot != null) {
                Spacer(modifier = Modifier.height(14.dp))
                foot()
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(34.dp + navigationBarBottom))
        }
    }
}

/**
 * Шит створення словника; у режимі РЕДАГУВАННЯ ([editMode] = true) поля
 * префілляться наявним словником, заголовок стає «Редагування», а CTA —
 * «Зберегти». Пара мов у обох режимах не редагується (D6).
 */
@Composable
internal fun CreateDictionarySheet(
    sourceLanguageCode: String,
    targetLanguageCode: String,
    existingDictionariesCount: Int,
    onDismiss: () -> Unit,
    onCreate: (title: String, coverIndex: Int, iconIndex: Int) -> Unit,
    editMode: Boolean = false,
    initialTitle: String = "",
    initialCoverIndex: Int = 0,
    initialIconIndex: Int = 0,
) {
    var name by remember { mutableStateOf(initialTitle) }
    var selectedIndex by remember { mutableStateOf(initialCoverIndex.coerceIn(0, PrototypeTopicThemes.lastIndex)) }
    var selectedIcon by remember { mutableStateOf(initialIconIndex.coerceIn(0, PrototypeTopicIcons.lastIndex)) }
    val focusRequester = remember { FocusRequester() }
    val cleanedName = name.trim()
    val isValid = cleanedName.isNotEmpty()
    val atLimit = !editMode && existingDictionariesCount >= 5

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    PrototypeBottomSheet(
        title = if (editMode) "Редагування" else "Новий словник",
        onDismiss = onDismiss,
        // Пара мов живе в ХЕДЕРІ шита (борди A/A′) і не редагується в жодному
        // з режимів — вона фіксується при створенні словника (D6).
        titleAccessory = {
            SheetLanguagePairChip(
                sourceCode = sourceLanguageCode,
                targetCode = targetLanguageCode,
            )
        },
        foot = {
            SheetPrimaryButton(
                label = if (editMode) "Зберегти" else "Створити",
                onClick = { if (isValid) onCreate(cleanedName, selectedIndex, selectedIcon) },
                enabled = isValid,
            )
        },
    ) {
        Column {
            SheetTextField(
                value = name,
                onValueChange = { input ->
                    if (input.length <= 28) name = input
                },
                floatingLabel = "Введіть назву нового словника",
                modifier = Modifier.focusRequester(focusRequester),
            )

            Spacer(modifier = Modifier.height(18.dp))
            SheetLabel(text = "Іконка теми")
            IconPicker(
                selectedIconIndex = selectedIcon,
                accent = PrototypeTopicThemes[selectedIndex].color,
                onSelect = { selectedIcon = it },
            )

            Spacer(modifier = Modifier.height(18.dp))
            SheetLabel(text = "Колір теми")
            SwatchPalette(
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
            )

            if (atLimit) {
                Spacer(modifier = Modifier.height(14.dp))
                LimitNote()
            }
        }
    }
}

@Composable
internal fun SheetLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        color = PrototypeColor.Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
    )
}

/** Поле шита за бордом (P.field): 54dp, r15, field-фон, purple-бордер у фокусі. */
@Composable
internal fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    /**
     * Плаваючий лейбл: підказка стоїть у полі, а при фокусі (або коли вже є
     * текст) з анімацією їде у виріз бордера. Тоді окремий підпис над полем
     * не потрібен. Якщо null — поводиться як звичайне поле з плейсхолдером.
     */
    floatingLabel: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            // З плаваючим лейблом поле мусить бути вищим: інакше піднятий
            // лейбл не влазить у виріз бордера.
            .then(if (floatingLabel == null) Modifier.height(54.dp) else Modifier.heightIn(min = 58.dp)),
        singleLine = true,
        label = floatingLabel?.let { text ->
            {
                Text(
                    text,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        placeholder = placeholder?.let { text ->
            {
                Text(
                    text,
                    color = PrototypeColor.Muted2,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    // Один рядок з обрізанням у кінці: довга підказка на вузькому
                    // екрані інакше переноситься і розпирає поле.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        textStyle = TextStyle(
            fontFamily = manropeFamily(),
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
            cursorColor = PrototypeColor.Purple,
            focusedLabelColor = PrototypeColor.PurpleText,
            unfocusedLabelColor = PrototypeColor.Muted2,
        ),
    )
}

/** Основна кнопка шита: 56dp r16 purple — анатомія з борду секції 7. */
@Composable
internal fun SheetPrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            // Літерал: білий текст на purple-заливці читається в ОБОХ темах.
            .background(if (enabled) PrototypeColor.Purple else PrototypeColor.Purple.copy(alpha = 0.45f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.85f),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
        )
    }
}

/** 12 акцентів у 2 ряди по 6 (борд «Колір теми»). */
@Composable
private fun SwatchPalette(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val rows = PrototypeTopicThemes.chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEachIndexed { columnIndex, theme ->
                    val index = rowIndex * 6 + columnIndex
                    SwatchTile(
                        color = theme.color,
                        selected = selectedIndex == index,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwatchTile(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (selected) {
                    Modifier
                        // Кільце вибору за бордом: проріз кольором ПОВЕРХНІ шита,
                        // назовні — сам акцент (у dark білий проріз світився).
                        .border(BorderStroke(3.dp, PrototypeColor.SheetSurface), RoundedCornerShape(12.dp))
                        .border(BorderStroke(5.5.dp, color), RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Check,
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.6f,
            )
        }
    }
}

/**
 * Сітка 5×4 з 20 іконок тем. Рендер іде в ДИСПЛЕЙНОМУ порядку борду
 * ([PrototypeTopicIconsPickerOrder]), а назовні віддається КАНОНІЧНИЙ індекс
 * зберігання ([prototypeTopicIconStorageIndex]) — інакше зміна порядку показу
 * мовчки перефарбувала б іконки наявних словників.
 */
@Composable
private fun IconPicker(
    selectedIconIndex: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    val rows = PrototypeTopicIconsPickerOrder.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEach { icon ->
                    val storageIndex = prototypeTopicIconStorageIndex(icon)
                    IconTile(
                        icon = icon,
                        selected = selectedIconIndex == storageIndex,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(storageIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IconTile(
    icon: PrototypeIcon,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent else PrototypeColor.NeutralSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PrototypeLineIcon(
            icon = icon,
            modifier = Modifier.size(23.dp),
            color = if (selected) Color.White else PrototypeColor.Muted,
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun LimitNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PrototypeColor.NoteYellowBg)
            .border(BorderStroke(1.dp, PrototypeColor.NoteYellowBorder), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        PrototypeLineIcon(
            icon = PrototypeIcon.Star,
            modifier = Modifier.size(16.dp),
            color = PrototypeColor.StatFlameText,
            strokeWidth = 1.9f,
        )
        Text(
            text = "Ти створив(ла) максимум 5 словників. Переглянь відео, щоб відкрити більше.",
            color = PrototypeColor.NoteYellowText,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

/**
 * Шит «Я вивчаю» — перемикач робочої мовної пари з хедера Головної/Тренування.
 *
 * АКТИВНІ мови — ті, що вже мають словники в парі з рідною (плюс поточна
 * вибрана, навіть якщо словників у неї ще нема): клікабельні, з лічильником
 * «N словники». Решта підтримуваних мов показані приглушено (opacity .42) і
 * не реагують на тап — новий словник іншої мови створюється через FAB/профіль.
 */
@Composable
internal fun LearningLanguageSheet(
    selectedCode: String,
    userLanguageCode: String,
    topicCountsByLanguage: Map<String, Int>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val options = remember(userLanguageCode, selectedCode, topicCountsByLanguage) {
        PrototypeLanguages
            .filter { lang -> lang.code != userLanguageCode }
            .map { lang ->
                val count = topicCountsByLanguage[lang.code] ?: 0
                LearningLanguageChoice(
                    lang = lang,
                    topicCount = count,
                    enabled = count > 0 || lang.code == selectedCode,
                )
            }
            .sortedWith(
                compareByDescending<LearningLanguageChoice> { choice -> choice.enabled }
                    .thenByDescending { choice -> choice.topicCount }
                    .thenBy { choice -> choice.lang.name },
            )
    }

    PrototypeBottomSheet(
        title = "Я вивчаю",
        onDismiss = onDismiss,
        titleAccessory = {
            SheetLanguagePairChip(
                sourceCode = selectedCode,
                targetCode = userLanguageCode,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
        ) {
            items(options, key = { choice -> choice.lang.code }) { choice ->
                LearningLanguageSheetRow(
                    choice = choice,
                    selected = choice.lang.code == selectedCode,
                    onClick = { onPick(choice.lang.code) },
                )
            }
        }
        Text(
            text = "Неактивні — підтримувані мови без словників у парі з ${languageFlag(userLanguageCode)}. " +
                "Новий словник іншої мови створюється через профіль або кнопку «+».",
            modifier = Modifier.padding(top = 12.dp),
            color = PrototypeColor.Muted2,
            fontWeight = FontWeight.Medium,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        )
    }
}

private data class LearningLanguageChoice(
    val lang: PrototypeLanguage,
    val topicCount: Int,
    val enabled: Boolean,
)

@Composable
private fun LearningLanguageSheetRow(
    choice: LearningLanguageChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (choice.enabled) 1f else 0.42f)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) PrototypeColor.Tint else PrototypeColor.White)
            .border(
                BorderStroke(
                    1.5.dp,
                    if (selected) PrototypeColor.Purple else PrototypeColor.Line,
                ),
                RoundedCornerShape(15.dp),
            )
            .clickable(enabled = choice.enabled, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(choice.lang.flag, fontSize = 22.sp)
        Text(
            text = choice.lang.name,
            modifier = Modifier.weight(1f),
            color = if (selected) PrototypeColor.PurpleText else PrototypeColor.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        if (choice.topicCount > 0) {
            Text(
                text = "${choice.topicCount} ${ukPlural(choice.topicCount, "словник", "словники", "словників")}",
                color = PrototypeColor.Muted2,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.Purple),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Check,
                    modifier = Modifier.size(15.dp),
                    color = Color.White,
                    strokeWidth = 2.6f,
                )
            }
        }
    }
}

/** Пара прапорів «мова-джерело → мова-переклад» у хедері шита. */
@Composable
internal fun SheetLanguagePairChip(sourceCode: String, targetCode: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(PrototypeColor.NeutralSurface)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(languageFlag(sourceCode), fontSize = 15.sp)
        PrototypeLineIcon(
            icon = PrototypeIcon.ArrowRight,
            modifier = Modifier.size(12.dp),
            color = PrototypeColor.Muted2,
            strokeWidth = 2.2f,
        )
        Text(languageFlag(targetCode), fontSize = 15.sp)
    }
}

@Composable
internal fun LanguageSheet(
    title: String,
    subtitle: String?,
    selectedCode: String,
    excludeCode: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val options = remember(excludeCode) {
        PrototypeLanguages.filter { it.code != excludeCode }
    }

    PrototypeBottomSheet(
        title = title,
        onDismiss = onDismiss,
    ) {
        if (subtitle != null) {
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = (-6).dp, bottom = 16.dp),
                color = PrototypeColor.Muted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 21.sp,
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp),
        ) {
            items(options, key = { it.code }) { lang ->
                LanguageSheetRow(
                    lang = lang,
                    selected = lang.code == selectedCode,
                    onClick = { onPick(lang.code) },
                )
            }
        }
    }
}

@Composable
private fun LanguageSheetRow(
    lang: PrototypeLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) PrototypeColor.Tint else PrototypeColor.White)
            .border(
                BorderStroke(
                    1.5.dp,
                    if (selected) PrototypeColor.Purple else PrototypeColor.Line,
                ),
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(lang.flag, fontSize = 22.sp)
        Text(
            text = lang.name,
            modifier = Modifier.weight(1f),
            color = if (selected) PrototypeColor.Purple else PrototypeColor.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(PrototypeColor.Purple),
                contentAlignment = Alignment.Center,
            ) {
                PrototypeLineIcon(
                    icon = PrototypeIcon.Check,
                    modifier = Modifier.size(15.dp),
                    color = Color.White,
                    strokeWidth = 2.6f,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(22.dp))
        }
    }
}
