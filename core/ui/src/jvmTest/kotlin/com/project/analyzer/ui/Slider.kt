@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.slider.CustomSlider
import com.project.analyzer.ui.slider.SettingsIntSliderRow
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class Slider {

    @Test
    fun `settings int slider row updates visible value and commit count`() = runDesktopComposeUiTest {
        val state = IntSliderState(value = 2)

        setContent {
            SimAnalyzerTheme {
                Column(modifier = Modifier.width(240.dp)) {
                    SettingsIntSliderRow(
                        title = "Force feedback",
                        value = state.value,
                        range = 0..10,
                        step = 2,
                        valueSuffix = "%",
                        onPreviewChange = { state.value = it },
                        onCommit = { state.commitCount += 1 },
                    )
                    Text("commits=${state.commitCount}")
                }
            }
        }

        onNodeWithText("Force feedback").assertIsDisplayed()
        onNodeWithText("2 %").assertIsDisplayed()
        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(1)

        onNodeWithTag(TestTags.Slider.value).performTouchInput {
            swipeRight()
        }

        waitForIdle()

        onNodeWithText("10 %").assertIsDisplayed()
        onNodeWithText("commits=1").assertIsDisplayed()
        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(8)
    }

    @Test
    fun `custom slider clicks snap to nearest step and commit`() = runDesktopComposeUiTest {
        val state = FloatSliderState(value = 0f)

        setContent {
            SimAnalyzerTheme {
                Column {
                    CustomSlider(
                        value = state.value,
                        valueRange = 0f..20f,
                        snapStep = 5f,
                        onValueChange = { state.value = it },
                        onValueChangeFinished = { state.commitCount += 1 },
                        modifier = Modifier.width(240.dp),
                    )
                    Text("value=${state.value.toInt()}")
                    Text("commits=${state.commitCount}")
                }
            }
        }

        onNodeWithText("value=0").assertIsDisplayed()
        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(1)

        onNodeWithTag(TestTags.Slider.value).performTouchInput {
            click(center)
        }

        waitForIdle()

        onNodeWithText("value=10").assertIsDisplayed()
        onNodeWithText("commits=1").assertIsDisplayed()
        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(4)
    }

    @Test
    fun `disabled custom slider ignores touch input`() = runDesktopComposeUiTest {
        val state = FloatSliderState(value = 5f)

        setContent {
            SimAnalyzerTheme {
                Column {
                    CustomSlider(
                        value = state.value,
                        valueRange = 0f..20f,
                        snapStep = 5f,
                        enabled = false,
                        onValueChange = { state.value = it },
                        onValueChangeFinished = { state.commitCount += 1 },
                        modifier = Modifier.width(240.dp),
                    )
                    Text("value=${state.value.toInt()}")
                    Text("commits=${state.commitCount}")
                }
            }
        }

        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(1)
        onNodeWithTag(TestTags.Slider.value).performTouchInput {
            swipeRight()
        }

        waitForIdle()

        onNodeWithText("value=5").assertIsDisplayed()
        onNodeWithText("commits=0").assertIsDisplayed()
        onNodeWithTag(TestTags.Slider.value).assertRecompositionCountAtMost(1)
    }
}

private class IntSliderState(value: Int) {

    var value by mutableIntStateOf(value)
    var commitCount by mutableIntStateOf(0)
}

private class FloatSliderState(value: Float) {

    var value by mutableFloatStateOf(value)
    var commitCount by mutableIntStateOf(0)
}
