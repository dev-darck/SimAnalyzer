package com.project.analyzer.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.FilterNone
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowScope
import com.project.analyzer.app.win.hittest.winCaptionBarRect
import com.project.analyzer.app.win.hittest.winCloseButtonRect
import com.project.analyzer.app.win.hittest.winExcludeFromCaption
import com.project.analyzer.app.win.hittest.winMaximizeButtonRect
import com.project.analyzer.app.win.hittest.winMinimizeButtonRect
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.utils.logger.logger
import java.awt.Frame
import java.awt.event.WindowStateListener

@Composable
fun WindowScope.AppTitleBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onCloseRequest: () -> Unit,
    decorator: FrameDecoratorState,
    modifier: Modifier = Modifier,
    appName: String = BuildConfig.APP_NAME,
) {
    val m = SimAnalyzerTheme.material
    val frame = window as? Frame
    val reg = decorator.win.hitTestRegistry

    var isMaximized by remember { mutableStateOf(false) }
    val canMaximize = remember(frame) { frame?.isResizable == true }
    val canMinimize = remember(frame) { frame != null }

    DisposableEffect(frame) {
        if (frame == null) return@DisposableEffect onDispose { }
        fun update() {
            isMaximized = (frame.extendedState and Frame.MAXIMIZED_BOTH) != 0
        }
        update()
        val l = WindowStateListener { update() }
        frame.addWindowStateListener(l)
        onDispose { frame.removeWindowStateListener(l) }
    }

    fun minimize() {
        logger.info { "Window: minimize" }
        frame?.extendedState = (frame.extendedState or Frame.ICONIFIED)
    }

    fun toggleMaximize() {
        val f = frame ?: return
        val max = (f.extendedState and Frame.MAXIMIZED_BOTH) != 0
        logger.info { "Window: toggleMaximize (wasMax=$max)" }
        f.extendedState = if (max) Frame.NORMAL else Frame.MAXIMIZED_BOTH
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(other = modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(decorator.captionHeight)
                .background(m.surface)
                .winCaptionBarRect(reg, key = "captionBar")
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(10.dp))

            Text(text = appName, color = m.onSurface, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.winExcludeFromCaption(reg, key = "navButtons"),
            ) {
                NavButtons(
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onBack = {
                        logger.info { "TitleBar: back" }
                        onBack()
                    },
                    onForward = {
                        logger.info { "TitleBar: forward" }
                        onForward()
                    },
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier.winExcludeFromCaption(reg, key = "sysButtons"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TitleBarWinButton(
                    enabled = canMinimize,
                    onClick = ::minimize,
                    hoverBg = m.surfaceVariant.copy(alpha = 0.7f),
                    iconTint = m.onSurfaceVariant,
                    modifier = Modifier.winMinimizeButtonRect(reg, key = "minBtn"),
                ) {
                    Icon(Icons.Outlined.Remove, "Minimize", tint = it, modifier = Modifier.size(18.dp))
                }

                TitleBarWinButton(
                    enabled = canMaximize,
                    onClick = ::toggleMaximize,
                    hoverBg = m.surfaceVariant.copy(alpha = 0.7f),
                    iconTint = m.onSurfaceVariant,
                    modifier = Modifier.winMaximizeButtonRect(reg, key = "maxBtn"),
                ) {
                    Icon(
                        imageVector = if (isMaximized) Icons.Outlined.FilterNone else Icons.Outlined.CropSquare,
                        contentDescription = "Maximize",
                        tint = it,
                        modifier = Modifier.size(18.dp),
                    )
                }

                TitleBarWinButton(
                    enabled = true,
                    onClick = {
                        logger.info { "Window: close" }
                        onCloseRequest()
                    },
                    hoverBg = m.error.copy(alpha = 0.22f),
                    iconTint = m.onSurface,
                    modifier = Modifier.winCloseButtonRect(reg, key = "closeBtn"),
                ) {
                    Icon(Icons.Outlined.Close, "Close", tint = it, modifier = Modifier.size(18.dp))
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(m.outlineVariant.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun NavButtons(canGoBack: Boolean, canGoForward: Boolean, onBack: () -> Unit, onForward: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TitleBarPillButton(enabled = canGoBack, onClick = onBack) { tint ->
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(6.dp))

        TitleBarPillButton(enabled = canGoForward, onClick = onForward) { tint ->
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Forward",
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun TitleBarPillButton(enabled: Boolean, onClick: () -> Unit, icon: @Composable (tint: Color) -> Unit) {
    val m = SimAnalyzerTheme.material
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            hovered -> m.surfaceVariant.copy(alpha = 0.65f)
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "titleNavBg",
    )

    val tint by animateColorAsState(
        targetValue = when {
            !enabled -> m.onSurfaceVariant.copy(alpha = 0.35f)
            hovered -> m.onSurface
            else -> m.onSurfaceVariant
        },
        animationSpec = tween(120),
        label = "titleNavTint",
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .hoverable(interaction)
            .onClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(tint)
    }
}

@Composable
private fun TitleBarWinButton(
    enabled: Boolean,
    onClick: () -> Unit,
    hoverBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = if (enabled && hovered) hoverBg else Color.Transparent,
        animationSpec = tween(120),
        label = "titleWinBg",
    )

    val tint by animateColorAsState(
        targetValue = if (enabled) iconTint else iconTint.copy(alpha = 0.35f),
        animationSpec = tween(120),
        label = "titleWinTint",
    )

    Box(
        modifier = modifier
            .size(width = 42.dp, height = 38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .hoverable(interaction)
            .onClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(tint)
    }
}
