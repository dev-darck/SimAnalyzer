@file:OptIn(ExperimentalComposeUiApi::class)

package com.project.analyzer

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowFocusListener

private const val ShowDelay = 50L

@Composable
fun ApplicationScope.CustomTray(
    overlayVisible: Boolean,
    onMainAction: () -> Unit,
    onOverlayToggle: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(Pair(0, 0)) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        if (!SystemTray.isSupported()) return@DisposableEffect onDispose { }

        val tray = SystemTray.getSystemTray()
        val url = javaClass.getResource("/icons/tray_icon.png")
        val image = if (url != null) {
            Toolkit.getDefaultToolkit().createImage(url)
        } else {
            Toolkit.getDefaultToolkit().createImage(ByteArray(0))
        }

        val trayIcon = TrayIcon(image, "SimAnalyzer").apply {
            isImageAutoSize = true
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    when (e.button) {
                        MouseEvent.BUTTON1 -> {
                            if (e.clickCount == 2) onMainAction()
                        }

                        MouseEvent.BUTTON3 -> {
                            menuPosition = Pair(e.xOnScreen, e.yOnScreen)
                            scope.launch {
                                delay(ShowDelay)
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
            onExit = ::exitApplication
        )
    }
}

@Composable
private fun TrayMenuWindow(
    position: Pair<Int, Int>,
    overlayVisible: Boolean,
    onDismiss: () -> Unit,
    onOverlayToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onExit: () -> Unit
) {
    val menuWidthDp = 220.dp
    val menuHeightDp = 200.dp
    val density = LocalDensity.current

    val (xDp, yDp) = remember(position.first, position.second, density.density) {
        val menuWidthPx = with(density) { menuWidthDp.roundToPx() }
        val menuHeightPx = with(density) { menuHeightDp.roundToPx() }

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
        size = DpSize(menuWidthDp, menuHeightDp),
        position = WindowPosition.Absolute(x = xDp, y = yDp)
    )

    DialogWindow(
        onCloseRequest = onDismiss,
        state = state,
        undecorated = true,
        transparent = true,
        resizable = false
    ) {
        DisposableEffect(window) {
            window.background = java.awt.Color(0, 0, 0, 0)
            window.isAlwaysOnTop = true

            var armed = false
            val focusListener = object : WindowFocusListener {
                override fun windowGainedFocus(e: WindowEvent?) {
                    armed = true
                }

                override fun windowLostFocus(e: WindowEvent?) {
                    if (armed) onDismiss()
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
                    } else false
                }
        ) {
            TrayMenuContent(
                overlayVisible = overlayVisible,
                onOverlayToggle = onOverlayToggle,
                onOpenApp = onOpenApp,
                onExit = onExit
            )
        }
    }
}

@Composable
private fun TrayMenuContent(
    overlayVisible: Boolean,
    onOverlayToggle: () -> Unit,
    onOpenApp: () -> Unit,
    onExit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E1E2E),
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "SimAnalyzer",
                color = Color(0xFFCDD6F4),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            Divider(
                color = Color(0xFF45475A),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TrayMenuItem(
                icon = Icons.Default.Home,
                text = "Open App",
                onClick = onOpenApp
            )

            TrayMenuItem(
                icon = if (overlayVisible) Icons.Default.Delete else Icons.Default.Create,
                text = if (overlayVisible) "Hide HUD" else "Show HUD",
                onClick = onOverlayToggle
            )

            Divider(
                color = Color(0xFF45475A),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            TrayMenuItem(
                icon = Icons.Default.Close,
                text = "Exit",
                onClick = onExit,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun TrayMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val alpha by animateFloatAsState(if (isHovered) 1f else 0f)

    val textColor = when {
        isDestructive -> Color(0xFFF38BA8)
        else -> Color(0xFFCDD6F4)
    }
    val hoverColor = when {
        isDestructive -> Color(0xFFF38BA8).copy(alpha = 0.15f)
        else -> Color(0xFF89B4FA).copy(alpha = 0.15f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(hoverColor.copy(alpha = hoverColor.alpha * alpha))
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp
        )
    }
}
