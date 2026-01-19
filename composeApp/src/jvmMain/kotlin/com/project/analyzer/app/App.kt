package com.project.analyzer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.impl.AppNavGraph
import com.project.analyzer.navigation.impl.rememberNavigationState
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
fun App(providerFactory: EntryFactory) {
    val navigationState = rememberNavigationState()
    val items = remember {
        listOf(
            NavItem(Root.Live, "Live", Icons.Outlined.Star),
            NavItem(Root.Session, "Session", Icons.Outlined.AccountBox),
            NavItem(Root.Setup, "Setup", Icons.Outlined.Build),
            NavItem(Root.Settings, "Settings", Icons.Outlined.Settings)
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            topIcon = { },
            items = items,
            bottomItemsCount = 1,
            selectedKey = navigationState.currentTopLevel,
            onSelect = {
                navigationState.switchTopLevel(it.key)
            }
        )

        Box(
            modifier = Modifier
                .background(SimAnalyzerTheme.material.background)
                .fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            AppNavGraph(
                navigationState = navigationState,
                providerFactory = providerFactory
            )
        }
    }
}
