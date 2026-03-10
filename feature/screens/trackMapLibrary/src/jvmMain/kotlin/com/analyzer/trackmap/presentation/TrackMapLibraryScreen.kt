package com.analyzer.trackmap.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun TrackMapLibraryScreen() {
    val viewModel = metroViewModel<TrackMapLibraryViewModel>()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current

    TrackMapLibraryListContent(
        items = items,
        onOpenEditor = { card ->
            viewModel.rememberSelection(card.mapKey)
            navigator.navigate(
                Route.SettingsRoot.TrackMapCalibrationEditor(
                    gameId = card.gameId,
                    trackId = card.trackId,
                    layoutId = card.layoutId,
                ),
            )
        },
    )
}
