package com.project.analyzer.navigation.api

public interface NavigationState<T : Route> {
    public val currentTopLevel: T
    public val backStack: List<T>

    public fun navigateToTopLevel(route: T)
    public fun navigate(route: T)
    public fun handleBack(): Boolean
}
