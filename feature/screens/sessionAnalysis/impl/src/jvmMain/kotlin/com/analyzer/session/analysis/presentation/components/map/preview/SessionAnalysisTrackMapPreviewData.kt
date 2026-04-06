package com.analyzer.session.analysis.presentation.components.map.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackMapPalette
import com.analyzer.session.analysis.presentation.components.map.model.SessionAnalysisTrackSectorMarker
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal fun sessionAnalysisTrackMapPreviewCanvasState(): SessionAnalysisTrackCanvasState {
    val centerLine = persistentListOf(
        SessionAnalysisFractionPointUi(0f, 10f, 60f, 1000L),
        SessionAnalysisFractionPointUi(0.17f, 40f, 20f, 1010L),
        SessionAnalysisFractionPointUi(0.34f, 95f, 14f, 1020L),
        SessionAnalysisFractionPointUi(0.51f, 132f, 58f, 1030L),
        SessionAnalysisFractionPointUi(0.68f, 112f, 112f, 1040L),
        SessionAnalysisFractionPointUi(0.85f, 54f, 126f, 1050L),
        SessionAnalysisFractionPointUi(1f, 10f, 60f, 1060L),
    )
    val leftEdge = persistentListOf(
        SessionAnalysisFractionPointUi(0f, 6f, 64f),
        SessionAnalysisFractionPointUi(0.17f, 36f, 16f),
        SessionAnalysisFractionPointUi(0.34f, 98f, 8f),
        SessionAnalysisFractionPointUi(0.51f, 138f, 56f),
        SessionAnalysisFractionPointUi(0.68f, 116f, 118f),
        SessionAnalysisFractionPointUi(0.85f, 50f, 132f),
        SessionAnalysisFractionPointUi(1f, 6f, 64f),
    )
    val rightEdge = persistentListOf(
        SessionAnalysisFractionPointUi(0f, 14f, 56f),
        SessionAnalysisFractionPointUi(0.17f, 44f, 24f),
        SessionAnalysisFractionPointUi(0.34f, 92f, 20f),
        SessionAnalysisFractionPointUi(0.51f, 126f, 60f),
        SessionAnalysisFractionPointUi(0.68f, 108f, 106f),
        SessionAnalysisFractionPointUi(0.85f, 58f, 120f),
        SessionAnalysisFractionPointUi(1f, 14f, 56f),
    )
    val referenceTrace = centerLine.map { point ->
        point.copy(x = point.x - 3f, y = point.y + 2f)
    }.toPersistentList()
    val idealLine = centerLine.map { point ->
        point.copy(x = point.x + 2f, y = point.y - 3f)
    }.toPersistentList()
    val selectedTrace = centerLine

    return SessionAnalysisTrackCanvasState(
        centerLine = centerLine,
        trackLeftEdge = leftEdge,
        trackRightEdge = rightEdge,
        idealLine = idealLine,
        selectedTrace = selectedTrace,
        referenceTrace = referenceTrace,
        interactionTrace = selectedTrace,
        sectors = persistentListOf(
            SessionAnalysisSectorUi("S1", 0f, 0.33f, 0.16f),
            SessionAnalysisSectorUi("S2", 0.33f, 0.66f, 0.49f),
            SessionAnalysisSectorUi("S3", 0.66f, 1f, 0.83f),
        ),
        cornerScores = persistentListOf(
            CornerScoreUi(cornerNumber = 3, score = 78, trackPosition = 0.34f),
            CornerScoreUi(cornerNumber = 7, score = 69, trackPosition = 0.68f),
        ),
        cornerMarkers = persistentListOf(
            CornerScoreUi(cornerNumber = 3, score = 78, trackPosition = 0.34f),
            CornerScoreUi(cornerNumber = 7, score = 69, trackPosition = 0.68f),
        ),
        issueMarkers = persistentListOf(
            SessionAnalysisHighlightUi(
                id = "issue-1",
                category = SessionAnalysisHighlightCategory.TimeLoss,
                severity = SessionAnalysisHighlightSeverity.Warning,
                lapNumber = 7,
                title = "Late release",
                description = "Brake release is late into corner.",
                trackPosition = 0.34f,
                cornerNumber = 3,
                diagnosisSource = SessionAnalysisDiagnosisSource.DrivingStyle,
            ),
        ),
        selectedTrailStartFraction = 0.18f,
        minX = 0f,
        minY = 0f,
        maxX = 145f,
        maxY = 140f,
        trackSurfaceWidthMeters = 12f,
    )
}

internal fun sessionAnalysisTrackMapPreviewActivePoint(): SessionAnalysisComparisonPointUi =
    SessionAnalysisComparisonPointUi(
        fraction = 0.34f,
        trackPosition = 0.34f,
        selectedFrameId = 1020L,
        deltaMs = -92,
        selectedSpeedKmh = 146f,
        referenceSpeedKmh = 142f,
    )

internal fun sessionAnalysisTrackMapPreviewActiveSample(): SessionAnalysisSampleUi = SessionAnalysisSampleUi(
    frameId = 1020L,
    lapNumber = 7,
    sectorIndex = 1,
    sampleIndexInLap = 120,
    trackPosition = 0.34f,
    speedKmh = 146f,
    gear = 4,
    rpm = 7120f,
    throttle = 0.82f,
    brake = 0.08f,
)

internal fun sessionAnalysisTrackMapPreviewPalette(): SessionAnalysisTrackMapPalette = SessionAnalysisTrackMapPalette(
    panelBg = Color(0xFF11161B).copy(alpha = 0.38f),
    canvasBg = Color(0xFF2D353F).copy(alpha = 0.18f),
    trackSurfaceColor = Color(0xFF586574).copy(alpha = 0.62f),
    trackEdgeColor = Color(0xFFF3F6FA).copy(alpha = 0.22f),
    lineCasingColor = Color(0xFF0D1117).copy(alpha = 0.88f),
    referenceColor = Color(0xFFB8C3CF).copy(alpha = 0.66f),
    idealColor = Color(0xFF46C9D8).copy(alpha = 0.96f),
    selectedColor = Color(0xFFF0B84E).copy(alpha = 0.98f),
    markerRingColor = Color(0xFFF3F6FA).copy(alpha = 0.92f),
    markerInnerColor = Color(0xFFF0B84E).copy(alpha = 0.98f),
    overlayBg = Color(0xFF111922).copy(alpha = 0.92f),
    overlayFg = Color(0xFFF3F6FA),
    overlayDimAlpha = 1f,
    sectorLabelBg = Color(0xFF11161B).copy(alpha = 0.90f),
    sectorSfColor = Color(0xFFF3F6FA),
    sectorS1Color = Color(0xFFF0B84E),
    sectorS2Color = Color(0xFF46C9D8),
    sectorOtherColor = Color(0xFF2EB29C),
)

internal fun sessionAnalysisTrackMapPreviewSectorMarkers(): ImmutableList<SessionAnalysisTrackSectorMarker> =
    persistentListOf(
        SessionAnalysisTrackSectorMarker(
            label = "S1",
            point = Offset(x = 46f, y = 26f),
        ),
        SessionAnalysisTrackSectorMarker(
            label = "S2",
            point = Offset(x = 124f, y = 62f),
        ),
    )
