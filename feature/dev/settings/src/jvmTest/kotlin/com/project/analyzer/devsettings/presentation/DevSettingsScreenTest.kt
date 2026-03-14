@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.devsettings.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertTrue

class DevSettingsScreenTest {

    @Test
    fun `dev hud settings toggles panel with bounded recompositions`() = runDesktopComposeUiTest {
        val state = DevHudStateHolder(
            value = DevHudState(
                hudEnabled = true,
                panels = persistentListOf(
                    DevHudPanelUi("calibration_debug", true),
                    DevHudPanelUi("track_map_builder", false),
                ),
            ),
        )

        setContent {
            SimAnalyzerTheme {
                DevHudSettingsScreen(
                    state = state.value,
                    onToggleHudPanel = { id, enabled ->
                        state.value = state.value.copy(
                            panels = state.value.panels.map { panel ->
                                if (panel.id == id) {
                                    panel.copy(enabled = enabled)
                                } else {
                                    panel
                                }
                            }.toPersistentList(),
                        )
                    },
                    modifier = Modifier
                        .uiTestTag(TestTags.DevHudSettings)
                        .trackRecompositions(),
                )
            }
        }

        onAllNodes(isToggleable()).assertCountEquals(2)
        onNodeWithTag(TestTags.DevHudSettings.value).assertRecompositionCountAtMost(1)

        onAllNodes(isToggleable())[1].performClick()
        waitForIdle()

        runOnIdle {
            assertTrue(state.value.panels[1].enabled)
        }
        onNodeWithTag(TestTags.DevHudSettings.value).assertRecompositionCountAtMost(4)
    }

    @Test
    fun `telemetry inspector updates entries with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TelemetryInspectorHolder(
            value = TelemetryInspectorState(
                status = TelemetryStatusUi.SimConnected,
                sessionType = "Practice",
                trackLabel = "Monza",
                carLabel = "BMW M4 GT3",
                frameId = 128L,
                lastUpdatedLabel = "12:00:00.000",
                entries = persistentListOf(TelemetryEntry("speedKmh", "120")),
            ),
        )

        setContent {
            SimAnalyzerTheme {
                TelemetryInspectorScreen(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.TelemetryInspector)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("speedKmh").assertIsDisplayed()
        onNodeWithTag(TestTags.TelemetryInspector.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.value = state.value.copy(
                entries = persistentListOf(
                    TelemetryEntry("speedKmh", "120"),
                    TelemetryEntry("gear", "4"),
                ),
                lastUpdatedLabel = "12:00:00.100",
            )
        }
        waitForIdle()

        onNodeWithText("gear").assertIsDisplayed()
        onNodeWithText("4").assertIsDisplayed()
        onNodeWithTag(TestTags.TelemetryInspector.value).assertRecompositionCountAtMost(3)
    }
}

private class DevHudStateHolder(value: DevHudState) {

    var value by mutableStateOf(value)
}

private class TelemetryInspectorHolder(value: TelemetryInspectorState) {

    var value by mutableStateOf(value)
}

private fun <T> List<T>.toPersistentList() = persistentListOf<T>().addAll(this)
