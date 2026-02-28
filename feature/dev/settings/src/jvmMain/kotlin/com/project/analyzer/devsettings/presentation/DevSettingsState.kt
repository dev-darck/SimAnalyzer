package com.project.analyzer.devsettings.presentation

data class DevSettingsState(
    val telemetry: TelemetryInspectorState = TelemetryInspectorState(),
    val hud: DevHudState = DevHudState(),
)

sealed interface TelemetryStatusUi {
    data object WaitingForTelemetry : TelemetryStatusUi
    data object SimConnected : TelemetryStatusUi
    data object SimDisconnected : TelemetryStatusUi
    data object SessionStarted : TelemetryStatusUi
    data object SessionUpdated : TelemetryStatusUi
    data object SessionPaused : TelemetryStatusUi
    data object SessionResumed : TelemetryStatusUi
    data object SessionEnded : TelemetryStatusUi
    data object LapStarted : TelemetryStatusUi
    data object LapFinished : TelemetryStatusUi
    data class Raw(val value: String) : TelemetryStatusUi
}

data class TelemetryInspectorState(
    val status: TelemetryStatusUi = TelemetryStatusUi.WaitingForTelemetry,
    val sessionType: String = "-",
    val trackLabel: String = "-",
    val carLabel: String = "-",
    val frameId: Long? = null,
    val lastUpdatedLabel: String = "-",
    val entries: List<TelemetryEntry> = emptyList(),
)

data class TelemetryEntry(val path: String, val value: String)

data class DevHudState(val hudEnabled: Boolean = true, val panels: List<DevHudPanelUi> = emptyList())

data class DevHudPanelUi(val id: String, val enabled: Boolean)
