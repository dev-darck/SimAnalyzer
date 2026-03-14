@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.session.details.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailFilterKind
import com.analyzer.session.details.presentation.model.SessionDetailFilterOptionUi
import com.analyzer.session.details.presentation.model.SessionDetailFilterUiModel
import com.analyzer.session.details.presentation.model.SessionDetailHeaderUi
import com.analyzer.session.details.presentation.model.SessionDetailIntent
import com.analyzer.session.details.presentation.model.SessionDetailState
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.analyzer.session.details.presentation.model.SessionLapRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class SessionDetailsScreenTest {

    @Test
    fun `session details changes page with bounded recompositions`() = runDesktopComposeUiTest {
        val state = SessionDetailsStateHolder(value = sampleSessionDetailState())

        setContent {
            SimAnalyzerTheme {
                SessionDetailsContent(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.SessionDetails)
                        .trackRecompositions(),
                    onIntent = { intent ->
                        if (intent is SessionDetailIntent.ChangePage) {
                            state.value = state.value.copy(page = intent.page)
                        }
                    },
                )
            }
        }

        onNodeWithText("BMW M4 GT3").assertIsDisplayed()
        onNodeWithTag(TestTags.SessionDetails.value).assertRecompositionCountAtMost(1)

        onNodeWithText("4").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(4, state.value.page)
        }
        onNodeWithTag(TestTags.SessionDetails.value).assertRecompositionCountAtMost(4)
    }
}

private class SessionDetailsStateHolder(value: SessionDetailState) {

    var value by mutableStateOf(value)
}

private fun sampleSessionDetailState(): SessionDetailState = SessionDetailState(
    isLoading = false,
    header = SessionDetailHeaderUi(
        subtitle = "Mar 14, 2026",
        sessionTypeLabel = "Race",
        airTempLabel = "19°C",
        trackTempLabel = "24°C",
        carLabel = "BMW M4 GT3",
        trackLabel = "Monza",
    ),
    stats = SessionDetailStatsUi(
        bestLapLabel = "1:45.678",
        averageLapLabel = "1:47.222",
        incidentsCount = 1,
    ),
    sortFilter = detailFilter(SessionDetailFilterKind.Sort, "lap", "lap", "best"),
    showFilter = detailFilter(SessionDetailFilterKind.Show, "all", "all", "valid"),
    sessionTypeFilter = detailFilter(
        SessionDetailFilterKind.SessionType,
        "all_session_types",
        "all_session_types",
        "race"
    ),
    page = 1,
    pageCount = 4,
    visibleLaps = persistentListOf(
        SessionLapRowUi(
            lapNumber = 1,
            lapLabel = "1",
            sessionTypeLabel = "Race",
            totalTimeMs = 105678,
            totalTime = "1:45.678",
            s1 = "35.100",
            s2 = "34.800",
            s3 = "35.778",
            incidents = "0",
            delta = "-0.000",
            deltaIsPositive = false,
            status = LapStatus.BestLap,
        ),
    ),
)

private fun detailFilter(
    kind: SessionDetailFilterKind,
    selectedId: String,
    firstId: String,
    secondId: String,
): SessionDetailFilterUiModel = SessionDetailFilterUiModel(
    kind = kind,
    selectedId = selectedId,
    options = persistentListOf(
        SessionDetailFilterOptionUi(id = firstId),
        SessionDetailFilterOptionUi(id = secondId),
    ),
)
