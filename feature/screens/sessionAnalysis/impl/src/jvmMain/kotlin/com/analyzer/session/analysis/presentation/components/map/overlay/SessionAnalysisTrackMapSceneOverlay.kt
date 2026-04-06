package com.analyzer.session.analysis.presentation.components.map.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.components.map.model.sectorAccent
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewActiveSample
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewPalette
import com.analyzer.session.analysis.presentation.components.map.preview.sessionAnalysisTrackMapPreviewSectorMarkers
import com.analyzer.session.analysis.presentation.components.map.support.resolveTrackMapSectorLabelBorderColor
import com.analyzer.session.analysis.presentation.components.map.support.sectorLabelChipSizeDp
import com.analyzer.session.analysis.presentation.formatter.formatSpeed
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

@Composable
internal fun SessionAnalysisTrackMapSpeedOverlay(
    activeMarkerInView: SessionAnalysisFractionPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    overlayBg: Color,
    overlayFg: Color,
) {
    if (activeMarkerInView == null || activeSample == null) return

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (activeMarkerInView.x + 12f).roundToInt(),
                    y = (activeMarkerInView.y - 14f).roundToInt(),
                )
            },
        shape = SimAnalyzerTheme.corners.control,
        color = overlayBg,
    ) {
        Text(
            text = formatSpeed(activeSample.speedKmh),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            color = overlayFg,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun SessionAnalysisTrackMapSectorLabels(
    markers: ImmutableList<SessionAnalysisTrackSectorMarker>,
    palette: SessionAnalysisTrackMapPalette,
    markerScale: Float = 1f,
) {
    val density = LocalDensity.current
    val markerSizeDp = sectorLabelChipSizeDp * markerScale.coerceIn(0.55f, 1f)
    val sectorLabelHalfSizePx = with(density) { markerSizeDp.toPx() * 0.5f }
    markers.forEach { marker ->
        val accent = palette.sectorAccent(marker.label)
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (marker.point.x - sectorLabelHalfSizePx).roundToInt(),
                        y = (marker.point.y - sectorLabelHalfSizePx).roundToInt(),
                    )
                }
                .size(markerSizeDp),
            shape = SimAnalyzerTheme.corners.control,
            color = palette.sectorLabelBg,
            border = BorderStroke(1.dp, resolveTrackMapSectorLabelBorderColor(accent)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = marker.label,
                    color = accent,
                    style = SimAnalyzerTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisTrackMapSceneOverlayPreview() {
    SimAnalyzerTheme {
        val palette = sessionAnalysisTrackMapPreviewPalette()
        Box(modifier = Modifier.size(200.dp)) {
            SessionAnalysisTrackMapSpeedOverlay(
                activeMarkerInView = sessionAnalysisTrackMapPreviewActivePoint().let { point ->
                    SessionAnalysisFractionPointUi(
                        fraction = point.fraction,
                        x = 72f,
                        y = 92f,
                        frameId = point.selectedFrameId,
                    )
                },
                activeSample = sessionAnalysisTrackMapPreviewActiveSample(),
                overlayBg = palette.overlayBg,
                overlayFg = palette.overlayFg,
            )
            SessionAnalysisTrackMapSectorLabels(
                markers = sessionAnalysisTrackMapPreviewSectorMarkers(),
                palette = palette,
                markerScale = 0.72f,
            )
        }
    }
}
