package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.Route
import kotlin.reflect.KClass

internal class Nav3EntryAdapter(
    private val scope: EntryProviderScope<Route>,
) : NavigationEntryBuilder {

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
            content = { content(it) },
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
