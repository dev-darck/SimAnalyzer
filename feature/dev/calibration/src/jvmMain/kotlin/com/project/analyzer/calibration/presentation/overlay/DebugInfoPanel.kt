package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.components.TimingInfo
import com.project.analyzer.calibration.presentation.components.fmt
import com.project.analyzer.calibration.presentation.overlay.state.OverlayDebugState
import com.project.analyzer.math.Vec2
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun DebugInfoPanel(state: OverlayDebugState, modifier: Modifier = Modifier) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    val extended = SimAnalyzerTheme.extended
    val carPos = state.carPos
    val carDir = state.carDir ?: Vec2.Up

    Column(
        modifier
            .padding(14.dp)
            .background(chrome.fillOverlay)
            .padding(10.dp),
    ) {
        Text(
            "Overlay Debug",
            color = material.onSurface,
            style = SimAnalyzerTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(8.dp))

        if (carPos == null) {
            Text(
                text = "Waiting for telemetry...",
                color = material.onSurface,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            return
        }

        TimingInfo("Lap", state.currentLapMs, state.lastLapMs, state.bestLapMs)

        val idx = state.currentSectorIndex ?: 0
        TimingInfo("S1", state.currentSectorMs.takeIf { idx == 1 }, state.lastS1Ms, state.bestS1Ms)
        TimingInfo("S2", state.currentSectorMs.takeIf { idx == 2 }, state.lastS2Ms, state.bestS2Ms)
        TimingInfo("S3", state.currentSectorMs.takeIf { idx == 3 }, state.lastS3Ms, state.bestS3Ms)

        Text(
            text = "track=${state.trackId ?: "?"}",
            color = material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = "speed=${state.speedKmh?.let { "%.1f".format(it) } ?: "?"} km/h",
            color = material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = "pos=(${fmt(carPos.x)}, ${fmt(carPos.y)})",
            color = material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = "dir=(${fmt(carDir.x)}, ${fmt(carDir.y)})",
            color = material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(8.dp))

        state.gateInfo.forEach { gi ->
            val inside = if (gi.isInside) "INSIDE" else "OUTSIDE"
            val planeDistance = gi.signedDistanceFromPlane?.let(::fmt) ?: "?"
            val directionDot = gi.directionDot?.let(::fmt) ?: "?"
            Text(
                text = "${gi.name}: $inside " +
                    "dist=${fmt(gi.distanceMeters)} " +
                    "dPlane=$planeDistance " +
                    "dPar=${fmt(gi.dParallel)} " +
                    "margin=${fmt(gi.margin)} " +
                    "dot=$directionDot " +
                    "crossed=${gi.isCrossed}",
                color = if (gi.isInside) extended.lightGreen else extended.red,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}
