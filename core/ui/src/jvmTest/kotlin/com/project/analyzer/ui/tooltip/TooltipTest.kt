@file:OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)

package com.project.analyzer.ui.tooltip

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class TooltipTest {

    @Test
    fun `tooltip reveals on hover and keeps anchor recompositions bounded`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                Tooltip(tooltip = "Best lap delta") {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .uiTestTag(TestTags.Tooltip)
                            .trackRecompositions(),
                    ) {
                        Text("Delta")
                    }
                }
            }
        }

        mainClock.autoAdvance = false

        onNodeWithTag(TestTags.Tooltip.value).assertRecompositionCountAtMost(1)
        onAllNodesWithText("Best lap delta").assertCountEquals(0)

        onNodeWithTag(TestTags.Tooltip.value).performMouseInput {
            enter(center)
        }
        mainClock.advanceTimeBy(360)
        waitForIdle()

        onNodeWithText("Best lap delta").assertIsDisplayed()
        onNodeWithTag(TestTags.Tooltip.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `tooltip disabled renders content without popup`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                Tooltip(
                    tooltip = "Should stay hidden",
                    isShowTooltip = false,
                ) {
                    Text(
                        text = "Fuel",
                        modifier = Modifier.uiTestTag(TestTags.Tooltip),
                    )
                }
            }
        }

        onNodeWithTag(TestTags.Tooltip.value).assertIsDisplayed()
        onAllNodesWithText("Should stay hidden").assertCountEquals(0)
    }
}
