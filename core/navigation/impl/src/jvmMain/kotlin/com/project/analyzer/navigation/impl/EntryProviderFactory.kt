package com.project.analyzer.navigation.impl

import androidx.navigation3.runtime.entryProvider
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<EntryFactory>())
@Inject
class EntryProviderFactory(
    private val builders: Set<@JvmSuppressWildcards RouteEntryBuilder>
) : EntryFactory {
    override fun create() = entryProvider {
        builders.forEach { builder ->
            with(builder) { build() }
        }
    }
}
