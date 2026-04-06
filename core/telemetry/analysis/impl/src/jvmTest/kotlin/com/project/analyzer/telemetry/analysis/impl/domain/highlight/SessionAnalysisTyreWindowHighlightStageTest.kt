package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightSeverity
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionAnalysisTyreWindowHighlightStageTest {

    private val stage = SessionAnalysisTyreWindowHighlightStage()

    @Test
    fun `execute emits tyre overheat and cold-window drafts`() = runBlocking {
        val result = stage.execute(
            highlightContext(
                samples = listOf(
                    highlightSample(
                        lapNumber = 2,
                        sampleIndexInLap = 1,
                        trackPosition = 0.21f,
                        deltaToBestMs = 92,
                        tyreBand = SessionAnalysisTyreTemperatureBand.Hot,
                        coreTempC = 101f,
                    ),
                    highlightSample(
                        lapNumber = 3,
                        sampleIndexInLap = 2,
                        trackPosition = 0.48f,
                        deltaToBestMs = 131,
                        tyreBand = SessionAnalysisTyreTemperatureBand.Critical,
                        coreTempC = 108f,
                    ),
                    highlightSample(
                        lapNumber = 4,
                        sampleIndexInLap = 3,
                        trackPosition = 0.66f,
                        deltaToBestMs = 117,
                        tyreBand = SessionAnalysisTyreTemperatureBand.Cold,
                        coreTempC = 68f,
                    ),
                ),
            ),
        )

        val overheatDraft = result.drafts.first { draft ->
            draft.category == SessionAnalysisHighlightCategory.TyreOverheat
        }
        val coldDraft = result.drafts.first { draft ->
            draft.category == SessionAnalysisHighlightCategory.TyreCold
        }

        assertEquals(SessionAnalysisHighlightSeverity.Critical, overheatDraft.severity)
        assertEquals(listOf(2, 3), overheatDraft.affectedLaps)
        assertEquals(listOf(4), coldDraft.affectedLaps)
    }

    @Test
    fun `execute ignores cold tyres on opening lap`() = runBlocking {
        val result = stage.execute(
            highlightContext(
                samples = listOf(
                    highlightSample(
                        lapNumber = 1,
                        sampleIndexInLap = 1,
                        trackPosition = 0.12f,
                        deltaToBestMs = 155,
                        tyreBand = SessionAnalysisTyreTemperatureBand.Cold,
                        coreTempC = 64f,
                    ),
                ),
            ),
        )

        val categories = result.drafts.map { draft -> draft.category }

        assertFalse(SessionAnalysisHighlightCategory.TyreCold in categories)
        assertTrue(result.drafts.isEmpty())
    }
}
