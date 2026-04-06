package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisInteractionState(
    val visibleSamples: ImmutableList<SessionAnalysisSampleUi> = persistentListOf(),
    val comparisonFractions: ImmutableList<Float> = persistentListOf(),
    val resolvedSamplePositions: ImmutableList<Float> = persistentListOf(),
    val sampleIndexByFrameId: ImmutableMap<Long, Int> = persistentHashMapOf(),
)
