package com.analyzer.settings.di

import androidx.lifecycle.ViewModel
import com.analyzer.settings.data.telemetry.SettingsRepository
import com.analyzer.settings.data.telemetry.SettingsRepositoryImpl
import com.analyzer.settings.data.theme.ThemeRepository
import com.analyzer.settings.presentation.SettingsViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
@ContributesTo(ScreenScope::class)
interface SettingsBindings {

    @Binds
    fun bindTelemetrySettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(SettingsViewModel::class)
        fun provideSettingsViewModel(
            themeRepository: ThemeRepository,
            telemetrySettingsRepository: SettingsRepository
        ): ViewModel = SettingsViewModel(themeRepository, telemetrySettingsRepository)
    }
}
