@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.trackmap.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.trackmap.presentation.model.TrackMapBuilderUiState
import com.analyzer.trackmap.presentation.model.TrackMapPreviewBoundsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewPointUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class TrackMapBuilderScreenTest {

    @Test
    fun `track map builder starts recording with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TrackMapBuilderStateHolder(value = sampleTrackMapBuilderState())

        setContent {
            SimAnalyzerTheme {
                TrackMapBuilderContent(
                    state = state.value,
                    onStart = { state.value = state.value.copy(recording = true) },
                    onStop = { state.value = state.value.copy(recording = false) },
                    onReferencePoint = { referencePoint ->
                        state.value = state.value.copy(referencePoint = referencePoint)
                    },
                    modifier = Modifier
                        .uiTestTag(TestTags.TrackMapBuilder)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("Assetto Corsa").assertIsDisplayed()
        onNodeWithTag(TestTags.TrackMapBuilder.value).assertRecompositionCountAtMost(1)

        onNodeWithTag(TestTags.TrackMapBuilderScroll.value)
            .performScrollToNode(hasTestTag(TestTags.TrackMapBuilderStart.value))
        onNodeWithTag(TestTags.TrackMapBuilderStart.value).performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(true, state.value.recording)
        }
        onNodeWithTag(TestTags.TrackMapBuilderStart.value).assertIsNotEnabled()
        onNodeWithText("Stop").assertIsEnabled()
        onNodeWithTag(TestTags.TrackMapBuilder.value).assertRecompositionCountAtMost(6)
    }
}

private class TrackMapBuilderStateHolder(value: TrackMapBuilderUiState) {

    var value by mutableStateOf(value)
}

private fun sampleTrackMapBuilderState(): TrackMapBuilderUiState = TrackMapBuilderUiState(
    recording = false,
    gameId = "ac",
    gameLabel = "Assetto Corsa",
    trackId = "monza",
    trackName = "Monza",
    referencePoint = ReferencePoint.FRONT_AXLE,
    pointCount = 60,
    totalDistanceMeters = 5793.4f,
    averageTrackWidthMeters = 11.4f,
    leftCoverageRatio = 0.82f,
    rightCoverageRatio = 0.79f,
    minSpacingMeters = 1.2f,
    maxSpacingMeters = 3.4f,
    minAngleDeg = 3f,
    minSpeedKmh = 60f,
    fallbackHalfWidthMeters = 5.5f,
    lapIndex = 2,
    lapsRecorded = 1,
    sectorCount = 3,
    capturedSectorCount = 1,
    preview = TrackMapPreviewUi(
        points = persistentListOf(
            TrackMapPreviewPointUi(0f, 0f),
            TrackMapPreviewPointUi(20f, 10f),
            TrackMapPreviewPointUi(40f, 15f),
        ),
        pointCount = 60,
        totalDistanceMeters = 5793.4f,
        averageTrackWidthMeters = 11.4f,
        bounds = TrackMapPreviewBoundsUi(0f, 0f, 40f, 15f),
    ),
)
