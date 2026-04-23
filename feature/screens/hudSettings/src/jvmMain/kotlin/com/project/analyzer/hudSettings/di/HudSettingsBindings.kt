package com.project.analyzer.hudSettings.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCase
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCaseImpl
import com.project.analyzer.hudSettings.presentation.HudSettingsViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface HudSettingsBindings {

    companion object {

        @Provides
        private fun provideHudSettingsUseCase(
            hudPreferencesStore: HudPreferencesStore,
            panels: () -> Set<HudPanel>,
        ): HudSettingsUseCase = HudSettingsUseCaseImpl(
            hudPreferencesStore = hudPreferencesStore,
            panels = panels,
        )

        @Provides
        @IntoMap
        @ViewModelKey(HudSettingsViewModel::class)
        private fun provideHudSettingsViewModel(useCase: HudSettingsUseCase): ViewModel = HudSettingsViewModel(
            useCase = useCase,
        )
    }
}
