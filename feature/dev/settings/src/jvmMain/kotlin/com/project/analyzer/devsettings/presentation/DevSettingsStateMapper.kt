package com.project.analyzer.devsettings.presentation

import com.project.analyzer.devsettings.domain.model.DevHudDomainState
import com.project.analyzer.devsettings.domain.model.DevHudPanelModel
import com.project.analyzer.devsettings.domain.model.DevSettingsDomainState
import com.project.analyzer.devsettings.domain.model.TelemetryEntryModel
import com.project.analyzer.devsettings.domain.model.TelemetryInspectorDomainState
import com.project.analyzer.devsettings.domain.model.TelemetryStatus
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toImmutableList

@Inject
internal class DevSettingsStateMapper {

    fun map(state: DevSettingsDomainState): DevSettingsState = DevSettingsState(
        telemetry = mapTelemetry(state.telemetry),
        hud = mapHud(state.hud),
    )

    private fun mapTelemetry(state: TelemetryInspectorDomainState): TelemetryInspectorState =
        TelemetryInspectorState(
            status = mapStatus(state.status),
            sessionType = state.sessionType,
            trackLabel = state.trackLabel,
            carLabel = state.carLabel,
            frameId = state.frameId,
            lastUpdatedLabel = state.lastUpdatedLabel,
            entries = state.entries.map(::mapEntry).toImmutableList(),
        )

    private fun mapHud(state: DevHudDomainState): DevHudState = DevHudState(
        hudEnabled = state.hudEnabled,
        panels = state.panels.map(::mapPanel).toImmutableList(),
    )

    private fun mapStatus(status: TelemetryStatus): TelemetryStatusUi = when (status) {
        TelemetryStatus.WaitingForTelemetry -> TelemetryStatusUi.WaitingForTelemetry
        TelemetryStatus.SimConnected -> TelemetryStatusUi.SimConnected
        TelemetryStatus.SimDisconnected -> TelemetryStatusUi.SimDisconnected
        TelemetryStatus.SessionStarted -> TelemetryStatusUi.SessionStarted
        TelemetryStatus.SessionUpdated -> TelemetryStatusUi.SessionUpdated
        TelemetryStatus.SessionPaused -> TelemetryStatusUi.SessionPaused
        TelemetryStatus.SessionResumed -> TelemetryStatusUi.SessionResumed
        TelemetryStatus.SessionEnded -> TelemetryStatusUi.SessionEnded
        TelemetryStatus.LapStarted -> TelemetryStatusUi.LapStarted
        TelemetryStatus.LapFinished -> TelemetryStatusUi.LapFinished
        is TelemetryStatus.Raw -> TelemetryStatusUi.Raw(status.value)
    }

    private fun mapEntry(entry: TelemetryEntryModel): TelemetryEntry = TelemetryEntry(
        path = entry.path,
        value = entry.value,
    )

    private fun mapPanel(panel: DevHudPanelModel): DevHudPanelUi = DevHudPanelUi(
        id = panel.id,
        enabled = panel.enabled,
    )
}
