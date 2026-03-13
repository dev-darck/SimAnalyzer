package com.project.analyzer.inputs.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.data.repository.InputHudSettingsRepository
import com.project.analyzer.inputs.data.repository.InputsHudSettingsRepositoryImpl
import com.project.analyzer.inputs.domain.usecase.InputsUseCase
import com.project.analyzer.inputs.domain.usecase.InputsUseCaseImpl
import com.project.analyzer.inputs.presentation.InputsHudViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(HudScope::class)
@BindingContainer
interface InputsHudBindings {
    companion object {

        @Provides
        private fun provideInputsUseCase(impl: InputsUseCaseImpl): InputsUseCase = impl

        @Provides
        private fun provideInputHudSettingsRepository(
            impl: InputsHudSettingsRepositoryImpl,
        ): InputHudSettingsRepository = impl

        @Provides
        @IntoMap
        @ViewModelKey(InputsHudViewModel::class)
        private fun provideInputsHudViewModel(inputHudUseCase: InputsUseCase): ViewModel =
            InputsHudViewModel(inputHudUseCase)
    }
}
