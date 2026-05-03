@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.session.details.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.session.details.presentation.model.LapStatus
import com.analyzer.session.details.presentation.model.SessionDetailCompareLapUi
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionCandidateUi
import com.analyzer.session.details.presentation.model.SessionDetailCompareSessionPickerUi
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
import kotlinx.collections.immutable.toImmutableList
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

    @Test
    fun `session details enters compare selection mode from fab`() = runDesktopComposeUiTest {
        val state = SessionDetailsStateHolder(value = sampleSessionDetailState())

        setContent {
            SimAnalyzerTheme {
                SessionDetailsContent(
                    state = state.value,
                    modifier = Modifier.uiTestTag(TestTags.SessionDetails),
                    onIntent = { intent ->
                        when (intent) {
                            SessionDetailIntent.StartCompareSelection -> {
                                state.value = state.value.enableCompareSelectionMode()
                            }

                            SessionDetailIntent.CancelCompareSelection -> {
                                state.value = sampleSessionDetailState()
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }

        onNodeWithContentDescription("More session actions").performClick()
        waitForIdle()
        onNodeWithText("Compare Laps").performClick()
        waitForIdle()

        onNodeWithText("Select 2 Laps", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithContentDescription("Cancel lap compare", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithContentDescription("Open lap comparison", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `session details opens compare session picker from fab`() = runDesktopComposeUiTest {
        val state = SessionDetailsStateHolder(value = sampleSessionDetailState())

        setContent {
            SimAnalyzerTheme {
                SessionDetailsContent(
                    state = state.value,
                    modifier = Modifier.uiTestTag(TestTags.SessionDetails),
                    onIntent = { intent ->
                        when (intent) {
                            SessionDetailIntent.OpenCompareSessionPicker -> {
                                state.value = state.value.copy(
                                    compareSessionPicker = SessionDetailCompareSessionPickerUi(
                                        isVisible = true,
                                        title = "Compare on Monza GP",
                                        supportingText = "Only ACC sessions from the same track layout are suggested.",
                                        candidates = persistentListOf(
                                            SessionDetailCompareSessionCandidateUi(
                                                sessionId = 77L,
                                                carLabel = "BMW M4 GT3",
                                                sessionTypeLabel = "Race",
                                                dateLabel = "Mar 20, 2026",
                                                timeLabel = "20:40",
                                                bestLapLabel = "1:38.100",
                                                lapsLabel = "12",
                                                recommendationLabel = "Same car",
                                            ),
                                        ),
                                    ),
                                )
                            }

                            SessionDetailIntent.DismissCompareSessionPicker -> {
                                state.value = state.value.copy(
                                    compareSessionPicker = SessionDetailCompareSessionPickerUi(),
                                )
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }

        onNodeWithContentDescription("More session actions").performClick()
        waitForIdle()
        onNodeWithText("Add Compare Session").performClick()
        waitForIdle()

        onNodeWithText("Compare on Monza GP").assertIsDisplayed()
        onNodeWithText("Same car").assertIsDisplayed()
        onNodeWithText("Best 1:38.100 • 12 laps").assertIsDisplayed()
    }

    @Test
    fun `session details opens share dialog from fab`() = runDesktopComposeUiTest {
        val state = SessionDetailsStateHolder(value = sampleSessionDetailState())

        setContent {
            SimAnalyzerTheme {
                SessionDetailsContent(
                    state = state.value,
                    modifier = Modifier.uiTestTag(TestTags.SessionDetails),
                    onIntent = { intent ->
                        when (intent) {
                            SessionDetailIntent.OpenShareResults -> {
                                state.value = state.value.copy(
                                    shareDialog = state.value.shareDialog.copy(
                                        isVisible = true,
                                        title = "Share Monza GP session",
                                        supportingText = "Copy the summary or export a Markdown report.",
                                        summaryText = "Sim Analyzer Session Summary\n\nTrack: Monza GP",
                                        reportFileName = "simanalyzer_monza_gp_race.md",
                                    ),
                                )
                            }

                            SessionDetailIntent.DismissShareResults -> {
                                state.value = state.value.copy(shareDialog = state.value.shareDialog.copy(isVisible = false))
                            }

                            else -> Unit
                        }
                    },
                )
            }
        }

        onNodeWithContentDescription("More session actions").performClick()
        waitForIdle()
        onNodeWithText("Share Results").performClick()
        waitForIdle()

        onNodeWithText("Share Monza GP session").assertIsDisplayed()
        onNodeWithText("Copy Chat Summary").assertIsDisplayed()
        onNodeWithText("Save Markdown Report").assertIsDisplayed()
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
            segmentId = 1L,
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
        SessionLapRowUi(
            segmentId = 1L,
            lapNumber = 2,
            lapLabel = "2",
            sessionTypeLabel = "Race",
            totalTimeMs = 106890,
            totalTime = "1:46.890",
            s1 = "35.800",
            s2 = "35.200",
            s3 = "35.890",
            incidents = "0",
            delta = "+1.212",
            deltaIsPositive = true,
            status = LapStatus.Clean,
        ),
    ),
)

private fun SessionDetailState.enableCompareSelectionMode(): SessionDetailState = copy(
    isCompareSelectionMode = true,
    selectedCompareLaps = persistentListOf<SessionDetailCompareLapUi>(),
    compareConfirmEnabled = false,
    visibleLaps = visibleLaps.map { lap ->
        lap.copy(compareAvailable = true)
    }.toImmutableList(),
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
