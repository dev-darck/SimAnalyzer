package com.project.analyzer.navigation.impl

import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(NavigationScope::class)
class RouteEntryRegistry(private val builders: Set<@JvmSuppressWildcards RouteEntryBuilder>) {

    private val compiledRegistry: CompiledRouteRegistry by lazy(LazyThreadSafetyMode.NONE) {
        val recordedEntries = mutableListOf<RegisteredRouteEntry>()
        val entryBuilder = RecordingNavigationEntryBuilder(recordedEntries)
        builders.forEach { routeBuilder ->
            with(routeBuilder) { entryBuilder.build() }
        }
        CompiledRouteRegistry(
            exactEntries = recordedEntries
                .filterIsInstance<KeyedRouteEntry<*>>()
                .associateBy { it.key },
            classEntries = recordedEntries
                .filterIsInstance<ClassRouteEntry<*>>()
                .associateBy { it.clazz },
            entriesById = recordedEntries.associateBy { it.id },
        )
    }

    internal fun resolve(route: Route): RegisteredRouteEntry = compiledRegistry.exactEntries[route]
        ?: compiledRegistry.classEntries[route::class]
        ?: error("Unknown screen $route")

    internal fun resolve(routeKey: NavRouteKey): RegisteredRouteEntry = compiledRegistry.entriesById[routeKey.entryId]
        ?: error("Unknown entry id ${routeKey.entryId} for route ${routeKey.route}")

    internal fun keyOf(route: Route): NavRouteKey {
        val entry = resolve(route)
        return NavRouteKey(route = route, entryId = entry.id)
    }
}

private data class CompiledRouteRegistry(
    val exactEntries: Map<Route, KeyedRouteEntry<*>>,
    val classEntries: Map<kotlin.reflect.KClass<out Route>, ClassRouteEntry<*>>,
    val entriesById: Map<String, RegisteredRouteEntry>,
)
