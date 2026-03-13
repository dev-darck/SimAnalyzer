package com.analyzer.trackmap.di

import androidx.lifecycle.ViewModel
import com.analyzer.trackmap.domain.usecase.TrackMapLibraryUseCase
import com.analyzer.trackmap.presentation.mapper.TrackMapLibraryCardUiMapper
import com.analyzer.trackmap.presentation.TrackMapLibraryViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@Suppress("unused")
@ContributesTo(ScreenScope::class)
@BindingContainer
interface TrackMapLibraryBindings {

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(TrackMapLibraryViewModel::class)
        private fun provideTrackMapLibraryViewModel(
            useCase: TrackMapLibraryUseCase,
            cardUiMapper: TrackMapLibraryCardUiMapper,
        ): ViewModel = TrackMapLibraryViewModel(
            useCase = useCase,
            cardUiMapper = cardUiMapper,
        )
    }
}
