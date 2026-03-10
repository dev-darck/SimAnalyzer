package com.project.analyzer.calibration.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.TelemetrySampleProviderImpl
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.usecase.BuildGateUseCase
import com.project.analyzer.calibration.domain.usecase.BuildGateUseCaseImpl
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCaseImpl
import com.project.analyzer.calibration.domain.usecase.LoadTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.LoadTrackCalibrationUseCaseImpl
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCaseImpl
import com.project.analyzer.calibration.presentation.setup.CalibrationViewModel
import com.project.analyzer.calibration.presentation.verify.CalibrationVerifyViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
@Suppress("unused")
interface CalibrationBindings {

    companion object {

        @Provides
        private fun provideTelemetrySampleProvider(impl: TelemetrySampleProviderImpl): TelemetrySampleProvider = impl

        @Provides
        private fun provideBuildGateUseCase(impl: BuildGateUseCaseImpl): BuildGateUseCase = impl

        @Provides
        private fun provideCaptureGateOnStandstillUseCase(
            impl: CaptureGateOnStandstillUseCaseImpl,
        ): CaptureGateOnStandstillUseCase = impl

        @Provides
        private fun provideLoadTrackCalibrationUseCase(
            impl: LoadTrackCalibrationUseCaseImpl,
        ): LoadTrackCalibrationUseCase = impl

        @Provides
        private fun provideSaveTrackCalibrationUseCase(
            impl: SaveTrackCalibrationUseCaseImpl,
        ): SaveTrackCalibrationUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(CalibrationViewModel::class)
        private fun provideCalibrationViewModel(impl: CalibrationViewModel): ViewModel = impl

        @Provides
        @IntoMap
        @ViewModelKey(CalibrationVerifyViewModel::class)
        private fun provideCalibrationVerifyViewModel(impl: CalibrationVerifyViewModel): ViewModel = impl
    }
}
