package com.project.analyzer.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.window.WindowScope
import com.project.analyzer.app.frame.FrameDecoratorState
import com.project.analyzer.app.sidebar.NavItem
import com.project.analyzer.app.sidebar.Sidebar
import com.project.analyzer.app.titlebar.AppTitleBar
import com.project.analyzer.composeApp.Res.Res
import com.project.analyzer.composeApp.Res.Res.string
import com.project.analyzer.composeApp.Res.app_logo_content_description
import com.project.analyzer.composeApp.Res.app_nav_live
import com.project.analyzer.composeApp.Res.app_nav_session
import com.project.analyzer.composeApp.Res.app_nav_settings
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
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.trackRecompositions
import com.project.analyzer.ui.modifier.uiTestTag
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun WindowScope.App(
    providerFactory: EntryFactory,
    navigationState: NavigationState<Route>,
    decorator: FrameDecoratorState,
    onCloseRequest: () -> Unit = {},
) {
    AppContent(
        navigationState = navigationState,
        onCloseRequest = onCloseRequest,
        titleBar = { canGoBack, canGoForward, onBack, onForward, close ->
            AppTitleBar(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBack = onBack,
                onForward = onForward,
                onCloseRequest = close,
                appName = BuildConfig.APP_NAME,
                decorator = decorator,
            )
        },
        navigationContent = {
            AppNavGraph(
                navigationState = navigationState,
                providerFactory = providerFactory,
            )
        },
    )
}

@Composable
internal fun AppContent(
    navigationState: NavigationState<Route>,
    onCloseRequest: () -> Unit = {},
    titleBar: @Composable (
        canGoBack: Boolean,
        canGoForward: Boolean,
        onBack: () -> Unit,
        onForward: () -> Unit,
        onCloseRequest: () -> Unit,
    ) -> Unit = { _, _, _, _, _ -> },
    navigationContent: @Composable () -> Unit = {},
) {
    val items = persistentListOf(
        NavItem(Root.Live, stringResource(Res.string.app_nav_live), Icons.Filled.Live),
        NavItem(Root.Session, stringResource(Res.string.app_nav_session), Icons.Filled.Session),
//            NavItem(Root.Setup, "Setup", Icons.Outlined.Build),
        NavItem(Root.Settings, stringResource(string.app_nav_settings), Filled.Settings),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .uiTestTag(TestTags.App)
            .trackRecompositions(),
    ) {
        titleBar(
            navigationState.backStack.size > 1,
            navigationState.canGoForward,
            { navigationState.handleBack() },
            { navigationState.handleForward() },
            onCloseRequest,
        )

        Row(
            modifier = Modifier
                .background(SimAnalyzerTheme.material.background)
                .fillMaxSize(),
        ) {
            AnimatedVisibility(
                visible = navigationState.isCurrentRouteRoot,
            ) {
                Sidebar(
                    modifier = Modifier.clipToBounds(),
                    showTopIcon = true,
                    topIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.logo),
                            contentDescription = stringResource(Res.string.app_logo_content_description),
                        )
                    },
                    items = items,
                    bottomItemsCount = 1,
                    selectedKey = navigationState.currentTopLevel,
                    onSelect = {
                        navigationState.switchTopLevel(it.key)
                    },
                )
            }

            Box(
                modifier = Modifier
                    .background(SimAnalyzerTheme.material.background)
                    .weight(1f)
                    .fillMaxHeight()
                    .uiTestTag(TestTags.Content),
                contentAlignment = Alignment.TopStart,
            ) {
                navigationContent()
            }
        }
    }
}
