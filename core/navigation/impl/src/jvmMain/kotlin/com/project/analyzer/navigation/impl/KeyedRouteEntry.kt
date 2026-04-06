package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import com.project.analyzer.navigation.api.Route

internal class KeyedRouteEntry<T : Route>(
    internal val key: T,
    private val contentKey: Any,
    private val metadata: Map<String, Any>,
    private val content: @Composable (T) -> Unit,
) : RegisteredRouteEntry {

    override val id: String = buildString {
        append(key::class.qualifiedName ?: key::class.simpleName ?: "route")
        append(':')
        append(key)
    }

    override fun contentKey(route: Route): Any = contentKey

    override fun metadata(route: Route): Map<String, Any> = metadata

    @Composable
    override fun Content(route: Route) {
        @Suppress("UNCHECKED_CAST")
        content(route as T)
    }
}
