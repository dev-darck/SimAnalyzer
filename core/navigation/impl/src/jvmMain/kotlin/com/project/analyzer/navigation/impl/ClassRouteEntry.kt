package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import com.project.analyzer.navigation.api.Route
import kotlin.reflect.KClass

internal class ClassRouteEntry<T : Route>(
    internal val clazz: KClass<out T>,
    private val clazzContentKey: (T) -> Any,
    private val metadataProvider: (T) -> Map<String, Any>,
    private val content: @Composable (T) -> Unit,
) : RegisteredRouteEntry {

    override val id: String = clazz.qualifiedName ?: error("Navigation route class must have a qualified name: $clazz")

    override fun contentKey(route: Route): Any {
        @Suppress("UNCHECKED_CAST")
        return clazzContentKey(route as T)
    }

    override fun metadata(route: Route): Map<String, Any> {
        @Suppress("UNCHECKED_CAST")
        return metadataProvider(route as T)
    }

    @Composable
    override fun Content(route: Route) {
        @Suppress("UNCHECKED_CAST")
        content(route as T)
    }
}
