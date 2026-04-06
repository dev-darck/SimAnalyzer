@file:OptIn(ExperimentalFoundationApi::class)

package com.project.analyzer.navigation.impl

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.project.analyzer.navigation.api.LocalNavigator

@Composable
internal fun AppNavGraph(
    navigationState: NavigationStateInternal,
    entryProvider: (NavRouteKey) -> NavEntry<NavRouteKey>,
    modifier: Modifier = Modifier,
) {
    val owner = rememberNavigationEventDispatcherOwner()
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
        val dispatcher = owner.navigationEventDispatcher
        val mouseInput = remember { MouseBackForwardNavigationEventInput() }

        val entries = rememberDecoratedNavEntries(
            backStack = navigationState.navBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider,
        )

        require(entries.isNotEmpty()) { "BackStack entries cannot be empty" }

        val sceneState = rememberSceneState(
            entries = entries,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            sharedTransitionScope = null,
            onBack = navigationState::handleBack,
        )

        val scene = sceneState.currentScene

        val currentInfo = SceneInfo(scene)
        val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
        val gestureState = rememberNavigationEventState(
            currentInfo = currentInfo,
            backInfo = previousSceneInfos,
        )

        NavigationEventHandler(
            state = gestureState,
            isBackEnabled = scene.previousEntries.isNotEmpty(),
            onBackCompleted = {
                // If `enabled` becomes stale (e.g., it was set to false but a gesture was
                // dispatched in the same frame), this may result in no entries being popped
                // due to entries.size being smaller than scene.previousEntries.size
                // but that's preferable to crashing with an IndexOutOfBoundsException
                repeat(entries.size - scene.previousEntries.size) { navigationState.handleBack() }
            },
            isForwardEnabled = navigationState.canGoForward,
            onForwardCompleted = { navigationState.handleForward() },
        )

        CompositionLocalProvider(LocalNavigator provides navigationState) {
            NavDisplay(
                sceneState = sceneState,
                modifier = modifier
                    .fillMaxSize()
                    .bindMouseBackForward(mouseInput),
                navigationEventState = gestureState,
                transitionSpec = { fadeForward()(this) },
                popTransitionSpec = { fadeBackward()(this) },
            )
        }

        DisposableEffect(dispatcher) {
            dispatcher.addInput(input = mouseInput, priority = NavigationEventDispatcher.PRIORITY_DEFAULT)
            onDispose {
                dispatcher.removeInput(mouseInput)
            }
        }
    }
}

class MouseBackForwardNavigationEventInput : NavigationEventInput() {

    fun fireBack() = dispatchOnBackCompleted()
    fun fireForward() = dispatchOnForwardCompleted()
}

private fun Modifier.bindMouseBackForward(input: MouseBackForwardNavigationEventInput): Modifier =
    onClick(matcher = PointerMatcher.mouse(PointerButton.Back), onClick = input::fireBack)
        .onClick(matcher = PointerMatcher.mouse(PointerButton.Forward), onClick = input::fireForward)
