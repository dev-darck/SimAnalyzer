@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.trackmap.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.trackmap.presentation.model.TrackMapPreviewBoundsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewPointUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class TrackMapPreviewTest {

    @Test
    fun `track map preview transitions from empty to recording with bounded recompositions`() =
        runDesktopComposeUiTest {
            val state = TrackMapPreviewStateHolder(value = TrackMapPreviewUi())

            setContent {
                SimAnalyzerTheme {
                    TrackMapPreview(
                        state = state.value,
                        showStatus = true,
                        modifier = Modifier
                            .uiTestTag(TestTags.TrackMapPreview)
                            .trackRecompositions(),
                    )
                }
            }

            onNodeWithText("Start recording to build the map").assertIsDisplayed()
            onNodeWithTag(TestTags.TrackMapPreview.value).assertRecompositionCountAtMost(1)

            runOnIdle {
                state.value = TrackMapPreviewUi(
                    recording = true,
                    points = persistentListOf(
                        TrackMapPreviewPointUi(0f, 0f),
                        TrackMapPreviewPointUi(20f, 10f),
                        TrackMapPreviewPointUi(40f, 15f),
                    ),
                    pointCount = 3,
                    totalDistanceMeters = 120.5f,
                    averageTrackWidthMeters = 11.4f,
                    bounds = TrackMapPreviewBoundsUi(0f, 0f, 40f, 15f),
                )
            }
            waitForIdle()

            onNodeWithText("Recording").assertIsDisplayed()
            onNodeWithText("Points: 3").assertIsDisplayed()
            onNodeWithTag(TestTags.TrackMapPreview.value).assertRecompositionCountAtMost(3)
        }
}

private class TrackMapPreviewStateHolder(value: TrackMapPreviewUi) {

    var value by mutableStateOf(value)
}
