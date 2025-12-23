package com.project.analyzer.navigation.api

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public val LocalNavigator: ProvidableCompositionLocal<NavigationState<Route>> = staticCompositionLocalOf {
    error("No AppNavigator provided")
}
