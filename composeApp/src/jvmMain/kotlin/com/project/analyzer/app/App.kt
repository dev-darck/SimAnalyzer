package com.project.analyzer.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.WindowScope
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.logo
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Root
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.impl.AppNavGraph
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.icons.Live
import com.project.analyzer.ui.icons.Session
import com.project.analyzer.ui.icons.Settings
import org.jetbrains.compose.resources.painterResource

@Composable
fun WindowScope.App(
    providerFactory: EntryFactory,
    navigationState: NavigationState<Route>,
    decorator: FrameDecoratorState,
    onCloseRequest: () -> Unit = {}
) {
    val items = remember {
        listOf(
            NavItem(Root.Live, "Live", Icons.Filled.Live),
            NavItem(Root.Session, "Session", Icons.Filled.Session),
//            NavItem(Root.Setup, "Setup", Icons.Outlined.Build),
            NavItem(Root.Settings, "Settings", Icons.Filled.Settings)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
    ) {
        AppTitleBar(
            canGoBack = navigationState.backStack.size > 1,
            canGoForward = navigationState.canGoForward,
            onBack = { navigationState.handleBack() },
            onForward = { navigationState.handleForward() },
            onCloseRequest = onCloseRequest,
            appName = BuildConfig.APP_NAME,
            decorator = decorator
        )

        Row(
            modifier = Modifier
                .background(SimAnalyzerTheme.material.background)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = navigationState.isCurrentRouteRoot
            ) {
                Sidebar(
                    modifier = Modifier.clipToBounds(),
                    topIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.logo),
                            contentDescription = "App logo"
                        )
                    },
                    items = items,
                    bottomItemsCount = 1,
                    selectedKey = navigationState.currentTopLevel,
                    onSelect = {
                        navigationState.switchTopLevel(it.key)
                    }
                )
            }

            Box(
                modifier = Modifier
                    .background(SimAnalyzerTheme.material.background)
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopStart
            ) {
                AppNavGraph(
                    navigationState = navigationState,
                    providerFactory = providerFactory
                )
            }
        }
    }
}
