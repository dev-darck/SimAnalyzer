package com.analyzer.session.analysis.presentation.components.map.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SessionAnalysisTrackMapFocusState(
    val selectedTraceAvailable: Boolean,
    val hoverTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val activeMarker: SessionAnalysisFractionPointUi?,
    val displayMarkerDirection: Offset?,
    val cameraAnchor: Offset?,
    val cameraFocusPoint: Offset?,
    val cameraZoom: Float,
    val overlayDimAlpha: Float,
    val hoverPreferenceIndex: Int?,
    val hoverLocalIndexWindow: Int,
    val hoverPreferenceFraction: Float?,
    val selectedTrailTrace: ImmutableList<SessionAnalysisFractionPointUi>,
)
