@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.trackmap.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationInspectorUiState
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationMarkerPanelUiState
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationSidebarUiState
import com.analyzer.trackmap.presentation.model.TrackMapMarkerRowUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class TrackMapCalibrationEditorSidebarTest {

    @Test
    fun `track map editor sidebar handles save and add marker with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TrackMapSidebarStateHolder(value = sampleSidebarUiState())
        val saveClicks = ClickCounter()

        setContent {
            SimAnalyzerTheme {
                TrackMapCalibrationEditorSidebar(
                    uiState = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.TrackMapEditorSidebar)
                        .trackRecompositions(),
                    scrollable = false,
                    onAddPointModeChange = { enabled ->
                        state.value = state.value.copy(
                            markerPanel = state.value.markerPanel.copy(isAddPointMode = enabled),
                        )
                    },
                    onDeletePoint = {},
                    onSave = { saveClicks.count += 1 },
                    onSelectMarker = {},
                )
            }
        }

        onAllNodesWithText("Start / Finish").assertCountEquals(2)
        onNodeWithTag(TestTags.TrackMapEditorSidebar.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Save").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(1, saveClicks.count)
        }

        onNodeWithText("New marker").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(true, state.value.markerPanel.isAddPointMode)
        }
        onNodeWithTag(TestTags.TrackMapEditorSidebar.value).assertRecompositionCountAtMost(4)
    }
}

private class TrackMapSidebarStateHolder(value: TrackMapCalibrationSidebarUiState) {

    var value by mutableStateOf(value)
}

private class ClickCounter {

    var count by mutableIntStateOf(0)
}

private fun sampleSidebarUiState(): TrackMapCalibrationSidebarUiState {
    val selectedRow = TrackMapMarkerRowUi(
        gateId = "sf",
        title = "Start / Finish",
        startLabel = "0.0 m",
        endLabel = "0.0 m",
        colorHex = 0xFFFF9800,
    )

    return TrackMapCalibrationSidebarUiState(
        markerPanel = TrackMapCalibrationMarkerPanelUiState(
            markerRows = persistentListOf(selectedRow),
            selectedMarkerId = "sf",
            isAddPointMode = false,
        ),
        inspector = TrackMapCalibrationInspectorUiState(
            selectedRow = selectedRow,
            message = "Unsaved changes",
            saveLabel = "Save",
            canSave = true,
            deleteGateId = null,
        ),
    )
}
