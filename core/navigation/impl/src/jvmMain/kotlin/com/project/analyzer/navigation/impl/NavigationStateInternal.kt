package com.project.analyzer.navigation.impl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateMapSerializer
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route
import kotlinx.serialization.Serializable

@Serializable
class NavigationStateInternal<T : Route>(
    val startTopLevel: Root,

    @Serializable(with = SnapshotStateMapSerializer::class)
    private val stacks: SnapshotStateMap<Root, BackStack<T>>,

    @Serializable(with = MutableStateSerializer::class)
    private val currentTopLevelState: MutableState<Root> = mutableStateOf(startTopLevel),
) : NavigationState<T> {

    override val currentTopLevel: Root
        get() = currentTopLevelState.value

    override val backStack: BackStack<T>
        get() = stacks.getValue(currentTopLevelState.value)

    override fun switchTopLevel(topLevel: Root) {
        val current = currentTopLevel
        if (current == topLevel) return
        currentTopLevelState.value = topLevel
    }

    override fun navigate(route: T) {
        val tl = route.topLevel

        if (currentTopLevelState.value != tl) {
            currentTopLevelState.value = tl
        }

        val stack = stacks.getValue(tl)

        if (route.isRoot) {
            val existingRoot = stack.firstOrNull()
            if (existingRoot != route) {
                stack.clear()
                stack.add(route)
            }
            return
        }

        stack.add(route)
    }

    override fun navigateToTopLevel(route: T) {
        if (!route.isRoot) {
            navigate(route)
            return
        }
        setRoot(root = route, resetStack = false, switchToTopLevel = true)
    }

    override fun handleBack(): Boolean {
        val tl = currentTopLevelState.value
        val stack = stacks.getValue(tl)

        if (stack.size > 1) {
            stack.removeLast()
            return true
        }

        if (tl != startTopLevel) {
            currentTopLevelState.value = startTopLevel
            return true
        }

        return false
    }

    private fun setRoot(
        root: T,
        resetStack: Boolean = true,
        switchToTopLevel: Boolean = true,
    ) {
        require(root.isRoot) { "setRoot expects a root route, got=$root" }

        val tl = root.topLevel
        val stack = stacks.getValue(tl)

        if (resetStack) {
            stack.clear()
            stack.add(root)
        } else {
            if (stack.isEmpty()) stack.add(root) else stack[0] = root
        }

        if (switchToTopLevel) {
            currentTopLevelState.value = tl
        }
    }

}
