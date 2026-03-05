package com.project.analyzer.hudSettings.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCase
import com.project.analyzer.hudSettings.domain.interactor.HudSettingsUseCaseImpl
import com.project.analyzer.hudSettings.presentation.HudSettingsViewModel
import com.project.analyzer.impl.compose.HudPreferencesRepository
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface HudSettingsBindings {

    companion object {

        @Provides
        private fun provideHudSettingsUseCase(
            hudRepository: HudPreferencesRepository,
            panels: Provider<Set<HudPanel>>
        ): HudSettingsUseCase =
            HudSettingsUseCaseImpl(
                hudRepository = hudRepository,
                panels = panels
            )

        @Provides
        @IntoMap
        @ViewModelKey(HudSettingsViewModel::class)
        private fun provideHudSettingsViewModel(useCase: HudSettingsUseCase): ViewModel =
            HudSettingsViewModel(
                useCase = useCase,
            )
    }
}
