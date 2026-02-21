package com.project.analyzer.live.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.live.domain.mapper.LiveScreenStateMapper
import com.project.analyzer.live.domain.usecase.LiveTelemetryUseCase
import com.project.analyzer.live.domain.usecase.LiveTelemetryUseCaseImpl
import com.project.analyzer.live.presentation.LiveViewModel
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
object LiveBindings {

    @Provides
    @SingleIn(ScreenScope::class)
    private fun provideLiveTelemetryUseCase(
        telemetryLifecycle: TelemetryLifecycle,
        liveScreenStateMapper: LiveScreenStateMapper,
    ): LiveTelemetryUseCase = LiveTelemetryUseCaseImpl(telemetryLifecycle, liveScreenStateMapper)

    @Provides
    @IntoMap
    @ViewModelKey(LiveViewModel::class)
    private fun provideLiveViewModel(liveTelemetryUseCase: LiveTelemetryUseCase): ViewModel =
        LiveViewModel(liveTelemetryUseCase)
}
