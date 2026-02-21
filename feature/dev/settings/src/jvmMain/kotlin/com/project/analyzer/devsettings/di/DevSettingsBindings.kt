package com.project.analyzer.devsettings.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.devsettings.presentation.DevSettingsViewModel
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.impl.compose.HudPreferences
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
object DevSettingsBindings {

    @Provides
    @IntoMap
    @ViewModelKey(DevSettingsViewModel::class)
    private fun provideDevSettingsViewModel(
        telemetryLifecycle: TelemetryLifecycle,
        hudPreferences: HudPreferences,
        panels: Provider<Set<HudPanel>>,
    ): ViewModel = DevSettingsViewModel(
        telemetryLifecycle = telemetryLifecycle,
        hudPreferences = hudPreferences,
        panels = panels,
    )
}
