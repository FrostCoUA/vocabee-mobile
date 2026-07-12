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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vocabee.android.core.presentation.designsystem.PrototypeColor
import com.vocabee.android.core.presentation.designsystem.manropeFamily
import com.vocabee.android.core.presentation.designsystem.PrototypeIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguage
import com.vocabee.android.core.presentation.designsystem.PrototypeLanguages
import com.vocabee.android.core.presentation.designsystem.PrototypeLineIcon
import com.vocabee.android.core.presentation.designsystem.PrototypeTopicIcons
import com.vocabee.android.core.presentation.designsystem.PrototypeTopicThemes
import com.vocabee.android.core.presentation.designsystem.languageFlag
import com.vocabee.android.core.presentation.designsystem.languageName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrototypeBottomSheet(
    title: String?,
    onDismiss: () -> Unit,
    foot: @Composable (ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PrototypeColor.SheetSurface,
        contentColor = PrototypeColor.Ink,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        // Status-bar inset kicks in only when the sheet stretches to full
        // height (keyboard open) — keeps the title clear of the notch/island.
        contentWindowInsets = { WindowInsets.statusBars },
        scrimColor = Color(0x80111827),
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
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
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

            body()

            if (foot != null) {
                Spacer(modifier = Modifier.height(14.dp))
                foot()
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
internal fun CreateDictionarySheet(
    sourceLanguageCode: String,
    targetLanguageCode: String,
    existingDictionariesCount: Int,
    onDismiss: () -> Unit,
    onCreate: (title: String, coverIndex: Int, iconIndex: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedIcon by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val cleanedName = name.trim()
    val isValid = cleanedName.isNotEmpty()
    val atLimit = existingDictionariesCount >= 5

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    PrototypeBottomSheet(
        title = "Новий словник",
        onDismiss = onDismiss,
        foot = {
            PrimaryPillButton(
                label = "Створити",
                onClick = { if (isValid) onCreate(cleanedName, selectedIndex, selectedIcon) },
                enabled = isValid,
            )
        },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SheetLabel(text = "Назва теми")
            OutlinedTextField(
                value = name,
                onValueChange = { input ->
                    if (input.length <= 28) name = input
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                placeholder = {
                    Text(
                        "напр. Подорожі, Робота, Книга…",
                        color = PrototypeColor.Muted2,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    )
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
                ),
            )

            Spacer(modifier = Modifier.height(18.dp))
            SheetLabel(text = "Іконка теми")
            IconPicker(
                selectedIcon = selectedIcon,
                accent = PrototypeTopicThemes[selectedIndex].color,
                onSelect = { selectedIcon = it },
            )

            Spacer(modifier = Modifier.height(18.dp))
            SheetLabel(text = "Колір теми")
            SwatchPalette(
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
            )

            Spacer(modifier = Modifier.height(20.dp))
            LanguageInfoStrip(
                sourceCode = sourceLanguageCode,
                targetCode = targetLanguageCode,
            )

            if (atLimit) {
                Spacer(modifier = Modifier.height(14.dp))
                LimitNote()
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        color = PrototypeColor.Muted,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
    )
}

@Composable
private fun SwatchPalette(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val rows = PrototypeTopicThemes.chunked(4)
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                row.forEachIndexed { columnIndex, theme ->
                    val index = rows.indexOf(row) * 4 + columnIndex
                    SwatchTile(
                        color = theme.color,
                        selected = selectedIndex == index,
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
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .then(
                if (selected) {
                    Modifier
                        .border(BorderStroke(3.dp, PrototypeColor.White), RoundedCornerShape(14.dp))
                        .border(BorderStroke(5.5.dp, color), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            PrototypeLineIcon(
                icon = PrototypeIcon.Check,
                modifier = Modifier.size(18.dp),
                color = PrototypeColor.White,
                strokeWidth = 2.6f,
            )
        }
    }
}

@Composable
private fun IconPicker(
    selectedIcon: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    val rows = PrototypeTopicIcons.chunked(5)
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                row.forEachIndexed { columnIndex, icon ->
                    val index = rowIndex * 5 + columnIndex
                    IconTile(
                        icon = icon,
                        selected = selectedIcon == index,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(index) },
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
            color = if (selected) PrototypeColor.White else PrototypeColor.Muted,
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun LanguageInfoStrip(sourceCode: String, targetCode: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PrototypeColor.Background)
            .padding(horizontal = 14.dp, vertical = 13.dp),
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
            text = "Мова: ${languageFlag(sourceCode)} ${languageName(sourceCode)} → ${languageFlag(targetCode)} ${languageName(targetCode)}",
            color = PrototypeColor.Muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
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
                    color = PrototypeColor.White,
                    strokeWidth = 2.6f,
                )
            }
        } else {
            Spacer(modifier = Modifier.width(22.dp))
        }
    }
}
