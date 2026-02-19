@file:OptIn(ExperimentalComposeUiApi::class)

package com.project.analyzer.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.tray_icon
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.icons.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.imageResource
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Point
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener
import kotlin.time.Duration.Companion.milliseconds

private val SHOW_DELAY_MS = 50.milliseconds.inWholeMilliseconds
private val MENU_WIDTH_DP = 228.dp
private val MENU_HEIGHT_DP = 232.dp

@Composable
fun ApplicationScope.CustomTray(
    brandName: String,
    overlayVisible: Boolean,
    onMainAction: () -> Unit,
    onOverlayToggle: () -> Unit,
    onOpenSession: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Pair(0, 0)) }
    val scope = rememberCoroutineScope()

    val trayBitmap = imageResource(Res.drawable.tray_icon)

    DisposableEffect(Unit) {
        if (!SystemTray.isSupported()) return@DisposableEffect onDispose { }

        val tray = SystemTray.getSystemTray()
        val awt: Image = trayBitmap.toAwtImage()

        val trayIcon = TrayIcon(awt, BuildConfig.APP_NAME).apply {
            isImageAutoSize = false

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    when (e.button) {
                        MouseEvent.BUTTON1 -> {
                            if (e.clickCount == 2) onMainAction()
                        }

                        MouseEvent.BUTTON3 -> {
                            menuPosition = Pair(e.xOnScreen, e.yOnScreen)
                            scope.launch {
                                delay(SHOW_DELAY_MS)
                                showMenu = true
                            }
                        }
                    }
                }
            })
        }

        tray.add(trayIcon)
        onDispose { tray.remove(trayIcon) }
    }

    if (showMenu) {
        TrayMenuWindow(
            brandName = brandName,
            position = menuPosition,
            overlayVisible = overlayVisible,
            onDismiss = { showMenu = false },
            onOverlayToggle = {
                onOverlayToggle()
                showMenu = false
            },
            onOpenApp = {
                onMainAction()
                showMenu = false
            },
            onOpenSession = {
                onOpenSession()
                showMenu = false
            },
            onExit = ::exitApplication
        )
    }
}

@Composable
private fun TrayMenuWindow(
    brandName: String,
    position: Pair<Int, Int>,
    overlayVisible: Boolean,
    onDismiss: () -> Unit,
    onOverlayToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenSession: () -> Unit,
    onExit: () -> Unit
) {
    val density = LocalDensity.current

    val (xDp, yDp) = remember(position.first, position.second, density.density) {
        val menuWidthPx = with(density) { MENU_WIDTH_DP.roundToPx() }
        val menuHeightPx = with(density) { MENU_HEIGHT_DP.roundToPx() }

        val click = Point(position.first, position.second)

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screenBounds = ge.screenDevices
            .firstOrNull { it.defaultConfiguration.bounds.contains(click) }
            ?.defaultConfiguration?.bounds
            ?: ge.defaultScreenDevice.defaultConfiguration.bounds

        val xPx = (click.x - menuWidthPx)
            .coerceIn(screenBounds.x, screenBounds.x + screenBounds.width - menuWidthPx)

        val yPx = (click.y - menuHeightPx)
            .coerceIn(screenBounds.y, screenBounds.y + screenBounds.height - menuHeightPx)

        with(density) { xPx.toDp() to yPx.toDp() }
    }

    val state = rememberDialogState(
        size = DpSize(MENU_WIDTH_DP, MENU_HEIGHT_DP),
        position = WindowPosition.Absolute(x = xDp, y = yDp)
    )

    DialogWindow(
        onCloseRequest = onDismiss,
        state = state,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = true
    ) {
        DisposableEffect(window) {
            window.background = java.awt.Color(0, 0, 0, 0)
            window.isAlwaysOnTop = true

            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent?) = Unit

                override fun windowLostFocus(e: WindowEvent?) {
                    onDismiss()
                }
            }
            window.addWindowFocusListener(focusListener)

            window.toFront()
            window.requestFocus()

            onDispose {
                window.removeWindowFocusListener(focusListener)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                }
        ) {
            TrayMenuContent(
                brandName = brandName,
                overlayVisible = overlayVisible,
                onOverlayToggle = onOverlayToggle,
                onOpenApp = onOpenApp,
                onOpenSession = onOpenSession,
                onExit = onExit
            )
        }
    }
}

@Composable
private fun TrayMenuContent(
    brandName: String,
    overlayVisible: Boolean,
    onOverlayToggle: () -> Unit = {},
    onOpenApp: () -> Unit = {},
    onOpenSession: () -> Unit = {},
    onExit: () -> Unit = {}
) {
    val surface = SimAnalyzerTheme.material.surface
    val outline = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.75f)

    val textPrimary = SimAnalyzerTheme.material.onSurface
    val textSecondary = SimAnalyzerTheme.material.onSurfaceVariant

    val hoverPill = SimAnalyzerTheme.material.surfaceVariant
    val accent = SimAnalyzerTheme.material.primary

    val destructive = SimAnalyzerTheme.material.error
    val destructiveHover = SimAnalyzerTheme.material.error.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(14.dp),
        color = surface,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, outline),
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (overlayVisible) SimAnalyzerTheme.material.primary else textSecondary.copy(alpha = 0.6f))
                )

                Spacer(Modifier.width(8.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = brandName,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tray actions",
                        color = textSecondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                StatusPill(
                    text = if (overlayVisible) "ON" else "OFF",
                    background = if (overlayVisible)
                        SimAnalyzerTheme.material.secondaryContainer
                    else
                        SimAnalyzerTheme.material.surfaceVariant,
                    foreground = if (overlayVisible)
                        SimAnalyzerTheme.material.onSecondaryContainer
                    else
                        textSecondary
                )
            }

            HorizontalDivider(color = outline)
            Spacer(Modifier.height(4.dp))

            TrayMenuItem(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                text = "Open app",
                onClick = onOpenApp,
                hoverColor = hoverPill,
                textColor = textPrimary,
                iconTint = accent
            )

            TrayMenuItem(
                icon = Icons.Filled.Session,
                text = "Open session",
                onClick = onOpenSession,
                hoverColor = hoverPill,
                textColor = textPrimary,
                iconTint = accent
            )

            TrayMenuItem(
                icon = if (overlayVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                text = "HUD overlay",
                onClick = onOverlayToggle,
                hoverColor = hoverPill,
                textColor = textPrimary,
                iconTint = textSecondary,
                trailing = {
                    StatusPill(
                        text = if (overlayVisible) "ON" else "OFF",
                        background = if (overlayVisible)
                            SimAnalyzerTheme.material.primaryContainer
                        else
                            SimAnalyzerTheme.material.surfaceVariant,
                        foreground = if (overlayVisible)
                            SimAnalyzerTheme.material.onPrimaryContainer
                        else
                            textSecondary
                    )
                }
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = outline)
            Spacer(Modifier.height(4.dp))

            TrayMenuItem(
                icon = Icons.Outlined.PowerSettingsNew,
                text = "Exit",
                onClick = onExit,
                hoverColor = destructiveHover,
                textColor = destructive,
                iconTint = destructive
            )
        }
    }
}

@Composable
private fun TrayMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    hoverColor: Color,
    textColor: Color,
    iconTint: Color,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    val bg by animateColorAsState(
        targetValue = if (hovered) hoverColor.copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = tween(120)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp)
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    foreground: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun TrayMenuPreview() {
    SimAnalyzerTheme {
        TrayMenuContent(
            brandName = "SimAnalyzer",
            overlayVisible = true
        )
    }
}
