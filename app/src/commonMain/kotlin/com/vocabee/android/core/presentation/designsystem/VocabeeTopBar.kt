package com.vocabee.android.core.presentation.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class VocabeeTopBarConfig constructor(
    val containerMode: VocabeeTopBarDisplayMode = VocabeeTopBarDisplayMode.Visible,
    val navBackMode: VocabeeTopBarDisplayMode = VocabeeTopBarDisplayMode.Visible,
    val type: VocabeeTopBarType = VocabeeTopBarType.CenterSingleRow,
)

internal enum class VocabeeTopBarDisplayMode {
    Visible,
    Space,
    Gone,
}

internal enum class VocabeeTopBarType {
    SingleRow,
    CenterSingleRow,
}

internal enum class VocabeeNavigationIcon {
    Back,
    Close,
}

internal sealed interface VocabeeTopBarAction {
    data class Text constructor(
        val text: String,
        val enabled: Boolean = true,
        val textColor: Color? = null,
        val action: () -> Unit,
    ) : VocabeeTopBarAction

    data class Custom constructor(
        val content: @Composable RowScope.() -> Unit,
    ) : VocabeeTopBarAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VocabeeTopBar(
    config: VocabeeTopBarConfig = VocabeeTopBarConfig(),
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    navigationIcon: VocabeeNavigationIcon = VocabeeNavigationIcon.Back,
    navigationContainerColor: Color = MaterialTheme.colorScheme.surface,
    navigationContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onNavigateBack: () -> Unit = {},
    actions: List<VocabeeTopBarAction> = emptyList(),
) {
    when (config.containerMode) {
        VocabeeTopBarDisplayMode.Gone -> return
        VocabeeTopBarDisplayMode.Space -> {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopBarHeight)
                    .background(vocabeeColor(VocabeeColor.Transparent)),
            )
            return
        }
        VocabeeTopBarDisplayMode.Visible -> Unit
    }

    val transparentColor = vocabeeColor(VocabeeColor.Transparent)
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = transparentColor,
        scrolledContainerColor = transparentColor,
    )

    when (config.type) {
        VocabeeTopBarType.SingleRow -> {
            TopAppBar(
                modifier = modifier,
                title = title,
                navigationIcon = {
                    NavigationIconView(
                        displayMode = config.navBackMode,
                        icon = navigationIcon,
                        containerColor = navigationContainerColor,
                        contentColor = navigationContentColor,
                        onClick = onNavigateBack,
                    )
                },
                actions = { ActionItems(actions) },
                colors = colors,
                windowInsets = TopBarWindowInsets,
            )
        }
        VocabeeTopBarType.CenterSingleRow -> {
            CenterAlignedTopAppBar(
                modifier = modifier,
                title = title,
                navigationIcon = {
                    NavigationIconView(
                        displayMode = config.navBackMode,
                        icon = navigationIcon,
                        containerColor = navigationContainerColor,
                        contentColor = navigationContentColor,
                        onClick = onNavigateBack,
                    )
                },
                actions = { ActionItems(actions) },
                colors = colors,
                windowInsets = TopBarWindowInsets,
            )
        }
    }
}

@Composable
private fun NavigationIconView(
    displayMode: VocabeeTopBarDisplayMode,
    icon: VocabeeNavigationIcon,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val contentDescription = when (icon) {
        VocabeeNavigationIcon.Back -> vocabeeString(VocabeeString.NavigationBack)
        VocabeeNavigationIcon.Close -> vocabeeString(VocabeeString.NavigationClose)
    }

    Row {
        Spacer(modifier = Modifier.width(8.dp))
        when (displayMode) {
            VocabeeTopBarDisplayMode.Visible -> {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .semantics { this.contentDescription = contentDescription }
                        .clickable(onClick = onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = containerColor,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            VocabeeNavigationIconView(
                                icon = icon,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
            VocabeeTopBarDisplayMode.Space -> Spacer(modifier = Modifier.width(48.dp))
            VocabeeTopBarDisplayMode.Gone -> Unit
        }
    }
}

@Composable
private fun VocabeeNavigationIconView(
    icon: VocabeeNavigationIcon,
    color: Color,
) {
    Canvas(
        modifier = Modifier.size(18.dp),
    ) {
        val strokeWidth = 2.2.dp.toPx()
        when (icon) {
            VocabeeNavigationIcon.Back -> {
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.64f, size.height * 0.2f),
                    end = Offset(size.width * 0.34f, size.height * 0.5f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.34f, size.height * 0.5f),
                    end = Offset(size.width * 0.64f, size.height * 0.8f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
            VocabeeNavigationIcon.Close -> {
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.28f, size.height * 0.28f),
                    end = Offset(size.width * 0.72f, size.height * 0.72f),
                    strokeWidth = stroke.width,
                    cap = stroke.cap,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.72f, size.height * 0.28f),
                    end = Offset(size.width * 0.28f, size.height * 0.72f),
                    strokeWidth = stroke.width,
                    cap = stroke.cap,
                )
            }
        }
    }
}

@Composable
private fun RowScope.ActionItems(actionItems: List<VocabeeTopBarAction>) {
    actionItems.forEach { actionItem ->
        when (actionItem) {
            is VocabeeTopBarAction.Text -> {
                TextButton(
                    onClick = actionItem.action,
                    enabled = actionItem.enabled,
                ) {
                    Text(
                        text = actionItem.text,
                        color = actionItem.textColor ?: MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            is VocabeeTopBarAction.Custom -> actionItem.content(this)
        }
    }
    Spacer(modifier = Modifier.width(8.dp))
}

private val TopBarHeight = 64.dp
private val TopBarWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
