package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.savedstate.serialization.SavedStateConfiguration
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.Route.HomeRoot.Home
import com.project.analyzer.navigation.api.Route.SettingsRoot.Settings
import com.project.analyzer.navigation.api.Route.TelemetryRoot.TelemetryDetails

@Composable
fun rememberNavigationState(
    startRoute: Route = Home,
): NavigationState<Route> {
    return rememberSerializable(
        configuration = SavedStateConfiguration.DEFAULT,
        serializer = NavigationStateInternal.serializer(Route.serializer())
    ) {
        NavigationStateInternal(
            startRoute = startRoute,
            stacks = mutableStateMapOf(
                Home to BackStack(mutableStateListOf(Home)),
                TelemetryDetails to BackStack(mutableStateListOf(TelemetryDetails)),
                Settings to BackStack(mutableStateListOf(Settings)),
            ),
            currentTopLevelState = mutableStateOf(startRoute)
        )
    }
}
