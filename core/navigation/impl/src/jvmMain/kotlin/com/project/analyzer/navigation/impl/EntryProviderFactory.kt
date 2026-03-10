package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlin.reflect.KClass

@Inject
@SingleIn(NavigationScope::class)
@ContributesBinding(NavigationScope::class, binding = binding<EntryFactory>())
class EntryProviderFactory(private val builders: Set<@JvmSuppressWildcards RouteEntryBuilder>) : EntryFactory {

    override fun create(): (Route) -> NavEntry<Route> = entryProvider {
        val adapter: NavigationEntryBuilder = Nav3EntryAdapter(this)

        builders.forEach { builder ->
            with(builder) { adapter.build() }
        }
    }
}

private class Nav3EntryAdapter(private val scope: EntryProviderScope<Route>) : NavigationEntryBuilder {

    override fun <T : Route> entry(
        key: T,
        contentKey: Any,
        metadata: Map<String, Any>,
        content: @Composable (T) -> Unit,
    ) {
        scope.addEntryProvider(
            key = key,
            contentKey = contentKey,
            metadata = metadata,
            content = {
                content(it)
            },
        )
    }

    override fun <T : Route> entry(
        clazz: KClass<out T>,
        clazzContentKey: (T) -> Any,
        metadata: (T) -> Map<String, Any>,
        content: @Composable (T) -> Unit,
    ) {
        scope.addEntryProvider(
            clazz = clazz,
            clazzContentKey = clazzContentKey,
            metadata = metadata,
            content = content,
        )
    }
}
