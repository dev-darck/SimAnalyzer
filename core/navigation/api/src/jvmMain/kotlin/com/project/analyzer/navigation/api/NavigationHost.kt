package com.project.analyzer.navigation.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

public interface NavigationHost {

    @Composable
    public fun rememberNavigationState(startTopLevel: Root = Root.Live): NavigationState<Route>

    @Composable
    public fun Content(navigationState: NavigationState<Route>, modifier: Modifier = Modifier)
}
