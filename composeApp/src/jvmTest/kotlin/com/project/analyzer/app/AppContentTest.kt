package com.project.analyzer.app

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.modifier.uiTestTagOf
import com.project.analyzer.ui.testing.assertRecompositionCountAtMost
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalTestApi::class)
class AppContentTest {

    @Test
    fun switchesTopLevelFromSidebar() = runDesktopComposeUiTest {
        val navigationState = FakeNavigationState()

        setContent {
            TestApp(navigationState)
        }

        onNodeWithTag(TestTags.App.value).assertIsDisplayed()
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(1)
        onNodeWithTag(TestTags.Sidebar.value).assertIsDisplayed()
        onNodeWithText("screen=Live").assertIsDisplayed()

        onNodeWithTag(sidebarItemTag(Root.Session)).performClick()
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(3)
        onNodeWithText("screen=Session").assertIsDisplayed()
    }

    @Test
    fun hidesSidebarOutsideRootRoute() = runDesktopComposeUiTest {
        val navigationState = FakeNavigationState()

        setContent {
            TestApp(navigationState)
        }

        onNodeWithTag(TestTags.App.value).assertIsDisplayed()
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(1)
        onNodeWithTag(TestTags.Sidebar.value).assertIsDisplayed()

        runOnIdle {
            navigationState.navigate(Route.LiveRoot.LiveDetails)
        }
        mainClock.advanceTimeBy(500)
        waitForIdle()

        onAllNodesWithTag(TestTags.Sidebar.value).assertCountEquals(0)
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(3)
    }

    @Test
    fun forwardsBackAndForwardStateIntoTitleBar() = runDesktopComposeUiTest {
        val navigationState = FakeNavigationState(
            backStack = persistentListOf(Route.LiveRoot.Live, Route.LiveRoot.LiveDetails),
            canGoForward = true,
            isCurrentRouteRoot = false,
        )

        setContent {
            TestApp(navigationState)
        }

        onNodeWithTag(TestTags.App.value).assertIsDisplayed()
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(1)
        onNodeWithText("back-enabled=true").assertIsDisplayed()
        onNodeWithText("forward-enabled=true").assertIsDisplayed()

        onNodeWithTag(backTag).performClick()
        onNodeWithTag(forwardTag).performClick()

        onNodeWithText("back-clicks=1").assertIsDisplayed()
        onNodeWithText("forward-clicks=1").assertIsDisplayed()
        onNodeWithTag(TestTags.App.value).assertRecompositionCountAtMost(5)
    }
}

@Composable
private fun TestApp(navigationState: FakeNavigationState) {
    SimAnalyzerTheme {
        AppContent(
            navigationState = navigationState,
            titleBar = { canGoBack, canGoForward, onBack, onForward, _ ->
                TestTitleBar(
                    navigationState = navigationState,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onBack = onBack,
                    onForward = onForward,
                )
            },
            navigationContent = {
                Text("screen=${navigationState.currentTopLevel.name}")
            },
        )
    }
}

@Composable
private fun TestTitleBar(
    navigationState: FakeNavigationState,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Column {
        Text("back-enabled=$canGoBack")
        Text("forward-enabled=$canGoForward")
        Button(
            modifier = Modifier.uiTestTag(AppContentTestUiTags.Back),
            enabled = canGoBack,
            onClick = onBack,
        ) {
            Text("Back")
        }
        Button(
            modifier = Modifier.uiTestTag(AppContentTestUiTags.Forward),
            enabled = canGoForward,
            onClick = onForward,
        ) {
            Text("Forward")
        }
        Text("back-clicks=${navigationState.backClicks}")
        Text("forward-clicks=${navigationState.forwardClicks}")
    }
}

@Stable
private class FakeNavigationState(
    currentTopLevel: Root = Root.Live,
    backStack: PersistentList<Route> = persistentListOf(Route.LiveRoot.Live),
    canGoForward: Boolean = false,
    isCurrentRouteRoot: Boolean = true,
) : NavigationState<Route> {

    private var currentTopLevelState by mutableStateOf(currentTopLevel)
    private var backStackState by mutableStateOf(backStack)
    private var canGoForwardState by mutableStateOf(canGoForward)
    private var isCurrentRouteRootState by mutableStateOf(isCurrentRouteRoot)

    var backClicks: Int by mutableStateOf(0)
        private set

    var forwardClicks: Int by mutableStateOf(0)
        private set

    override val currentTopLevel: Root
        get() = currentTopLevelState

    override val backStack: ImmutableList<Route>
        get() = backStackState

    override val canGoForward: Boolean
        get() = canGoForwardState

    override val isCurrentRouteRoot: Boolean
        get() = isCurrentRouteRootState

    override fun switchTopLevel(topLevel: Root) {
        currentTopLevelState = topLevel
        backStackState = persistentListOf(rootRoute(topLevel))
        isCurrentRouteRootState = true
    }

    override fun navigateToTopLevel(route: Route) {
        currentTopLevelState = route.topLevel
        backStackState =
            if (route.isRoot) persistentListOf(route) else persistentListOf(rootRoute(route.topLevel), route)
        isCurrentRouteRootState = route.isRoot
        canGoForwardState = false
    }

    override fun navigate(route: Route) {
        currentTopLevelState = route.topLevel
        backStackState = if (route.isRoot) {
            persistentListOf(route)
        } else {
            backStackState.add(route)
        }
        isCurrentRouteRootState = route.isRoot
        canGoForwardState = false
    }

    override fun handleBack(): Boolean {
        backClicks += 1

        if (backStackState.size <= 1) return false

        backStackState = backStackState.removeAt(backStackState.lastIndex)
        isCurrentRouteRootState = backStackState.last().isRoot
        canGoForwardState = true
        return true
    }

    override fun handleForward(): Boolean {
        forwardClicks += 1
        val canHandle = canGoForwardState
        canGoForwardState = false
        return canHandle
    }

    override fun registerForwardValidator(route: KClass<out Route>, validator: (Route) -> Boolean) = Unit

    override fun unregisterForwardValidator(route: KClass<out Route>) = Unit

    private fun rootRoute(root: Root): Route = when (root) {
        Root.Live -> Route.LiveRoot.Live
        Root.Session -> Route.SessionRoot.Session
        Root.Settings -> Route.SettingsRoot.Settings
        Root.Setup -> Route.SetupRoot.Setup
        Root.Track -> error("Track root is not used by AppContent tests")
    }
}

private object AppContentTestUiTags {

    val Back = uiTestTagOf(TestTags.Test, "back")
    val Forward = uiTestTagOf(TestTags.Test, "forward")
}

private fun sidebarItemTag(root: Root): String = TestTags.Sidebar.child(root).value
private val backTag = AppContentTestUiTags.Back.value
private val forwardTag = AppContentTestUiTags.Forward.value
