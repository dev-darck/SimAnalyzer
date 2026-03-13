package com.project.analyzer.app.frame.win.hittest

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

private fun Rect.toIntRect(): IntRect =
    IntRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

fun Modifier.winCaptionBarRect(reg: WindowsHitTestRegistry, key: String): Modifier = composed {
    DisposableEffect(reg, key) { onDispose { reg.clearCaptionBar() } }
    onGloballyPositioned { reg.setCaptionBar(it.boundsInWindow().toIntRect()) }
}

fun Modifier.winExcludeFromCaption(reg: WindowsHitTestRegistry, key: String): Modifier = composed {
    DisposableEffect(reg, key) { onDispose { reg.clearExclude(key) } }
    onGloballyPositioned { reg.setExclude(key, it.boundsInWindow().toIntRect()) }
}

fun Modifier.winMinimizeButtonRect(reg: WindowsHitTestRegistry, key: String): Modifier = composed {
    DisposableEffect(reg, key) { onDispose { reg.clearMinButton() } }
    onGloballyPositioned { reg.setMinButton(it.boundsInWindow().toIntRect()) }
}

fun Modifier.winMaximizeButtonRect(reg: WindowsHitTestRegistry, key: String): Modifier = composed {
    DisposableEffect(reg, key) { onDispose { reg.clearMaxButton() } }
    onGloballyPositioned { reg.setMaxButton(it.boundsInWindow().toIntRect()) }
}

fun Modifier.winCloseButtonRect(reg: WindowsHitTestRegistry, key: String): Modifier = composed {
    DisposableEffect(reg, key) { onDispose { reg.clearCloseButton() } }
    onGloballyPositioned { reg.setCloseButton(it.boundsInWindow().toIntRect()) }
}
