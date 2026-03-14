@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.live.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class LiveScreenTest {

    @Test
    fun `live screen updates telemetry with bounded recompositions`() = runDesktopComposeUiTest {
        val state = LiveScreenStateHolder(
            value = LiveScreenState(
                speedKmh = 123,
                rpmInt = 7200,
                rpmScale = 8f,
                gear = 4,
            ),
        )

        setContent {
            SimAnalyzerTheme {
                Screen(
                    state = state.value,
                    modifier = Modifier
                        .uiTestTag(TestTags.LiveScreen)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("123").assertIsDisplayed()
        onNodeWithTag(TestTags.LiveScreen.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.value = state.value.copy(
                speedKmh = 145,
                rpmInt = 7600,
                rpmScale = 9f,
                gear = 5,
            )
        }
        waitForIdle()

        onNodeWithText("145").assertIsDisplayed()
        onNodeWithText("5").assertIsDisplayed()
        onNodeWithTag(TestTags.LiveScreen.value).assertRecompositionCountAtMost(3)
    }
}

private class LiveScreenStateHolder(value: LiveScreenState) {

    var value by mutableStateOf(value)
}
