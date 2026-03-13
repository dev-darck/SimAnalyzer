package com.project.analyzer.devsettings.domain.interactor

import com.project.analyzer.devsettings.domain.model.DevSettingsDomainState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface DevSettingsUseCase {

    val state: StateFlow<DevSettingsDomainState>
    fun start(scope: CoroutineScope)
    suspend fun setDevHudPanelEnabled(id: String, enabled: Boolean)
}
