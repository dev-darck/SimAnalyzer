package com.project.analyzer

import androidx.compose.runtime.Composable
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.impl.AppNavGraph
import com.project.analyzer.navigation.impl.rememberNavigationState

@Composable
fun App(providerFactory: EntryFactory) {
    AppNavGraph(
        navigationState = rememberNavigationState(),
        providerFactory = providerFactory
    )
}
