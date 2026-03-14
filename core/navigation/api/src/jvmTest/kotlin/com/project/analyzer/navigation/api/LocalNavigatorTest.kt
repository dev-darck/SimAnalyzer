@file:OptIn(ExperimentalTestApi::class)

package com.project.analyzer.navigation.api

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.reflect.KClass

class LocalNavigatorTest {

    @Test
    fun `local navigator provides current root with bounded recompositions`() = runDesktopComposeUiTest {
        val state = NavigationRootStateHolder()

        setContent {
            CompositionLocalProvider(LocalNavigator provides FakeNavigationState(state.currentRoot)) {
                Text(
                    text = LocalNavigator.current.currentTopLevel.name,
                    modifier = Modifier
                        .uiTestTag(TestTags.NavigationLocal)
                        .trackRecompositions(),
                )
            }
        }

        onNodeWithText(Root.Live.name).assertIsDisplayed()
        onNodeWithTag(TestTags.NavigationLocal.value).assertRecompositionCountAtMost(1)

        runOnIdle {
            state.currentRoot = Root.Settings
        }
        waitForIdle()

        onNodeWithText(Root.Settings.name).assertIsDisplayed()
        onNodeWithTag(TestTags.NavigationLocal.value).assertRecompositionCountAtMost(3)
    }
}

private class NavigationRootStateHolder {

    var currentRoot by mutableStateOf(Root.Live)
}

private class FakeNavigationState(
    override val currentTopLevel: Root,
    override val backStack: ImmutableList<Route> = persistentListOf(Route.LiveRoot.Live),
) : NavigationState<Route> {

    override val canGoForward: Boolean = false
    override val isCurrentRouteRoot: Boolean = true

    override fun switchTopLevel(topLevel: Root) = Unit

    override fun navigateToTopLevel(route: Route) = Unit

    override fun navigate(route: Route) = Unit

    override fun handleBack(): Boolean = false

    override fun handleForward(): Boolean = false

    override fun registerForwardValidator(route: KClass<out Route>, validator: (Route) -> Boolean) = Unit

    override fun unregisterForwardValidator(route: KClass<out Route>) = Unit
}
