package com.project.analyzer.devsettings.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.devsettings.domain.interactor.DevSettingsUseCase
import com.project.analyzer.devsettings.domain.interactor.DevSettingsUseCaseImpl
import com.project.analyzer.devsettings.presentation.DevSettingsStateMapper
import com.project.analyzer.devsettings.presentation.DevSettingsViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface DevSettingsBindings {
    companion object {

        @Provides
        private fun provideDevSettingsUseCase(impl: DevSettingsUseCaseImpl): DevSettingsUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(DevSettingsViewModel::class)
        private fun provideDevSettingsViewModel(
            interactor: DevSettingsUseCase,
            stateMapper: DevSettingsStateMapper,
        ): ViewModel = DevSettingsViewModel(interactor, stateMapper)
    }
}
