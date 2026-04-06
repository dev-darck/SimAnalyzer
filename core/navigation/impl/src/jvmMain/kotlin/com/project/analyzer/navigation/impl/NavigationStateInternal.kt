package com.project.analyzer.navigation.impl

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.compose.serialization.serializers.SnapshotStateMapSerializer
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.KClass

@Stable
@Serializable
internal class NavigationStateInternal(
    val startTopLevel: Root,

    @Serializable(with = SnapshotStateMapSerializer::class)
    private val stacks: SnapshotStateMap<Root, BackStack<NavRouteKey>>,

    @Serializable(with = MutableStateSerializer::class)
    private val currentTopLevelState: MutableState<Root> = mutableStateOf(startTopLevel),
) : NavigationState<Route> {

    private sealed interface ForwardAction {
        data class PushRoute(val topLevel: Root, val routeKey: NavRouteKey) : ForwardAction
        data class SwitchTopLevel(val topLevel: Root) : ForwardAction
    }

    @Transient
    private val routeKeyFactoryState: MutableState<((Route) -> NavRouteKey)?> = mutableStateOf(null)

    @Transient
    private val forwardValidators: SnapshotStateMap<KClass<out Route>, (Route) -> Boolean> = mutableStateMapOf()

    @Transient
    private val forwardActions: SnapshotStateList<ForwardAction> = mutableStateListOf()

    internal val navBackStack: ImmutableList<NavRouteKey>
        get() {
            val current = currentTopLevelState.value
            val currentStack = stack(current)

            require(currentStack.isNotEmpty()) { "Current stack cannot be empty: $current" }

            if (current == startTopLevel) return currentStack.toImmutableList()

            val startStack = stack(startTopLevel)
            require(startStack.isNotEmpty()) { "Start stack cannot be empty: $startTopLevel" }

            val startRoot = startStack.first()
            return StartPlusStack(startRoot, currentStack).toImmutableList()
        }

    override val backStack: ImmutableList<Route>
        get() = navBackStack.map(NavRouteKey::route).toImmutableList()

    override val isCurrentRouteRoot: Boolean
        get() = navBackStack.last().route.isRoot

    override val canGoForward: Boolean
        get() {
            val nextAction = forwardActions.lastOrNull() ?: return false
            return when (nextAction) {
                is ForwardAction.SwitchTopLevel -> true
                is ForwardAction.PushRoute -> {
                    val route = nextAction.routeKey.route
                    val validator = forwardValidators[route::class]
                    validator?.invoke(route) ?: true
                }
            }
        }

    override val currentTopLevel: Root
        get() = currentTopLevelState.value

    override fun switchTopLevel(topLevel: Root) {
        if (currentTopLevel == topLevel) return

        clearForward()
        currentTopLevelState.value = topLevel

        require(stack(topLevel).isNotEmpty()) { "TopLevel=$topLevel has empty stack" }
    }

    override fun navigate(route: Route) {
        clearForward()

        val topLevel = route.topLevel
        if (currentTopLevelState.value != topLevel) {
            currentTopLevelState.value = topLevel
        }

        val stack = stack(topLevel)
        val routeKey = routeKey(route)

        if (route.isRoot) {
            if (stack.isEmpty()) {
                stack.add(routeKey)
            } else {
                stack[0] = routeKey
                while (stack.size > 1) stack.removeLast()
            }
            return
        }

        stack.add(routeKey)
    }

    override fun navigateToTopLevel(route: Route) {
        clearForward()

        val topLevel = route.topLevel
        if (currentTopLevelState.value != topLevel) {
            currentTopLevelState.value = topLevel
        }

        if (route.isRoot) {
            setRoot(route)
            return
        }

        stack(topLevel).add(routeKey(route))
    }

    override fun registerForwardValidator(route: KClass<out Route>, validator: (Route) -> Boolean) {
        forwardValidators[route] = validator
    }

    override fun unregisterForwardValidator(route: KClass<out Route>) {
        forwardValidators.remove(route)
    }

    override fun handleBack(): Boolean {
        val topLevel = currentTopLevelState.value
        val stack = stack(topLevel)

        if (stack.size > 1) {
            val popped = stack.removeLast()
            forwardActions.addLast(ForwardAction.PushRoute(topLevel = topLevel, routeKey = popped))
            return true
        }

        if (topLevel != startTopLevel) {
            forwardActions.addLast(ForwardAction.SwitchTopLevel(topLevel = topLevel))
            currentTopLevelState.value = startTopLevel
            return true
        }

        return false
    }

    override fun handleForward(): Boolean {
        val action = forwardActions.lastOrNull() ?: return false

        if (action is ForwardAction.PushRoute) {
            val route = action.routeKey.route
            val validator = forwardValidators[route::class]
            if (validator?.invoke(route) == false) {
                forwardActions.removeAt(forwardActions.lastIndex)
                return handleForward()
            }
        }

        forwardActions.removeAt(forwardActions.lastIndex)
        return when (action) {
            is ForwardAction.SwitchTopLevel -> {
                currentTopLevelState.value = action.topLevel
                true
            }

            is ForwardAction.PushRoute -> {
                val topLevel = action.topLevel
                currentTopLevelState.value = topLevel
                stack(topLevel).add(action.routeKey)
                true
            }
        }
    }

    internal fun bindRouteKeyFactory(factory: (Route) -> NavRouteKey): NavigationStateInternal = apply {
        routeKeyFactoryState.value = factory
    }

    private fun stack(topLevel: Root): BackStack<NavRouteKey> = stacks.getValue(topLevel)

    private fun routeKey(route: Route): NavRouteKey =
        routeKeyFactoryState.value?.invoke(route)
            ?: error("NavigationStateInternal is not bound to a NavRouteKey factory")

    private fun clearForward() {
        if (forwardActions.isNotEmpty()) forwardActions.clear()
    }

    private fun setRoot(root: Route, resetStack: Boolean = true, switchToTopLevel: Boolean = true) {
        require(root.isRoot) { "setRoot expects a root route, got=$root" }

        val topLevel = root.topLevel
        val stack = stack(topLevel)
        val routeKey = routeKey(root)

        if (resetStack) {
            stack.clear()
            stack.add(routeKey)
        } else {
            if (stack.isEmpty()) stack.add(routeKey) else stack[0] = routeKey
        }

        if (switchToTopLevel) {
            currentTopLevelState.value = topLevel
        }
    }
}

private class StartPlusStack<T>(private val startRoot: T, private val tail: List<T>) :
    AbstractList<T>(),
    RandomAccess {

    override val size: Int get() = 1 + tail.size

    override fun get(index: Int): T = if (index == 0) startRoot else tail[index - 1]
}
