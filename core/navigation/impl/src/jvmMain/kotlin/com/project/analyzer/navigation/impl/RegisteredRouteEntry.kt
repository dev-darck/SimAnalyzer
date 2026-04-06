package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import com.project.analyzer.navigation.api.Route

internal interface RegisteredRouteEntry {

    val id: String

    fun contentKey(route: Route): Any

    fun metadata(route: Route): Map<String, Any>

    @Composable
    fun Content(route: Route)
}
