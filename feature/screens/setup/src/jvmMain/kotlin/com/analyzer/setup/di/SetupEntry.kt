package com.analyzer.setup.di

import com.analyzer.setup.presentation.SetupScreen
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
class SetupEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SetupRoot.Setup) {
            SetupScreen()
        }
    }
}
