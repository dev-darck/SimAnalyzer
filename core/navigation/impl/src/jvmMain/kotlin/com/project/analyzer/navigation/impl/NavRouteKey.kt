package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import com.project.analyzer.navigation.api.Route
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal data class NavRouteKey(
    val route: Route,
    val entryId: String,
) : NavKey
