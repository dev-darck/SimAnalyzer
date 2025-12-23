package com.project.analyzer.navigation.impl

import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Route

fun NavigationState<Route>.isSelected(route: Route): Boolean =
    currentTopLevel == route
