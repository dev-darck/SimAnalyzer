package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.savedstate.serialization.SavedStateConfiguration
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route

@Composable
fun rememberNavigationState(startTopLevel: Root = Root.Live): NavigationState<Route> = rememberSerializable(
    configuration = SavedStateConfiguration.DEFAULT,
    serializer = NavigationStateInternal.serializer(Route.serializer()),
) {
    NavigationStateInternal(
        startTopLevel = startTopLevel,
        stacks = mutableStateMapOf(
            Root.Live to BackStack(mutableStateListOf(Route.LiveRoot.Live)),
            Root.Session to BackStack(mutableStateListOf(Route.SessionRoot.Session)),
            Root.Setup to BackStack(mutableStateListOf(Route.SetupRoot.Setup)),
            Root.Settings to BackStack(mutableStateListOf(Route.SettingsRoot.Settings)),
        ),
        currentTopLevelState = mutableStateOf(startTopLevel),
    )
}
