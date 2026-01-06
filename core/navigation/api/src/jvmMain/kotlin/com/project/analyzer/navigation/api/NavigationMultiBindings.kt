package com.project.analyzer.navigation.api

import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

@ContributesTo(NavigationScope::class)
public interface NavigationMultiBindings {

    @Multibinds(allowEmpty = true)
    public val entryContributions: Set<RouteEntryBuilder>
}
