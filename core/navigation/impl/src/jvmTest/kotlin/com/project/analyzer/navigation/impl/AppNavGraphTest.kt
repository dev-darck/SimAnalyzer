@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.navigation.impl

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import org.junit.Test

class AppNavGraphTest {

    @Test
    fun `app nav graph renders current destination and navigates with bounded recompositions`() =
        runDesktopComposeUiTest {
            lateinit var navigationState: NavigationStateInternal<Route>

            setContent {
                navigationState = rememberNavigationState() as NavigationStateInternal<Route>
                AppNavGraph(
                    navigationState = navigationState,
                    entryFactory = NavigationEntryFactory(RouteEntryRegistry(setOf(testRoutes()))),
                    modifier = Modifier
                        .uiTestTag(TestTags.NavigationGraph)
                        .trackRecompositions(),
                )
            }

            onNodeWithText("live").assertIsDisplayed()
            onNodeWithTag(TestTags.NavigationGraph.value).assertRecompositionCountAtMost(1)

            runOnIdle {
                navigationState.navigate(Route.SessionRoot.SessionDetails(sessionId = 42))
            }
            waitForIdle()

            onNodeWithText("session-42").assertIsDisplayed()
            onNodeWithTag(TestTags.NavigationGraph.value).assertRecompositionCountAtMost(7)
        }
}

private fun testRoutes(): RouteEntryBuilder = RouteEntryBuilder {
    entry(Route.LiveRoot.Live) {
        Text("live")
    }
    entry(Route.SessionRoot.Session) {
        Text("session")
    }
    entry(Route.SessionRoot.SessionDetails::class) { route: Route.SessionRoot.SessionDetails ->
        Text("session-${route.sessionId}")
    }
}
