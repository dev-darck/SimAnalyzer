package com.project.analyzer.navigation.api

public interface NavigationState<T : Route> {

    public val currentTopLevel: Root
    public val backStack: List<T>

    public fun switchTopLevel(topLevel: Root)
    public fun navigateToTopLevel(route: T)
    public fun navigate(route: T)
    public fun handleBack(): Boolean
}
