package com.analyzer.session.analysis.presentation.components.map.model

import androidx.compose.runtime.Immutable
import com.analyzer.session.analysis.presentation.components.map.support.SessionAnalysisTrackMapTraceLookup
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SessionAnalysisTrackMapPointerState(
    val selectedTraceAvailable: Boolean,
    val projectedSelectedTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val projectedSelectedTraceLookup: SessionAnalysisTrackMapTraceLookup,
    val hoverTrace: ImmutableList<SessionAnalysisFractionPointUi>,
    val hoverTraceLookup: SessionAnalysisTrackMapTraceLookup,
    val hoverDistancePx: Float,
    val hoverPreferenceIndex: Int?,
    val hoverLocalIndexWindow: Int,
    val hoverPreferenceFraction: Float?,
    val focusMode: Boolean,
)
