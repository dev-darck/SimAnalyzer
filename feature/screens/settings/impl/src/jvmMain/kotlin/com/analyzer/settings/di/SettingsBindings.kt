package com.analyzer.settings.di

import androidx.lifecycle.ViewModel
import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl
import com.analyzer.settings.domain.usecase.SettingsUseCase
import com.analyzer.settings.domain.usecase.SettingsUseCaseImpl
import com.analyzer.settings.presentation.SettingsViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface SettingsBindings {
    companion object {

        @Provides
        private fun provideTelemetrySettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository = impl

        @Provides
        private fun provideSettingsUseCase(impl: SettingsUseCaseImpl): SettingsUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(SettingsViewModel::class)
        private fun provideSettingsViewModel(interactor: SettingsUseCase): ViewModel = SettingsViewModel(
            useCase = interactor,
        )
    }
}
