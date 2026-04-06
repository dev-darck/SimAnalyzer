package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSectorUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisTrackCanvasState(
    val centerLine: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val trackLeftEdge: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val trackRightEdge: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val idealLine: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val pitLine: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val selectedTrace: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val referenceTrace: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val interactionTrace: ImmutableList<SessionAnalysisFractionPointUi> = persistentListOf(),
    val sectors: ImmutableList<SessionAnalysisSectorUi> = persistentListOf(),
    val cornerScores: ImmutableList<CornerScoreUi> = persistentListOf(),
    val cornerMarkers: ImmutableList<CornerScoreUi> = persistentListOf(),
    val issueMarkers: ImmutableList<SessionAnalysisHighlightUi> = persistentListOf(),
    val selectedTrailStartFraction: Float? = null,
    val minX: Float = 0f,
    val minY: Float = 0f,
    val maxX: Float = 1f,
    val maxY: Float = 1f,
    val trackSurfaceWidthMeters: Float = 19f,
)
