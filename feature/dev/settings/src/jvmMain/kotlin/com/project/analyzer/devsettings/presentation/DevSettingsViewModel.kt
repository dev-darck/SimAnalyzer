package com.project.analyzer.devsettings.presentation

import androidx.lifecycle.viewModelScope
import com.project.analyzer.devsettings.domain.interactor.DevSettingsUseCase
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Inject
internal class DevSettingsViewModel(private val useCase: DevSettingsUseCase) :
    LeakAwareMviViewModel<DevSettingsIntent, DevSettingsState>(DevSettingsState()) {

    init {
        useCase.start(viewModelScope)
        useCase.state
            .onEach(::setState)
            .launchIn(viewModelScope)
    }

    override suspend fun handleIntent(intent: DevSettingsIntent) {
        when (intent) {
            is DevSettingsIntent.ToggleHudPanel -> useCase.setDevHudPanelEnabled(intent.id, intent.enabled)
        }
    }
}
