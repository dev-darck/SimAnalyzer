package com.project.analyzer.navigation.impl

import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(NavigationScope::class)
class RouteEntryRegistry(
    private val builders: Set<@JvmSuppressWildcards RouteEntryBuilder>,
) {

    fun registerInto(entryBuilder: NavigationEntryBuilder) {
        builders.forEach { routeBuilder ->
            with(routeBuilder) { entryBuilder.build() }
        }
    }
}
