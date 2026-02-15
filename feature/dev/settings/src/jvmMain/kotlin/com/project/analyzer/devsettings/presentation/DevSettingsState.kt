package com.project.analyzer.devsettings.presentation

internal data class DevSettingsState(
    val telemetry: TelemetryInspectorState = TelemetryInspectorState(),
    val hud: DevHudState = DevHudState(),
)

internal data class TelemetryInspectorState(
    val status: String = "Waiting for telemetry",
    val sessionType: String = "-",
    val trackLabel: String = "-",
    val carLabel: String = "-",
    val frameId: Long? = null,
    val lastUpdatedLabel: String = "-",
    val entries: List<TelemetryEntry> = emptyList(),
)

internal data class TelemetryEntry(
    val path: String,
    val value: String,
)

internal data class DevHudState(
    val hudEnabled: Boolean = true,
    val panels: List<DevHudPanelUi> = emptyList(),
)

internal data class DevHudPanelUi(
    val id: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
)
