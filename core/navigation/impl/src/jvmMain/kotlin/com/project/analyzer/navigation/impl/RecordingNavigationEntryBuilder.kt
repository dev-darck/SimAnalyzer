package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.Route
import kotlin.reflect.KClass

internal class RecordingNavigationEntryBuilder(
    private val entries: MutableList<RegisteredRouteEntry>,
) : NavigationEntryBuilder {

    override fun <T : Route> entry(
        key: T,
        contentKey: Any,
        metadata: Map<String, Any>,
        content: @Composable (T) -> Unit,
    ) {
        entries += KeyedRouteEntry(
            key = key,
            contentKey = contentKey,
            metadata = metadata,
            content = content,
        )
    }

    override fun <T : Route> entry(
        clazz: KClass<out T>,
        clazzContentKey: (T) -> Any,
        metadata: (T) -> Map<String, Any>,
        content: @Composable (T) -> Unit,
    ) {
        entries += ClassRouteEntry(
            clazz = clazz,
            clazzContentKey = clazzContentKey,
            metadataProvider = metadata,
            content = content,
        )
    }
}
