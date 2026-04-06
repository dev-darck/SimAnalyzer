package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class SessionAnalysisDiagnosticSummaryUi(
    val overallScore: Int = 0,
    val drivingScore: Int = 0,
    val setupScore: Int = 0,
    val topDrivingIssues: ImmutableList<DiagnosticIssueUi> = persistentListOf(),
    val topSetupIssues: ImmutableList<DiagnosticIssueUi> = persistentListOf(),
    val cornerScores: ImmutableList<CornerScoreUi> = persistentListOf(),
)
