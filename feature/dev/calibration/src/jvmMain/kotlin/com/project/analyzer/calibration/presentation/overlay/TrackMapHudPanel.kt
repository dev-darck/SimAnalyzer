package com.project.analyzer.calibration.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.trackmap.domain.TrackMapCaptureController
import com.analyzer.trackmap.presentation.TrackMapPreview
import com.analyzer.trackmap.presentation.toTrackMapPreviewUi
import com.project.analyzer.hud.api.HudAnchor
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.launch

@Inject
@SingleIn(HudScope::class)
@ContributesIntoSet(HudScope::class)
@Suppress("unused")
class TrackMapHudPanel(private val controller: TrackMapCaptureController) : HudPanel {

    override val id: String = "track_map_builder"
    override val description: String = "Track map capture overlay"
    override val defaultAnchor: HudAnchor = HudAnchor.BottomLeft
    override val defaultMarginPx: IntOffset = IntOffset(32, 32)
    override val isDevOnly: Boolean = true
    override val zIndex: Int = 12

    @Composable
    override fun Content(modifier: Modifier) {
        val state by controller.state.collectAsStateWithLifecycle()
        val coroutineScope = rememberCoroutineScope()

        Box(modifier = modifier.size(420.dp)) {
            TrackMapPreview(
                state = state.toTrackMapPreviewUi(),
                modifier = Modifier.matchParentSize(),
                backgroundAlpha = 0.7f,
                borderAlpha = 0.5f,
                showStatus = true,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(SimAnalyzerTheme.shapes.medium)
                    .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.75f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.recording) {
                    Button(onClick = { coroutineScope.launch { controller.stop() } }) {
                        Text(
                            text = "Stop",
                            style = SimAnalyzerTheme.typography.labelMedium,
                        )
                    }
                } else {
                    Button(onClick = { coroutineScope.launch { controller.start() } }) {
                        Text(
                            text = "Start",
                            style = SimAnalyzerTheme.typography.labelMedium,
                        )
                    }
                }

                Button(
                    onClick = { coroutineScope.launch { controller.markPitEntry() } },
                    enabled = state.recording,
                ) {
                    Text(
                        text = "Pit in",
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }

                Button(
                    onClick = { coroutineScope.launch { controller.markPitExit() } },
                    enabled = state.recording,
                ) {
                    Text(
                        text = "Pit out",
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}
