package com.project.analyzer.navigation.api

import androidx.compose.runtime.Composable

public fun interface RouteEntryBuilder {

    public fun NavigationEntryBuilder.build()
}

public interface NavigationEntryBuilder {

    public fun <T : Route> entry(
        key: T,
        contentKey: Any = key,
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (T) -> Unit,
    )
}
