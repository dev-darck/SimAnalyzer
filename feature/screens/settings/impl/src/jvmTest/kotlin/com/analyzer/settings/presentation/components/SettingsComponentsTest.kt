@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.settings.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.analyzer.settings.api.AppCloseBehavior
import com.analyzer.settings.presentation.GameSelectionUi
import com.analyzer.settings.presentation.RecordingWarningKind
import com.analyzer.settings.presentation.StorageSizeInfo
import com.analyzer.settings.presentation.StorageSizeUnit
import com.analyzer.settings.presentation.buildGameSelectionUi
import com.project.analyzer.game.api.GameId
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.theme.ThemeMode
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test
import kotlin.test.assertEquals

class SettingsComponentsTest {

    @Test
    fun `appearance block changes theme with bounded recompositions`() = runDesktopComposeUiTest {
        val state = ThemeSelectionState(selectedTheme = ThemeMode.System)

        setContent {
            SimAnalyzerTheme {
                AppearanceBlock(
                    selectedTheme = state.selectedTheme,
                    onThemeSelected = { state.selectedTheme = it },
                    modifier = Modifier
                        .uiTestTag(TestTags.AppearanceSettings)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.AppearanceSettings.value).assertRecompositionCountAtMost(1)

        onNodeWithContentDescription("Dark").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(ThemeMode.Dark, state.selectedTheme)
        }
        onNodeWithTag(TestTags.AppearanceSettings.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `close behavior block changes selection with bounded recompositions`() = runDesktopComposeUiTest {
        val state = CloseBehaviorSelectionState(selectedBehavior = AppCloseBehavior.AskEveryTime)

        setContent {
            SimAnalyzerTheme {
                CloseBehaviorBlock(
                    selectedBehavior = state.selectedBehavior,
                    onBehaviorSelected = { state.selectedBehavior = it },
                    modifier = Modifier
                        .uiTestTag(TestTags.CloseBehaviorSettings)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.CloseBehaviorSettings.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Ask every time").performClick()
        waitForIdle()

        onNodeWithText("Minimize to tray").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(AppCloseBehavior.MinimizeToTray, state.selectedBehavior)
        }
        onNodeWithTag(TestTags.CloseBehaviorSettings.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `hud setup toggles state with bounded recompositions`() = runDesktopComposeUiTest {
        val state = HudSetupState(isEnabled = true)

        setContent {
            SimAnalyzerTheme {
                HudSetupBlock(
                    isHudEnabled = state.isEnabled,
                    onHudEnabledChange = { state.isEnabled = it },
                    modifier = Modifier
                        .uiTestTag(TestTags.HudSetupSettings)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.HudSetupSettings.value).assertRecompositionCountAtMost(1)

        onAllNodes(isToggleable()).assertCountEquals(1)
        onAllNodes(isToggleable())[0].performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(false, state.isEnabled)
        }
        onNodeWithTag(TestTags.HudSetupSettings.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `telemetry acquisition toggles recording with bounded recompositions`() = runDesktopComposeUiTest {
        val state = TelemetryAcquisitionState(recordingEnabled = true)

        setContent {
            SimAnalyzerTheme {
                TelemetryAcquisitionBlock(
                    samplingRateHz = 70,
                    storageLocation = "C:\\Telemetry",
                    storageLocationError = null,
                    storageSizeInfo = StorageSizeInfo.Value(size = 1.5, fractionDigits = 1, unit = StorageSizeUnit.GB),
                    recordingEnabled = state.recordingEnabled,
                    recordingWarning = RecordingWarningKind.HighRate,
                    maxRecordedLaps = 20,
                    onRecordingEnabledChange = { state.recordingEnabled = it },
                    modifier = Modifier
                        .uiTestTag(TestTags.TelemetryAcquisition)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("Browse").assertIsDisplayed()
        onNodeWithTag(TestTags.TelemetryAcquisition.value).assertRecompositionCountAtMost(1)

        onAllNodes(isToggleable())[0].performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(false, state.recordingEnabled)
        }
        onAllNodesWithText("Browse").assertCountEquals(0)
        onNodeWithTag(TestTags.TelemetryAcquisition.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `telemetry game selection changes game with bounded recompositions`() = runDesktopComposeUiTest {
        val state = GameSelectionState(
            selectionUi = buildGameSelectionUi(GameSelection.Auto),
        )

        setContent {
            SimAnalyzerTheme {
                TelemetryGameSelectionBlock(
                    selectionUi = state.selectionUi,
                    onSelectionChange = { selection ->
                        state.selectionUi = buildGameSelectionUi(selection)
                    },
                    modifier = Modifier
                        .uiTestTag(TestTags.TelemetryGameSelection)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.TelemetryGameSelection.value).assertRecompositionCountAtMost(1)

        onNodeWithContentDescription("Select game").performClick()
        onNodeWithText("Assetto Corsa Competizione").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(GameSelection.Manual(GameId.ACC), state.selectionUi.selection)
        }
        onNodeWithTag(TestTags.TelemetryGameSelection.value).assertRecompositionCountAtMost(5)
    }

    @Test
    fun `dev settings block forwards open action`() = runDesktopComposeUiTest {
        val state = ClickCounter()

        setContent {
            SimAnalyzerTheme {
                DevSettingsBlock(
                    onOpen = { state.count += 1 },
                    modifier = Modifier
                        .uiTestTag(TestTags.DevSettingsEntry)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("Open").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals(1, state.count)
        }
        onNodeWithTag(TestTags.DevSettingsEntry.value).assertRecompositionCountAtMost(2)
    }
}

private class ThemeSelectionState(selectedTheme: ThemeMode) {

    var selectedTheme by mutableStateOf(selectedTheme)
}

private class CloseBehaviorSelectionState(selectedBehavior: AppCloseBehavior) {

    var selectedBehavior by mutableStateOf(selectedBehavior)
}

private class HudSetupState(isEnabled: Boolean) {

    var isEnabled by mutableStateOf(isEnabled)
}

private class TelemetryAcquisitionState(recordingEnabled: Boolean) {

    var recordingEnabled by mutableStateOf(recordingEnabled)
}

private class GameSelectionState(selectionUi: GameSelectionUi) {

    var selectionUi by mutableStateOf(selectionUi)
}

private class ClickCounter {

    var count by mutableIntStateOf(0)
}
