@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class TrackMapTest {

    @Test
    fun `track map renders and updates scale with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TrackMapState()

        setContent {
            SimAnalyzerTheme {
                Box {
                    TrackMap(
                        trackMap = demoTrackMap,
                        scale = state.scale,
                        modifier = Modifier
                            .width(180.dp)
                            .height(180.dp)
                            .uiTestTag(TestTags.TrackMap)
                            .trackRecompositions(),
                    )
                    Text("scale=${state.scale}")
                }
            }
        }

        onNodeWithTag(TestTags.TrackMap.value).assertIsDisplayed()
        onNodeWithTag(TestTags.TrackMap.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.scale = 1.4f
        }
        waitForIdle()

        onNodeWithText("scale=1.4").assertIsDisplayed()
        onNodeWithTag(TestTags.TrackMap.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `track map does not render node for null data`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                TrackMap(
                    trackMap = null,
                    modifier = Modifier.uiTestTag(TestTags.TrackMap),
                )
            }
        }

        onAllNodesWithTag(TestTags.TrackMap.value).assertCountEquals(0)
    }
}

private class TrackMapState {

    var scale by mutableFloatStateOf(1f)
}

private val demoTrackMap = TrackMapData(
    points = persistentListOf(
        TrackMapPoint(x = 0f, y = 0f),
        TrackMapPoint(x = 80f, y = 0f),
        TrackMapPoint(x = 80f, y = 40f),
        TrackMapPoint(x = 20f, y = 60f),
        TrackMapPoint(x = 0f, y = 20f),
    ),
    pitPoints = persistentListOf(
        TrackMapPoint(x = 10f, y = 10f),
        TrackMapPoint(x = 40f, y = 10f),
        TrackMapPoint(x = 40f, y = 20f),
    ),
    bounds = TrackMapBounds(
        minX = 0f,
        minY = 0f,
        maxX = 80f,
        maxY = 60f,
    ),
)
