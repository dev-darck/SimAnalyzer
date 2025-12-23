package com.project.analyzer.navigation.impl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateMapSerializer
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Route
import kotlinx.serialization.Serializable

@Serializable
class NavigationStateInternal<T : Route>(
    val startRoute: T,
    @Serializable(with = SnapshotStateMapSerializer::class)
    private val stacks: SnapshotStateMap<Route, BackStack<T>>,
    @Serializable(with = MutableStateSerializer::class)
    private val currentTopLevelState: MutableState<T> = mutableStateOf(startRoute),
) : NavigationState<T> {

    override var currentTopLevel: T
        get() = currentTopLevelState.value
        set(value) {
            currentTopLevelState.value = value
        }

    override val backStack: BackStack<T>
        get() = stacks.getValue(currentTopLevel)

    override fun navigate(route: T) {
        if (route.isRoot) {
            navigateToTopLevel(route)
            return
        }

        stacks.getValue(currentTopLevel).add(route)
    }

    override fun navigateToTopLevel(route: T) {
        if (!route.isRoot) {
            navigate(route)
            return
        }

        if (currentTopLevel == route) return

        currentTopLevel = route
    }

    override fun handleBack(): Boolean {
        val stack = stacks.getValue(currentTopLevel)

        if (stack.size > 1) {
            stack.removeLast()
            return true
        }

        if (currentTopLevel != startRoute && startRoute.isRoot) {
            currentTopLevel = startRoute
            return true
        }

        return false
    }
}
