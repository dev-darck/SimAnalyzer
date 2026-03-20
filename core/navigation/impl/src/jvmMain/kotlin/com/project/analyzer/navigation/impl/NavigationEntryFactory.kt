package com.project.analyzer.navigation.impl

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@Inject
@SingleIn(NavigationScope::class)
@ContributesBinding(NavigationScope::class, binding = binding<EntryFactory>())
class NavigationEntryFactory(
    private val routeEntryRegistry: RouteEntryRegistry,
) : EntryFactory {

    override fun create(): (Route) -> NavEntry<Route> = entryProvider {
        routeEntryRegistry.registerInto(Nav3EntryAdapter(this))
    }
}
