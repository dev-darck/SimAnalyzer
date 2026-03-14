@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.hudSettings.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Test
import kotlin.test.assertEquals

class HudSettingsScreenTest {

    @Test
    fun `hud settings selects panel with bounded recompositions`() = runDesktopComposeUiTest {
        val fuelPanel = testHudPanel(id = "fuel", description = "Fuel overlay")
        val timingPanel = testHudPanel(id = "timing", description = "Timing overlay")
        val state = HudSettingsStateHolder(
            value = HudUiState(
                visiblePanels = persistentMapOf("fuel" to 0, "timing" to 0),
                panels = persistentListOf(fuelPanel, timingPanel),
                panel = null,
            ),
        )

        setContent {
            SimAnalyzerTheme {
                Screen(
                    state = state.value,
                    dispatch = { intent ->
                        when (intent) {
                            is HudSettingsIntent.OnShowPanel -> {
                                state.value = state.value.copy(
                                    panel = state.value.panels.first { it.id == intent.id },
                                )
                            }

                            else -> Unit
                        }
                    },
                    modifier = Modifier
                        .uiTestTag(TestTags.HudSettings)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("fuel").assertIsDisplayed()
        onNodeWithTag(TestTags.HudSettings.value).assertRecompositionCountAtMost(1)

        onNodeWithText("timing").performClick()
        waitForIdle()

        runOnIdle {
            assertEquals("timing", state.value.panel?.id)
        }
        onNodeWithTag(TestTags.HudSettings.value).assertRecompositionCountAtMost(3)
    }
}

private class HudSettingsStateHolder(value: HudUiState) {

    var value by mutableStateOf(value)
}

private fun testHudPanel(id: String, description: String): HudPanel = object : HudPanel {
    override val id: String = id
    override val description: String = description

    @Composable
    override fun DemoContent(modifier: Modifier) {
        Text(text = id, modifier = modifier)
    }
}
