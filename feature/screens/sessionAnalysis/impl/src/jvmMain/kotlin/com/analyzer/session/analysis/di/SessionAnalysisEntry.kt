package com.analyzer.session.analysis.di

import com.analyzer.session.analysis.presentation.SessionAnalysisScreen
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Registers the session-analysis screen with navigation and exposes the module injector.
 */
@Inject
@ContributesIntoSet(NavigationScope::class)
@SingleIn(NavigationScope::class)
class SessionAnalysisEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SessionRoot.SessionAnalysis::class) {
            SessionAnalysisScreen(sessionId = sessionId)
        }
    }
}
