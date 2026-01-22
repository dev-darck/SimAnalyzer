package com.project.analyzer.hudSettings.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hudSettings.presentation.HudSettingsViewModel
import com.project.analyzer.impl.compose.HudPreferences
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
@ContributesTo(ScreenScope::class)
interface HudSettingsBindings {

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(HudSettingsViewModel::class)
        fun provideHudSettingsViewModel(
            preferences: HudPreferences,
            panels: Provider<Set<HudPanel>>,
        ): ViewModel = HudSettingsViewModel(
            preferences = preferences,
            panels = panels
        )
    }
}
