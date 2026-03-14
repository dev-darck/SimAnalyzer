@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.hud.api

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test
import java.util.Locale

class HudPanelSurfaceTest {

    @Test
    fun `hud panel surface follows composition local opacity with bounded recompositions`() = runDesktopComposeUiTest {
        val state = HudSurfaceState(alpha = 0.62f)

        setContent {
            CompositionLocalProvider(LocalHudBackgroundOpacity provides state.alpha) {
                Text(
                    text = "alpha=${formatAlpha(hudPanelSurfaceColor(Color.Red).alpha)}",
                    modifier = Modifier
                        .uiTestTag(TestTags.HudPanelSurface)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("alpha=0.62").assertIsDisplayed()
        onNodeWithTag(TestTags.HudPanelSurface.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.alpha = 0.18f
        }
        waitForIdle()

        onNodeWithText("alpha=0.18").assertIsDisplayed()
        onNodeWithTag(TestTags.HudPanelSurface.value).assertRecompositionCountAtMost(3)
    }
}

private class HudSurfaceState(alpha: Float) {

    var alpha by mutableFloatStateOf(alpha)
}

private fun formatAlpha(alpha: Float): String = String.format(Locale.US, "%.2f", alpha)
