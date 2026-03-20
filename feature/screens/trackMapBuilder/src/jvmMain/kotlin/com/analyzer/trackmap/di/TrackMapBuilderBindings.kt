package com.analyzer.trackmap.di

import androidx.lifecycle.ViewModel
import com.analyzer.trackmap.domain.TrackMapCaptureController
import com.analyzer.trackmap.presentation.TrackMapBuilderViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@Suppress("unused")
@ContributesTo(ScreenScope::class)
@BindingContainer
interface TrackMapBuilderBindings {

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(TrackMapBuilderViewModel::class)
        private fun provideTrackMapBuilderViewModel(controller: TrackMapCaptureController): ViewModel =
            TrackMapBuilderViewModel(controller = controller)
    }
}
