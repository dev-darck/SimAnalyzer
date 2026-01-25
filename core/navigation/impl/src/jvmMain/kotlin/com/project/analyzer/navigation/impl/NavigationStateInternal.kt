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
import kotlinx.serialization.Transient
import kotlin.reflect.KClass

@Serializable
class NavigationStateInternal<T : Route>(
    val startTopLevel: Root,

    @Serializable(with = SnapshotStateMapSerializer::class)
    private val stacks: SnapshotStateMap<Root, BackStack<T>>,

    @Serializable(with = MutableStateSerializer::class)
    private val currentTopLevelState: MutableState<Root> = mutableStateOf(startTopLevel),
) : NavigationState<T> {

    private sealed interface ForwardAction {
        data class PushRoute(val topLevel: Root, val route: Route) : ForwardAction
        data class SwitchTopLevel(val topLevel: Root) : ForwardAction
    }

    @Transient
    private val forwardValidators: MutableMap<KClass<out T>, () -> Boolean> = mutableMapOf()

    @Transient
    private val forwardActions: ArrayDeque<ForwardAction> = ArrayDeque()

    override val isCurrentRouteRoot: Boolean
        get() = backStack.last().isRoot

    override val canGoForward: Boolean
        get() {
            val nextAction = forwardActions.lastOrNull() ?: return false
            return when (nextAction) {
                is ForwardAction.SwitchTopLevel -> true

                is ForwardAction.PushRoute -> {
                    val validator = forwardValidators[nextAction.route::class]
                    validator?.invoke() ?: true
                }
            }
        }

    override val currentTopLevel: Root
        get() = currentTopLevelState.value

    override val backStack: List<T>
        get() {
            val current = currentTopLevelState.value
            val currentStack = stack(current)

            require(currentStack.isNotEmpty()) { "Current stack cannot be empty: $current" }

            if (current == startTopLevel) return currentStack

            val startStack = stack(startTopLevel)
            require(startStack.isNotEmpty()) { "Start stack cannot be empty: $startTopLevel" }

            val startRoot = startStack.first()
            return StartPlusStack(startRoot, currentStack)
        }

    override fun switchTopLevel(topLevel: Root) {
        val current = currentTopLevel
        if (current == topLevel) return

        clearForward()

        currentTopLevelState.value = topLevel

        require(stack(topLevel).isNotEmpty()) { "TopLevel=$topLevel has empty stack" }
    }

    override fun navigate(route: T) {
        clearForward()

        val tl = route.topLevel
        if (currentTopLevelState.value != tl) {
            currentTopLevelState.value = tl
        }

        val stack = stacks.getValue(tl)

        if (route.isRoot) {
            if (stack.isEmpty()) {
                stack.add(route)
            } else {
                stack[0] = route
                while (stack.size > 1) stack.removeLast()
            }
            return
        }

        stack.add(route)
    }

    override fun navigateToTopLevel(route: T) {
        clearForward()

        val tl = route.topLevel
        if (currentTopLevelState.value != tl) {
            currentTopLevelState.value = tl
        }

        if (route.isRoot) {
            setRoot(root = route)
            return
        }

        stacks.getValue(tl).add(route)
    }

    override fun registerForwardValidator(route: KClass<out T>, validator: () -> Boolean) {
        forwardValidators[route] = validator
    }

    override fun unregisterForwardValidator(route: KClass<out T>) {
        forwardValidators.remove(route)
    }

    override fun handleBack(): Boolean {
        val tl = currentTopLevelState.value
        val stack = stacks.getValue(tl)

        if (stack.size > 1) {
            val popped = stack.removeLast()
            forwardActions.addLast(ForwardAction.PushRoute(topLevel = tl, route = popped))
            return true
        }

        if (tl != startTopLevel) {
            forwardActions.addLast(ForwardAction.SwitchTopLevel(topLevel = tl))
            currentTopLevelState.value = startTopLevel
            return true
        }

        return false
    }

    override fun handleForward(): Boolean {
        val action = forwardActions.lastOrNull() ?: return false

        if (action is ForwardAction.PushRoute) {
            val validator = forwardValidators[action.route::class]
            if (validator?.invoke() == false) {
                forwardActions.removeLast()
                return handleForward()
            }
        }

        forwardActions.removeLast()
        return when (action) {
            is ForwardAction.SwitchTopLevel -> {
                currentTopLevelState.value = action.topLevel
                true
            }

            is ForwardAction.PushRoute -> {
                val tl = action.topLevel
                currentTopLevelState.value = tl
                val stack = stacks.getValue(tl)
                @Suppress("UNCHECKED_CAST")
                stack.add(action.route as T)
                true
            }
        }
    }

    private fun stack(topLevel: Root): BackStack<T> = stacks.getValue(topLevel)

    private fun clearForward() {
        if (forwardActions.isNotEmpty()) forwardActions.clear()
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

private class StartPlusStack<T>(
    private val startRoot: T,
    private val tail: List<T>,
) : AbstractList<T>(), RandomAccess {

    override val size: Int get() = 1 + tail.size
    override fun get(index: Int): T = if (index == 0) startRoot else tail[index - 1]
}
