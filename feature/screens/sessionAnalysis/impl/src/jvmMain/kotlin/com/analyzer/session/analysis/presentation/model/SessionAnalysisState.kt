package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import com.analyzer.session.analysis.presentation.model.share.SessionAnalysisShareDialogUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisStudioState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisState(
    val isLoading: Boolean = true,
    val error: SessionAnalysisError? = null,
    val screenMode: SessionAnalysisScreenMode = SessionAnalysisScreenMode.Analysis,
    val header: SessionAnalysisHeaderUi? = null,
    val summary: SessionAnalysisSummaryUi? = null,
    val sessionOptions: ImmutableList<SessionAnalysisSessionOptionUi> = persistentListOf(),
    val lapOptions: ImmutableList<SessionAnalysisLapOptionUi> = persistentListOf(),
    val sectors: ImmutableList<SessionAnalysisSectorUi> = persistentListOf(),
    val selectedSample: SessionAnalysisSampleUi? = null,
    val selectedComparisonPoint: SessionAnalysisComparisonPointUi? = null,
    val lapCoach: SessionAnalysisLapCoachUi? = null,
    val diagnosticSummary: SessionAnalysisDiagnosticSummaryUi? = null,
    val highlights: ImmutableList<SessionAnalysisHighlightUi> = persistentListOf(),
    val laps: ImmutableList<SessionAnalysisLapSummaryUi> = persistentListOf(),
    val referenceLapSummary: SessionAnalysisLapSummaryUi? = null,
    val studio: SessionAnalysisStudioState? = null,
    val selectedSegmentId: Long? = null,
    val selectedLapNumber: Int? = null,
    val referenceLapNumber: Int? = null,
    val referenceLapIsCustom: Boolean = false,
    val hasExternalReference: Boolean = false,
    val selectedFrameId: Long? = null,
    val shareDialog: SessionAnalysisShareDialogUi = SessionAnalysisShareDialogUi(),
)
