package com.analyzer.session.analysis.presentation.components.inspector.support

import com.analyzer.session.analysis.presentation.model.toUi
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisDiagnosisSource
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlightCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionAnalysisDiagnosticClaritySupportTest {

    @Test
    fun `mixed source explains fix order`() {
        assertEquals(
            "Driver first. If it repeats, then car",
            SessionAnalysisDiagnosisSource.Mixed.toUi().fixInLabel(),
        )
    }

    @Test
    fun `driving look at labels stay phase specific`() {
        assertEquals(
            "Turn 5: first throttle pickup, steering unwind and exit speed",
            SessionAnalysisHighlightCategory.WheelSpin.toUi().toDrivingLookAtLabel(cornerNumber = 5),
        )
    }
}
