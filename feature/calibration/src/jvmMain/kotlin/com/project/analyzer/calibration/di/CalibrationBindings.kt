package com.project.analyzer.calibration.di

import com.project.analyzer.calibration.data.AcTelemetrySampleProvider
import com.project.analyzer.calibration.data.FileTrackCalibrationRepository
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.TrackCalibrationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo

@ContributesTo(AppScope::class)
@BindingContainer
abstract class CalibrationBindings {

    @Binds
    abstract fun provideTelemetrySampleProvider(impl: AcTelemetrySampleProvider): TelemetrySampleProvider

    @Binds
    abstract fun provideTrackCalibrationRepository(impl: FileTrackCalibrationRepository): TrackCalibrationRepository
}
