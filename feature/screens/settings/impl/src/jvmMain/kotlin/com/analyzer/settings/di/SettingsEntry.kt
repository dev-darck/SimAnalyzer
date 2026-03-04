package com.analyzer.settings.di

import com.analyzer.settings.presentation.SettingsScreen
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
class SettingsEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SettingsRoot.Settings) {
            SettingsScreen()
        }
    }
}
