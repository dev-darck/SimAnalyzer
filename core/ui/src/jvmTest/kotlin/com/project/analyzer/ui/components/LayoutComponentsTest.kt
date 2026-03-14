@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveGridMode
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class LayoutComponentsTest {

    @Test
    fun `session stat card uppercases title and stays stable`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                SessionStatCard(
                    title = "Favorite car",
                    value = "BMW M4 GT3",
                    modifier = Modifier
                        .uiTestTag(TestTags.SessionStatCard)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("FAVORITE CAR").assertIsDisplayed()
        onNodeWithText("BMW M4 GT3").assertIsDisplayed()
        onNodeWithTag(TestTags.SessionStatCard.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `stats row renders all cards with bounded recompositions`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                StatsRow(
                    stats = persistentListOf(
                        StatItem(title = "Distance", value = "120 km"),
                        StatItem(title = "Sessions", value = "8"),
                        StatItem(title = "Incidents", value = "0"),
                    ),
                    modifier = Modifier
                        .uiTestTag(TestTags.StatsRow)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText("DISTANCE").assertIsDisplayed()
        onNodeWithText("120 km").assertIsDisplayed()
        onNodeWithText("SESSIONS").assertIsDisplayed()
        onNodeWithText("INCIDENTS").assertIsDisplayed()
        onNodeWithTag(TestTags.StatsRow.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `responsive panel card renders nested content and stays stable`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                ResponsivePanelCard(
                    modifier = Modifier
                        .uiTestTag(TestTags.PanelCard)
                        .trackRecompositions(),
                ) {
                    Text("Panel content")
                }
            }
        }

        onNodeWithText("Panel content").assertIsDisplayed()
        onNodeWithTag(TestTags.PanelCard.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `scrollable screen column renders content and scrollbar`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                ScrollableScreenColumn(
                    modifier = Modifier
                        .width(260.dp)
                        .height(220.dp)
                        .uiTestTag(TestTags.ScreenLayout)
                        .trackRecompositions(),
                ) {
                    repeat(14) { index ->
                        Text("Screen item ${index + 1}")
                    }
                }
            }
        }

        onNodeWithText("Screen item 1").assertIsDisplayed()
        onAllNodesWithTag(TestTags.ScrollbarVertical.value).assertCountEquals(1)
        onNodeWithTag(TestTags.ScreenLayout.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `responsive screen uses linear content in compact mode`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                Box(
                    modifier = Modifier
                        .width(820.dp)
                        .height(320.dp),
                ) {
                    ResponsiveScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .uiTestTag(TestTags.ResponsiveScreen)
                            .trackRecompositions(),
                        compactMaxWidth = 900.dp,
                        mediumMaxWidth = 1250.dp,
                    ) {
                        item(key = "compact") { isLinear ->
                            Text("compact-linear=$isLinear")
                        }
                    }
                }
            }
        }

        onNodeWithText("compact-linear=true").assertIsDisplayed()
        onNodeWithTag(TestTags.ResponsiveScreen.value).assertRecompositionCountAtMost(1)
    }

    @Test
    fun `responsive screen uses grid semantics in medium mode`() = runDesktopComposeUiTest {
        setContent {
            SimAnalyzerTheme {
                Box(
                    modifier = Modifier
                        .width(1040.dp)
                        .height(360.dp),
                ) {
                    ResponsiveScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .uiTestTag(TestTags.ResponsiveScreen)
                            .trackRecompositions(),
                        compactMaxWidth = 900.dp,
                        mediumMaxWidth = 1250.dp,
                        gridMode = ResponsiveGridMode.Grid,
                    ) {
                        item(key = "grid-item") { isLinear ->
                            Text("grid-item-linear=$isLinear")
                        }
                        item(key = "full-item", isContentFull = true) { isLinear ->
                            Text("grid-full-linear=$isLinear")
                        }
                    }
                }
            }
        }

        onNodeWithText("grid-item-linear=false").assertIsDisplayed()
        onNodeWithText("grid-full-linear=true").assertIsDisplayed()
        onNodeWithTag(TestTags.ResponsiveScreen.value).assertRecompositionCountAtMost(1)
    }
}
