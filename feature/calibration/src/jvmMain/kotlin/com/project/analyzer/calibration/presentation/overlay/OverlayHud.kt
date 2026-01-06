package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.OverlayRegionController
import com.project.analyzer.calibration.presentation.overlay.state.OverlayDebugState
import com.project.analyzer.math.Vec2

@Composable
fun OverlayHud(
    state: OverlayDebugState,
    hitRegions: HitRegions,
    controller: OverlayRegionController,
) {
    val carPos = state.carPos ?: return
    val carDir = state.carDir ?: Vec2(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        DraggableBox(
            regionKey = "hud",
            initialX = 20,
            initialY = 20,
            hitRegions = hitRegions,
            controller = controller,
        ) {
            DebugInfoPanel(state)
        }

        DraggableBox(
            regionKey = "minimap",
            initialX = 900,
            initialY = 520,
            hitRegions = hitRegions,
            controller = controller,
        ) {
            MiniMap(
                carPos = carPos,
                carDir = carDir,
                gates = state.gates,
                lastCapturePoint = state.lastCapturePoint,
                pendingCapturePosition = state.pendingCapturePosition,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
