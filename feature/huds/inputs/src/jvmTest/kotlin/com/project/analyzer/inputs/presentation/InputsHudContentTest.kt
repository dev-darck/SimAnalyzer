package com.project.analyzer.inputs.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.theme.SimAnalyzerTheme
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@Ignore("Requires Skiko native binaries for Compose Desktop UI tests.")
class InputsHudContentTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsLegendWhenEnabled() {
        val state = InputsHudUiState(
            isShow = true,
            settings = InputHudSettings(showLegend = true, showHeader = false)
        )

        rule.setContent {
            SimAnalyzerTheme {
                InputsHudContent(state = state)
            }
        }

        rule.onNodeWithText("T").assertIsDisplayed()
        rule.onNodeWithText("B").assertIsDisplayed()
        rule.onNodeWithText("C").assertIsDisplayed()
        rule.onNodeWithText("S").assertIsDisplayed()
    }

    @Test
    fun hidesLegendWhenDisabled() {
        val state = InputsHudUiState(
            isShow = true,
            settings = InputHudSettings(showLegend = false, showHeader = false)
        )

        rule.setContent {
            SimAnalyzerTheme {
                InputsHudContent(state = state)
            }
        }

        rule.onAllNodesWithText("T").assertCountEquals(0)
        rule.onAllNodesWithText("B").assertCountEquals(0)
        rule.onAllNodesWithText("C").assertCountEquals(0)
        rule.onAllNodesWithText("S").assertCountEquals(0)
    }
}
