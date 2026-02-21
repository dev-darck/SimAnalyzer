package com.project.analyzer.inputs.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.domain.usecase.InputsUseCase
import com.project.analyzer.inputs.domain.usecase.InputsUseCaseImpl
import com.project.analyzer.inputs.presentation.InputsHudViewModel
import com.project.analyzer.inputs.settings.repository.InputHudSettingsRepository
import com.project.analyzer.inputs.settings.repository.InputsHudSettingsRepositoryImpl
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(HudScope::class)
@BindingContainer
interface InputsHudBindings {

    @Binds
    fun bindInputsUseCase(impl: InputsUseCaseImpl): InputsUseCase

    @Binds
    fun bindInputHudSettingsRepository(impl: InputsHudSettingsRepositoryImpl): InputHudSettingsRepository

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(InputsHudViewModel::class)
        private fun provideInputsHudViewModel(inputHudUseCase: InputsUseCase): ViewModel =
            InputsHudViewModel(inputHudUseCase)
    }
}
