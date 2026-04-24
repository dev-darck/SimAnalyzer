package com.analyzer.trackmap.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.collections.immutable.persistentListOf
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryHeaderUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryPointsPreviewUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryStatsUi
import com.analyzer.trackmap.presentation.model.TrackMapPreviewUi

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

@Preview
@Composable
private fun TrackMapLibraryScreenPreview() {
    SimAnalyzerTheme {
        TrackMapLibraryListContent(
            items = persistentListOf(
                TrackMapLibraryCardUi(
                    mapKey = "ace:sebring",
                    gameId = "ace",
                    trackId = "sebring",
                    layoutId = "gp",
                    header = TrackMapLibraryHeaderUi(
                        title = "Sebring International Raceway",
                        subtitle = "Assetto Corsa EVO / sebring",
                        layoutLabel = "GP",
                    ),
                    stats = TrackMapLibraryStatsUi(
                        pointCount = 182,
                        distanceMeters = 6019f,
                        averageTrackWidthMeters = 11.8f,
                        pitPointCount = 26,
                        createdAtLabel = "Today",
                    ),
                    preview = TrackMapPreviewUi(),
                    pointsPreview = TrackMapLibraryPointsPreviewUi(),
                ),
            ),
            onOpenEditor = {},
        )
    }
}
