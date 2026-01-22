package com.project.analyzer.hudSettings.di

import com.project.analyzer.hudSettings.presentation.HudSettingsScreen
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(NavigationScope::class)
@ContributesIntoSet(NavigationScope::class)
class HudSettingsEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SettingsRoot.HudSettings) {
            HudSettingsScreen()
        }
    }

}
