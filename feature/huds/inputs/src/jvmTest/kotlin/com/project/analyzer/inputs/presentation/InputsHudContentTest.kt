@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.inputs.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class InputsHudContentTest {

    @Test
    fun `shows legend when enabled with bounded recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                InputsHudContent(
                    state = InputsHudUiState(
                        isShow = true,
                        settings = InputHudSettings(showLegend = true, showHeader = false),
                    ),
                    modifier = Modifier
                        .uiTestTag(TestTags.InputsHud)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("T").assertIsDisplayed()
        onNodeWithText("B").assertIsDisplayed()
        onNodeWithText("C").assertIsDisplayed()
        onNodeWithText("S").assertIsDisplayed()
        onNodeWithTag(TestTags.InputsHud.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `hides legend after state update with bounded recompositions`() = runDesktopComposeUiTest {
        val state = InputsHudStateHolder(
            value = InputsHudUiState(
                isShow = true,
                settings = InputHudSettings(showLegend = true, showHeader = false),
            ),
        )

        setContent {
            SimAnalyzerTheme {
                InputsHudContent(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.InputsHud)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.InputsHud.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.value = state.value.copy(
                settings = state.value.settings.copy(showLegend = false),
            )
        }
        waitForIdle()

        onAllNodesWithText("T").assertCountEquals(0)
        onAllNodesWithText("B").assertCountEquals(0)
        onAllNodesWithText("C").assertCountEquals(0)
        onAllNodesWithText("S").assertCountEquals(0)
        onNodeWithTag(TestTags.InputsHud.value).assertRecompositionCountAtMost(3)
    }
}

private class InputsHudStateHolder(value: InputsHudUiState) {

    var value by mutableStateOf(value)
}
