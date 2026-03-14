@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.theme

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
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

class SimAnalyzerThemeTest {

    @Test
    fun `theme composes dark mode content with bounded recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme(themeMode = ThemeMode.Dark) {
                Text(
                    text = if (LocalDarkTheme.current) "dark" else "light",
                    modifier = Modifier
                        .uiTestTag(TestTags.Theme)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("dark").assertIsDisplayed()
        onNodeWithTag(TestTags.Theme.value).assertRecompositionCountAtMost(1)
    }
}
