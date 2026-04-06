package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.savedstate.serialization.SavedStateConfiguration
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route

@Composable
internal fun rememberNavigationState(
    entryFactory: NavigationEntryFactory,
    startTopLevel: Root = Root.Live,
): NavigationStateInternal = rememberSerializable(
    configuration = SavedStateConfiguration.DEFAULT,
    serializer = NavigationStateInternal.serializer(),
) {
    NavigationStateInternal(
        startTopLevel = startTopLevel,
        stacks = mutableStateMapOf<Root, BackStack<NavRouteKey>>(
            Root.Live to BackStack(mutableStateListOf(entryFactory.toKey(Route.LiveRoot.Live))),
            Root.Session to BackStack(mutableStateListOf(entryFactory.toKey(Route.SessionRoot.Session))),
            Root.Setup to BackStack(mutableStateListOf(entryFactory.toKey(Route.SetupRoot.Setup))),
            Root.Settings to BackStack(mutableStateListOf(entryFactory.toKey(Route.SettingsRoot.Settings))),
        ),
        currentTopLevelState = mutableStateOf(startTopLevel),
    )
}.bindRouteKeyFactory(entryFactory::toKey)
