package com.project.analyzer.calibration.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.TelemetrySampleProviderImpl
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.presentation.setup.CalibrationViewModel
import com.project.analyzer.calibration.presentation.trackmap.TrackMapBuilderViewModel
import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryViewModel
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface CalibrationBindings {
    companion object {

        @Provides
        private fun provideTelemetrySampleProvider(impl: TelemetrySampleProviderImpl): TelemetrySampleProvider = impl

        @Provides
        @IntoMap
        @ViewModelKey(CalibrationViewModel::class)
        private fun provideCalibrationViewModel(impl: CalibrationViewModel): ViewModel = impl

        @Provides
        @IntoMap
        @ViewModelKey(TrackMapBuilderViewModel::class)
        private fun provideTrackMapBuilderViewModel(impl: TrackMapBuilderViewModel): ViewModel = impl

        @Provides
        @IntoMap
        @ViewModelKey(TrackMapLibraryViewModel::class)
        private fun provideTrackMapLibraryViewModel(impl: TrackMapLibraryViewModel): ViewModel = impl

        @Provides
        @IntoMap
        @ViewModelKey(CalibrationVerifyViewModel::class)
        private fun provideCalibrationVerifyViewModel(impl: CalibrationVerifyViewModel): ViewModel = impl
    }
}
