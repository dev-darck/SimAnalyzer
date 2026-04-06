package com.analyzer.trackmap.di

import com.analyzer.trackmap.presentation.TrackMapCalibrationEditorScreen
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.NavigationEntryBuilder
import com.project.analyzer.navigation.api.NavigationScope
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.navigation.api.RouteEntryBuilder
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(NavigationScope::class)
@SingleIn(NavigationScope::class)
class TrackMapCalibrationEditorEntry : RouteEntryBuilder {

    override fun NavigationEntryBuilder.build() {
        entry(Route.SettingsRoot.TrackMapCalibrationEditor::class) {
            val navigator = LocalNavigator.current
            TrackMapCalibrationEditorScreen(
                gameId = gameId,
                trackId = trackId,
                layoutId = layoutId,
                onBack = navigator::handleBack,
            )
        }
    }
}
