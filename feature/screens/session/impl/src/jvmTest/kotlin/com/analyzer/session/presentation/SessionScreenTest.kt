@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.session.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.session.presentation.components.previewTrackMapData
import com.analyzer.session.presentation.model.FILTER_ALL_ID
import com.analyzer.session.presentation.model.SessionFilterKind
import com.analyzer.session.presentation.model.SessionFilterOptionUi
import com.analyzer.session.presentation.model.SessionFilterUiModel
import com.analyzer.session.presentation.model.SessionListIntent
import com.analyzer.session.presentation.model.SessionListState
import com.analyzer.session.presentation.model.SessionRowUi
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class SessionScreenTest {

    @Test
    fun `session list changes page with bounded recompositions`() = runDesktopComposeUiTest {
        val state = SessionScreenStateHolder(value = sampleSessionListState())

        setContent {
            SimAnalyzerTheme {
                SessionListContent(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.SessionList)
                        .trackRecompositions(),
                    onIntent = { intent ->
                        if (intent is SessionListIntent.ChangePage) {
                            state.value = state.value.copy(page = intent.page)
                        }
                    },
                )
            }
        }

        onNodeWithText("Monza").assertIsDisplayed()
        onNodeWithTag(TestTags.SessionList.value).assertRecompositionCountAtMost(1)

        onNode(hasText("4") and hasClickAction()).performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(4, state.value.page)
        }
        onNodeWithTag(TestTags.SessionList.value).assertRecompositionCountAtMost(4)
    }
}

private class SessionScreenStateHolder(value: SessionListState) {

    var value by mutableStateOf(value)
}

private fun sampleSessionListState(): SessionListState = SessionListState(
    isLoading = false,
    stats = SessionStatsUi(
        totalDistanceLabel = "123.456",
        sessionsCount = 4,
        incidentsCount = 1,
        favoriteCar = "BMW M4 GT3",
    ),
    gameFilter = sessionFilter(SessionFilterKind.Game, FILTER_ALL_ID, "ACC"),
    trackFilter = sessionFilter(SessionFilterKind.Track, FILTER_ALL_ID, "Monza"),
    carFilter = sessionFilter(SessionFilterKind.Car, FILTER_ALL_ID, "BMW M4 GT3"),
    dateFilter = sessionFilter(SessionFilterKind.Date, FILTER_ALL_ID, "Mar 2026"),
    sortFilter = SessionFilterUiModel(
        kind = SessionFilterKind.Sort,
        selectedId = "newest",
        options = persistentListOf(
            SessionFilterOptionUi("newest"),
            SessionFilterOptionUi("oldest"),
        ),
    ),
    page = 1,
    pageCount = 4,
    visibleSessions = persistentListOf(
        SessionRowUi(
            sessionId = 1L,
            dateLabel = "Mar 14, 2026",
            timeLabel = "12:00",
            gameId = "acc",
            trackId = "monza",
            gameLabel = "ACC",
            sessionTypeLabel = "Race",
            trackLabel = "Monza",
            carLabel = "BMW M4 GT3",
            lapsLabel = "12",
            bestLapLabel = "1:45.678",
            isSaved = true,
            trackMap = previewTrackMapData(),
        ),
    ),
)

private fun sessionFilter(kind: SessionFilterKind, selectedId: String, label: String): SessionFilterUiModel =
    SessionFilterUiModel(
        kind = kind,
        selectedId = selectedId,
        options = persistentListOf(
            SessionFilterOptionUi(FILTER_ALL_ID),
            SessionFilterOptionUi(label.lowercase(), label),
        ),
    )
