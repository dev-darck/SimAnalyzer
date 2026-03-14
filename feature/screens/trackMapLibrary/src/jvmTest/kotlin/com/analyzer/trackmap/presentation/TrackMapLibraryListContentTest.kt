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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryHeaderUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryPointsPreviewUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryStatsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewBoundsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewPointUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class TrackMapLibraryListContentTest {

    @Test
    fun `track map library loads items and opens editor with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TrackMapLibraryStateHolder(value = persistentListOf())
        var openedMapKey: String? = null

        setContent {
            SimAnalyzerTheme {
                TrackMapLibraryListContent(
                    items = state.value,
                    onOpenEditor = { openedMapKey = it.mapKey },
                    modifier = Modifier
                        .uiTestTag(TestTags.TrackMapLibrary)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.TrackMapLibrary.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.value = persistentListOf(sampleTrackMapLibraryCard())
        }
        waitForIdle()

        onNodeWithText("Monza GP").assertIsDisplayed()
        onNodeWithTag(TestTags.TrackMapLibrary.value).assertRecompositionCountAtMost(3)

        onNodeWithText("Monza GP").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals("ac:monza:gp", openedMapKey)
        }
    }
}

private class TrackMapLibraryStateHolder(value: ImmutableList<TrackMapLibraryCardUi>) {

    var value by mutableStateOf(value)
}

private fun sampleTrackMapLibraryCard(): TrackMapLibraryCardUi = TrackMapLibraryCardUi(
    mapKey = "ac:monza:gp",
    gameId = "ac",
    trackId = "monza",
    layoutId = "gp",
    header = TrackMapLibraryHeaderUi(
        title = "Monza GP",
        subtitle = "Assetto Corsa",
        layoutLabel = "GP",
    ),
    stats = TrackMapLibraryStatsUi(
        pointCount = 320,
        distanceMeters = 5793.4f,
        averageTrackWidthMeters = 11.4f,
        pitPointCount = 24,
        createdAtLabel = "2026-03-14",
    ),
    preview = TrackMapPreviewUi(
        points = persistentListOf(
            TrackMapPreviewPointUi(0f, 0f),
            TrackMapPreviewPointUi(20f, 10f),
            TrackMapPreviewPointUi(40f, 15f),
        ),
        pointCount = 320,
        totalDistanceMeters = 5793.4f,
        averageTrackWidthMeters = 11.4f,
        bounds = TrackMapPreviewBoundsUi(0f, 0f, 40f, 15f),
    ),
    pointsPreview = TrackMapLibraryPointsPreviewUi(
        points = persistentListOf(
            TrackMapPreviewPointUi(0f, 0f),
            TrackMapPreviewPointUi(20f, 10f),
        ),
        hiddenCount = 10,
    ),
)
