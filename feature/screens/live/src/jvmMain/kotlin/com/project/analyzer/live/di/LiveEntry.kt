package com.project.analyzer.live.di

import com.project.analyzer.live.presentation.LiveScreen
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
class LiveEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.LiveRoot.Live) {
            LiveScreen()
        }
    }
}
