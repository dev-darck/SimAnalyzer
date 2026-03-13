package com.project.analyzer.devsettings.domain.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class DevSettingsDomainState(
    val telemetry: TelemetryInspectorDomainState = TelemetryInspectorDomainState(),
    val hud: DevHudDomainState = DevHudDomainState(),
)

sealed interface TelemetryStatus {
    data object WaitingForTelemetry : TelemetryStatus
    data object SimConnected : TelemetryStatus
    data object SimDisconnected : TelemetryStatus
    data object SessionStarted : TelemetryStatus
    data object SessionUpdated : TelemetryStatus
    data object SessionPaused : TelemetryStatus
    data object SessionResumed : TelemetryStatus
    data object SessionEnded : TelemetryStatus
    data object LapStarted : TelemetryStatus
    data object LapFinished : TelemetryStatus
    data class Raw(val value: String) : TelemetryStatus
}

data class TelemetryInspectorDomainState(
    val status: TelemetryStatus = TelemetryStatus.WaitingForTelemetry,
    val sessionType: String = "-",
    val trackLabel: String = "-",
    val carLabel: String = "-",
    val frameId: Long? = null,
    val lastUpdatedLabel: String = "-",
    val entries: ImmutableList<TelemetryEntryModel> = persistentListOf(),
)

data class TelemetryEntryModel(val path: String, val value: String)

data class DevHudDomainState(
    val hudEnabled: Boolean = true,
    val panels: ImmutableList<DevHudPanelModel> = persistentListOf(),
)

data class DevHudPanelModel(val id: String, val enabled: Boolean)
