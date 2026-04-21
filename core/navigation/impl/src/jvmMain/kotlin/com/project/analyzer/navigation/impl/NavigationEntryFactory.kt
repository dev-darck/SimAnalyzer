package com.project.analyzer.navigation.impl

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import com.project.analyzer.navigation.api.NavigationScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(NavigationScope::class)
class NavigationEntryFactory(private val routeEntryRegistry: RouteEntryRegistry) {

    internal fun toKey(route: com.project.analyzer.navigation.api.Route): NavRouteKey = routeEntryRegistry.keyOf(route)

    internal fun create(): (NavRouteKey) -> NavEntry<NavRouteKey> = entryProvider {
        addEntryProvider(
            clazz = NavRouteKey::class,
            clazzContentKey = { routeKey ->
                routeEntryRegistry.resolve(routeKey).contentKey(routeKey.route)
            },
            metadata = { routeKey ->
                routeEntryRegistry.resolve(routeKey).metadata(routeKey.route)
            },
            content = { routeKey ->
                routeEntryRegistry.resolve(routeKey).Content(routeKey.route)
            },
        )
    }
}
