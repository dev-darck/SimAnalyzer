package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionAnalysisTelemetryOverviewHighlightStageTest {

    private val stage = SessionAnalysisTelemetryOverviewHighlightStage()

    @Test
    fun `execute creates overview drafts for major telemetry cues`() = runBlocking {
        val result = stage.execute(
            highlightContext(
                samples = listOf(
                    highlightSample(
                        lapNumber = 1,
                        sampleIndexInLap = 1,
                        trackPosition = 0.12f,
                        speedKmh = 282f,
                    ),
                    highlightSample(
                        lapNumber = 1,
                        sampleIndexInLap = 2,
                        trackPosition = 0.28f,
                        speedKmh = 238f,
                        throttle = 0.96f,
                        brake = 0.04f,
                    ),
                    highlightSample(
                        lapNumber = 2,
                        sampleIndexInLap = 3,
                        trackPosition = 0.41f,
                        speedKmh = 214f,
                        brake = 0.81f,
                    ),
                    highlightSample(
                        lapNumber = 2,
                        sampleIndexInLap = 4,
                        trackPosition = 0.57f,
                        speedKmh = 167f,
                    ),
                    highlightSample(
                        lapNumber = 3,
                        sampleIndexInLap = 5,
                        trackPosition = 0.58f,
                        speedKmh = 169f,
                    ),
                ),
                cornerReport = SessionAnalysisCornerAnalysisReport(
                    corners = listOf(
                        SessionAnalysisCornerAnalysis(
                            segmentId = 1L,
                            lapNumber = 2,
                            cornerNumber = 4,
                            score = 61,
                            representativeSample = highlightSample(
                                lapNumber = 2,
                                sampleIndexInLap = 4,
                                trackPosition = 0.57f,
                                speedKmh = 167f,
                            ),
                            startTrackPosition = 0.48f,
                            apexTrackPosition = 0.57f,
                            endTrackPosition = 0.68f,
                            trailBrakingScore = 48,
                            timeLossMs = 146,
                            apexClassification = SessionAnalysisCornerApexClassification.GoodApex,
                            entrySpeedDeltaKmh = -7f,
                            exitSpeedDeltaKmh = -9f,
                        ),
                        SessionAnalysisCornerAnalysis(
                            segmentId = 1L,
                            lapNumber = 3,
                            cornerNumber = 4,
                            score = 58,
                            representativeSample = highlightSample(
                                lapNumber = 3,
                                sampleIndexInLap = 5,
                                trackPosition = 0.58f,
                                speedKmh = 169f,
                            ),
                            startTrackPosition = 0.48f,
                            apexTrackPosition = 0.58f,
                            endTrackPosition = 0.68f,
                            trailBrakingScore = 46,
                            timeLossMs = 158,
                            apexClassification = SessionAnalysisCornerApexClassification.GoodApex,
                            entrySpeedDeltaKmh = -6f,
                            exitSpeedDeltaKmh = -8f,
                        ),
                    ),
                ),
            ),
        )

        val categories = result.drafts.map { draft -> draft.category }.toSet()

        assertEquals(4, result.drafts.size)
        assertTrue(SessionAnalysisHighlightCategory.TopSpeed in categories)
        assertTrue(SessionAnalysisHighlightCategory.ThrottleCommitment in categories)
        assertTrue(SessionAnalysisHighlightCategory.BrakePoint in categories)
        assertTrue(SessionAnalysisHighlightCategory.TimeLoss in categories)
        assertTrue(
            result.drafts.first { draft -> draft.category == SessionAnalysisHighlightCategory.TimeLoss }.title.contains(
                "Largest repeatable loss"
            )
        )
    }

    @Test
    fun `execute skips time loss highlight when no repeatable corner loss exists`() = runBlocking {
        val result = stage.execute(
            highlightContext(
                samples = listOf(
                    highlightSample(
                        lapNumber = 1,
                        sampleIndexInLap = 1,
                        trackPosition = 0.33f,
                        speedKmh = 201f,
                    ),
                ),
                cornerReport = SessionAnalysisCornerAnalysisReport(
                    corners = listOf(
                        SessionAnalysisCornerAnalysis(
                            segmentId = 1L,
                            lapNumber = 2,
                            cornerNumber = 3,
                            score = 62,
                            representativeSample = highlightSample(
                                lapNumber = 2,
                                sampleIndexInLap = 1,
                                trackPosition = 0.33f,
                                speedKmh = 201f,
                            ),
                            startTrackPosition = 0.28f,
                            apexTrackPosition = 0.33f,
                            endTrackPosition = 0.41f,
                            trailBrakingScore = 49,
                            timeLossMs = 132,
                            apexClassification = SessionAnalysisCornerApexClassification.GoodApex,
                            entrySpeedDeltaKmh = -5f,
                            exitSpeedDeltaKmh = -6f,
                        ),
                    ),
                ),
            ),
        )

        val categories = result.drafts.map { draft -> draft.category }

        assertFalse(SessionAnalysisHighlightCategory.TimeLoss in categories)
    }
}
