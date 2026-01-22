package com.project.analyzer.navigation.api

import kotlin.reflect.KClass

public interface NavigationState<T : Route> {

    public val currentTopLevel: Root
    public val backStack: List<T>
    public val canGoForward: Boolean

    public fun switchTopLevel(topLevel: Root)
    public fun navigateToTopLevel(route: T)
    public fun navigate(route: T)
    public fun handleBack(): Boolean
    public fun handleForward(): Boolean
    public fun registerForwardValidator(route: KClass<out T>, validator: () -> Boolean)
    public fun unregisterForwardValidator(route: KClass<out T>)
}
