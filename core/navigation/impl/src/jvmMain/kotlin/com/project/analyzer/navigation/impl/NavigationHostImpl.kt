package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.project.analyzer.navigation.api.NavigationHost
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import com.project.analyzer.navigation.impl.rememberNavigationState as rememberNavigationStateInternal

@Inject
@SingleIn(NavigationScope::class)
@ContributesBinding(NavigationScope::class, binding = binding<NavigationHost>())
class NavigationHostImpl(private val entryFactory: NavigationEntryFactory) : NavigationHost {

    @Composable
    override fun rememberNavigationState(startTopLevel: Root): NavigationState<Route> = rememberNavigationStateInternal(
        entryFactory = entryFactory,
        startTopLevel = startTopLevel,
    )

    @Composable
    override fun Content(navigationState: NavigationState<Route>, modifier: Modifier) {
        val runtimeState = navigationState as? NavigationStateInternal
            ?: error("NavigationHost requires state created by NavigationHost.rememberNavigationState()")
        val entryProvider = remember { entryFactory.create() }

        AppNavGraph(
            navigationState = runtimeState,
            entryProvider = entryProvider,
            modifier = modifier,
        )
    }
}
