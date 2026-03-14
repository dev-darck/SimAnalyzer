@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.calibration.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.calibration.presentation.setup.CalibrationContent
import com.project.analyzer.calibration.presentation.setup.CalibrationIntent
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.calibration.presentation.verify.components.DirectionInfoCard
import com.project.analyzer.calibration.presentation.verify.components.GateDebugSection
import com.project.analyzer.calibration.presentation.verify.state.EditingGate
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals

class CalibrationUiTest {

    @Test
    fun `calibration screen renders header with bounded recompositions`() = runDesktopComposeUiTest {
        val state = CalibrationScreenState(
            value = CalibrationState(
                trackName = "Monza",
                trackId = "monza",
                startFinish = sampleGate(),
                listOfData = persistentListOf("monza-saved"),
                lastSavedTrackId = "monza-saved",
            ),
        )

        setContent {
            SimAnalyzerTheme {
                CalibrationContent(
                    state = state.value,
                    dispatchEvent = { event ->
                        if (event is CalibrationIntent.LoadAllCalibrations) {
                            state.value = state.value.copy(
                                listOfData = persistentListOf("monza-saved", "spa-saved"),
                            )
                        }
                    },
                    onVerify = {},
                    modifier = Modifier
                        .uiTestTag(TestTags.CalibrationScreen)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("Monza").assertIsDisplayed()
        onNodeWithTag(TestTags.CalibrationScreen.value).assertRecompositionCountAtMost(2)

        waitForIdle()
        onNodeWithTag(TestTags.CalibrationScreen.value).assertRecompositionCountAtMost(4)
    }

    @Test
    fun `direction card updates heading with bounded recompositions`() = runDesktopComposeUiTest {
        val state = HeadingState(headingDegrees = 0f)

        setContent {
            SimAnalyzerTheme {
                DirectionInfoCard(
                    forward = Vec2(1f, 0f),
                    headingDegrees = state.headingDegrees,
                    modifier = Modifier
                        .uiTestTag(TestTags.CalibrationDirection)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("↑").assertIsDisplayed()
        onNodeWithTag(TestTags.CalibrationDirection.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.headingDegrees = 90f
        }
        waitForIdle()

        onNodeWithText("→").assertIsDisplayed()
        onNodeWithTag(TestTags.CalibrationDirection.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun `gate debug section handles flip and edit actions with bounded recompositions`() = runDesktopComposeUiTest {
        val state = GateDebugState()

        setContent {
            SimAnalyzerTheme {
                GateDebugSection(
                    gates = persistentListOf(sampleGateDebugInfo()),
                    editingGate = state.editingGate,
                    halfWidthMeters = state.halfWidthMeters,
                    isCapturing = false,
                    modifier = Modifier
                        .uiTestTag(TestTags.CalibrationGateDebug)
                        .trackRecompositions(),
                    onEditGate = { state.editingGate = it },
                    onCaptureGate = {},
                    onCancelEdit = { state.editingGate = null },
                    onFlipGate = { state.flippedGate = it },
                    onRadius = { state.halfWidthMeters = it },
                )
            }
        }

        onNodeWithTag(TestTags.CalibrationGateDebug.value).assertRecompositionCountAtMost(1)

        onNodeWithText("Flip").performClick()
        waitForIdle()
        runOnIdle {
            assertEquals(EditingGate.START_FINISH, state.flippedGate)
        }

        onNodeWithText("Edit").performClick()
        waitForIdle()
        runOnIdle {
            assertEquals(EditingGate.START_FINISH, state.editingGate)
        }

        onNodeWithText("Cancel").performClick()
        waitForIdle()
        runOnIdle {
            assertEquals<EditingGate?>(null, state.editingGate)
        }
        onNodeWithTag(TestTags.CalibrationGateDebug.value).assertRecompositionCountAtMost(5)
    }
}

private class CalibrationScreenState(value: CalibrationState) {

    var value by mutableStateOf(value)
}

private class HeadingState(headingDegrees: Float) {

    var headingDegrees by mutableFloatStateOf(headingDegrees)
}

private class GateDebugState {

    var editingGate by mutableStateOf<EditingGate?>(null)
    var flippedGate by mutableStateOf<EditingGate?>(null)
    var halfWidthMeters by mutableFloatStateOf(6f)
}

private fun sampleGate(): Gate = Gate.create(
    center = Vec2(0f, 0f),
    forward = Vec2(0f, 1f),
    normal = Vec2(1f, 0f),
    halfWidthMeters = 6f,
)

private fun sampleGateDebugInfo(): GateDebugInfo = GateDebugInfo(
    name = "Start / Finish",
    distanceMeters = 3.2f,
    isCrossed = false,
    gateForward = Vec2(0f, 1f),
    directionDot = -0.72f,
    signedDistanceFromPlane = -2.1f,
    isInside = true,
    margin = 0.4f,
    dParallel = 0.8f,
    gate = sampleGate(),
)
