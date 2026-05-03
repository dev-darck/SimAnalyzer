package com.analyzer.session.analysis.presentation.components.hero

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.model.DiagnosticIssueUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosisSourceUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisHighlightCategoryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisScreenMode
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.persistentListOf

@Preview
@Composable
internal fun SessionAnalysisWorkspaceHeroPreview() {
    val activePoint = SessionAnalysisComparisonPointUi(
        fraction = 0.42f,
        trackPosition = 0.42f,
        selectedFrameId = 1201L,
        selectedElapsedMs = 52_134,
        referenceElapsedMs = 52_312,
        deltaMs = -178,
        selectedSpeedKmh = 146f,
        referenceSpeedKmh = 142f,
        selectedThrottle = 0.84f,
        referenceThrottle = 0.79f,
        selectedBrake = 0.06f,
        referenceBrake = 0.12f,
        selectedGear = 4,
        referenceGear = 4,
        selectedRpm = 7_120f,
        referenceRpm = 6_980f,
        selectedFuelLiters = 34.6f,
        referenceFuelLiters = 34.8f,
    )
    val activeSample = SessionAnalysisSampleUi(
        frameId = 1201L,
        lapNumber = 7,
        sectorIndex = 2,
        sampleIndexInLap = 418,
        trackPosition = 0.42f,
        speedKmh = 146f,
        gear = 4,
        rpm = 7_120f,
        throttle = 0.84f,
        brake = 0.06f,
        fuelLiters = 34.6f,
    )

    SimAnalyzerTheme {
        SessionAnalysisWorkspaceHero(
            trackCanvasState = null,
            screenMode = SessionAnalysisScreenMode.Analysis,
            activePoint = activePoint,
            activeSample = activeSample,
            diagnosticSummary = SessionAnalysisDiagnosticSummaryUi(
                overallScore = 78,
                drivingScore = 82,
                setupScore = 69,
                topDrivingIssues = persistentListOf(
                    DiagnosticIssueUi(
                        title = "Brake release is late",
                        description = "Release pressure earlier into the apex.",
                        recommendation = "Blend brake release before initial rotation.",
                        priority = 1,
                        source = SessionAnalysisDiagnosisSourceUi.DrivingStyle,
                        potentialTimeGainMs = 182,
                        category = SessionAnalysisHighlightCategoryUi.TimeLoss,
                        cornerNumber = 7,
                        trackPosition = 0.42f,
                    ),
                ),
                topSetupIssues = persistentListOf(
                    DiagnosticIssueUi(
                        title = "Rear is nervous on exit",
                        description = "Rear axle unloads too abruptly on power.",
                        recommendation = "Rebalance rear damping and diff preload.",
                        priority = 2,
                        source = SessionAnalysisDiagnosisSourceUi.CarSetup,
                        potentialTimeGainMs = 96,
                        category = SessionAnalysisHighlightCategoryUi.Oversteer,
                        cornerNumber = 9,
                        trackPosition = 0.61f,
                    ),
                ),
            ),
            diagnosticsLoading = false,
            highlightsCount = 3,
            selectedLapNumber = 7,
            referenceLapNumber = 3,
            selectionLocked = false,
            cursorFraction = activePoint.fraction,
            cursorFrameId = activePoint.selectedFrameId,
            focusMode = false,
            collapsed = false,
            modifier = Modifier
                .width(1180.dp)
                .height(540.dp),
        )
    }
}
