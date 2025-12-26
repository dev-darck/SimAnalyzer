package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import com.project.analyzer.calibration.OverlayRegionController
import java.util.prefs.Preferences
import kotlin.math.roundToInt

private object OverlayPrefs {
    private val prefs: Preferences = Preferences.userNodeForPackage(OverlayPrefs::class.java)

    fun savePosition(key: String, x: Float, y: Float) {
        prefs.putFloat("${key}_x", x)
        prefs.putFloat("${key}_y", y)
    }

    fun loadX(key: String, default: Float): Float = prefs.getFloat("${key}_x", default)
    fun loadY(key: String, default: Float): Float = prefs.getFloat("${key}_y", default)
}

@Composable
fun DraggableBox(
    regionKey: String,
    initialX: Int,
    initialY: Int,
    hitRegions: HitRegions,
    controller: OverlayRegionController,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var offsetX by remember {
        mutableStateOf(OverlayPrefs.loadX(regionKey, initialX.toFloat()))
    }
    var offsetY by remember {
        mutableStateOf(OverlayPrefs.loadY(regionKey, initialY.toFloat()))
    }

    var dragging by remember { mutableStateOf(false) }
    var lastRect by remember { mutableStateOf<IntRect?>(null) }

    DisposableEffect(regionKey) {
        onDispose { hitRegions.remove(regionKey) }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .onGloballyPositioned { coords ->
                val p = coords.positionInRoot()
                val s = coords.size
                val rect = IntRect(
                    left = p.x.toInt(),
                    top = p.y.toInt(),
                    right = (p.x + s.width).toInt(),
                    bottom = (p.y + s.height).toInt()
                )
                lastRect = rect

                if (!dragging) {
                    hitRegions.put(regionKey, rect)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        controller.beginDrag()
                    },
                    onDragCancel = {
                        dragging = false
                        lastRect?.let { hitRegions.put(regionKey, it) }
                        controller.endDrag()
                        OverlayPrefs.savePosition(regionKey, offsetX, offsetY)
                    },
                    onDragEnd = {
                        dragging = false
                        lastRect?.let { hitRegions.put(regionKey, it) }
                        controller.endDrag()
                        OverlayPrefs.savePosition(regionKey, offsetX, offsetY)
                    },
                    onDrag = { change, dragAmount ->
                        if (change.positionChange() != Offset.Zero) change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        content = content
    )
}
