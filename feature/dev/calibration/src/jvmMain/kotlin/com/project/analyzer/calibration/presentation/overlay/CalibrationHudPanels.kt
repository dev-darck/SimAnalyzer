package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.hud.api.HudAnchor
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(HudScope::class)
@ContributesIntoSet(HudScope::class)
class CalibrationDebugHudPanel(
    private val overlayDebugBus: OverlayDebugBus
) : HudPanel {

    override val id: String = "calibration_debug"
    override val description: String = "Calibration debug telemetry"
    override val defaultAnchor: HudAnchor = HudAnchor.TopLeft
    override val defaultMarginPx: IntOffset = IntOffset(24, 24)
    override val isDevOnly: Boolean = true
    override val zIndex: Int = 10

    @Composable
    override fun Content(modifier: Modifier) {
        val state by overlayDebugBus.state.collectAsStateWithLifecycle()
        DebugInfoPanel(
            state = state,
            modifier = modifier
        )
    }
}

@Inject
@SingleIn(HudScope::class)
@ContributesIntoSet(HudScope::class)
class CalibrationMiniMapHudPanel(
    private val overlayDebugBus: OverlayDebugBus
) : HudPanel {

    override val id: String = "calibration_minimap"
    override val description: String = "Calibration minimap view"
    override val defaultAnchor: HudAnchor = HudAnchor.BottomRight
    override val defaultMarginPx: IntOffset = IntOffset(32, 32)
    override val isDevOnly: Boolean = true
    override val zIndex: Int = 11

    @Composable
    override fun Content(modifier: Modifier) {
        val state by overlayDebugBus.state.collectAsStateWithLifecycle()
        val carPos = state.carPos
        val carDir = state.carDir

        if (carPos == null || carDir == null) {
            Box(
                modifier = modifier
                    .background(Color(0x66000000))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            return
        }

        MiniMap(
            carPos = carPos,
            carDir = carDir,
            gates = state.gates,
            lastCapturePoint = state.lastCapturePoint,
            pendingCapturePosition = state.pendingCapturePosition,
            modifier = modifier
        )
    }
}
