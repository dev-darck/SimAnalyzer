package com.project.analyzer.calibration.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.presentation.setup.CalibrationViewModel
import com.project.analyzer.calibration.presentation.trackmap.TrackMapBuilderViewModel
import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryViewModel
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface CalibrationBindings {

    @Binds
    @IntoMap
    @ViewModelKey(CalibrationViewModel::class)
    fun bindCalibrationViewModel(impl: CalibrationViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrackMapBuilderViewModel::class)
    fun bindTrackMapBuilderViewModel(impl: TrackMapBuilderViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(TrackMapLibraryViewModel::class)
    fun bindTrackMapLibraryViewModel(impl: TrackMapLibraryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CalibrationVerifyViewModel::class)
    fun bindCalibrationVerifyViewModel(impl: CalibrationVerifyViewModel): ViewModel
}
