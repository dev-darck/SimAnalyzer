@file:OptIn(ExperimentalTestApi::class)

package com.analyzer.setup.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class SetupScreenTest {

    @Test
    fun `setup screen composes with bounded recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                SetupScreen(
                    modifier = Modifier
                        .uiTestTag(TestTags.SetupScreen)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithTag(TestTags.SetupScreen.value).assertIsDisplayed()
        onNodeWithTag(TestTags.SetupScreen.value).assertRecompositionCountAtMost(1)
    }
}
