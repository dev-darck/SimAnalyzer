package com.project.analyzer.devsettings.di

import com.project.analyzer.devsettings.presentation.DevSettingsScreen
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(NavigationScope::class)
@SingleIn(NavigationScope::class)
@Suppress("unused")
class DevSettingsEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SettingsRoot.DevSettings) { route ->
            DevSettingsScreen(route = route)
        }
        entry(Route.SettingsRoot.DevCalibration) { route ->
            DevSettingsScreen(route = route)
        }
        entry(Route.SettingsRoot.DevCalibrationVerify::class) { route ->
            DevSettingsScreen(route = route)
        }
        entry(Route.SettingsRoot.DevTelemetry) { route ->
            DevSettingsScreen(route = route)
        }
        entry(Route.SettingsRoot.DevHud) { route ->
            DevSettingsScreen(route = route)
        }
    }
}
