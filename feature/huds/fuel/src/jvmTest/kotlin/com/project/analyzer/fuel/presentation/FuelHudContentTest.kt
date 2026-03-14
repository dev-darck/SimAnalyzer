@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.fuel.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.fuel.domain.model.FuelPhase
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class FuelHudContentTest {

    @Test
    fun `fuel hud updates content with bounded recompositions`() = runDesktopComposeUiTest {
        val state = FuelHudStateHolder(
            value = FuelHudUiState(
                isShow = true,
                isSessionActive = true,
                phase = FuelPhase.PREDICTIVE,
                mainValue = "1.45 L",
                peakValue = "1.52 L",
                fuelLeftText = "20.34 L",
                lapsRemainingCount = 14,
                lapBasisText = "1:28.500",
                planRows = persistentListOf(PlanRowUi(5, "7:22", "7 L", "8 L")),
            ),
        )

        setContent {
            SimAnalyzerTheme {
                FuelHudContent(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.FuelHud)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("1.45 L").assertIsDisplayed()
        onNodeWithTag(TestTags.FuelHud.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.value = state.value.copy(
                phase = FuelPhase.PIT_WAITING,
                mainValue = "1.60 L",
            )
        }
        waitForIdle()

        onNodeWithText("1.60 L").assertIsDisplayed()
        onNodeWithText("Waiting in pits\u2026").assertIsDisplayed()
        onNodeWithTag(TestTags.FuelHud.value).assertRecompositionCountAtMost(3)
    }
}

private class FuelHudStateHolder(value: FuelHudUiState) {

    var value by mutableStateOf(value)
}
