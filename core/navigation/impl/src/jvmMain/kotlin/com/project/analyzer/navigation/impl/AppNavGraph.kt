package com.project.analyzer.navigation.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.project.analyzer.navigation.api.EntryFactory
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.NavigationState
import com.project.analyzer.navigation.api.Route

@Composable
fun AppNavGraph(
    navigationState: NavigationState<Route>,
    providerFactory: EntryFactory,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalNavigator provides navigationState) {
        NavDisplay(
            modifier = modifier,
            backStack = navigationState.backStack,
            onBack = { navigationState.handleBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = providerFactory.create(),
            transitionSpec = { sharedAxisZForward()(this) },
            popTransitionSpec = { sharedAxisZBackward()(this) },
        )
    }
}
