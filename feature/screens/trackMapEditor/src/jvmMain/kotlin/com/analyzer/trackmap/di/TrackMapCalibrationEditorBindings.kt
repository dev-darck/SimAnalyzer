package com.analyzer.trackmap.di

import androidx.lifecycle.ViewModel
import com.analyzer.trackmap.domain.usecase.editor.TrackMapCalibrationEditorReducer
import com.analyzer.trackmap.domain.usecase.editor.TrackMapCalibrationEditorUseCase
import com.analyzer.trackmap.domain.usecase.live.ObserveTrackMapLivePositionUseCase
import com.analyzer.trackmap.presentation.TrackMapCalibrationEditorViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@Suppress("unused")
@ContributesTo(ScreenScope::class)
@BindingContainer
interface TrackMapCalibrationEditorBindings {

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(TrackMapCalibrationEditorViewModel::class)
        private fun provideTrackMapCalibrationEditorViewModel(
            useCase: TrackMapCalibrationEditorUseCase,
            reducer: TrackMapCalibrationEditorReducer,
            observeLivePositionUseCase: ObserveTrackMapLivePositionUseCase,
        ): ViewModel = TrackMapCalibrationEditorViewModel(
            useCase = useCase,
            reducer = reducer,
            observeLivePositionUseCase = observeLivePositionUseCase,
        )
    }
}
