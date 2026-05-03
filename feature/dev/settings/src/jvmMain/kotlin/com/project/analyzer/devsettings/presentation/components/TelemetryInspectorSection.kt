package com.project.analyzer.devsettings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.project.analyzer.devsettings.presentation.TelemetryEntry
import com.project.analyzer.devsettings.presentation.TelemetryInspectorState
import com.project.analyzer.devsettings.presentation.TelemetryStatusUi
import com.project.analyzer.feature.dev.settings.Res.Res
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_lap_finished
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_lap_started
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_session_ended
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_session_paused
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_session_resumed
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_session_started
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_session_updated
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_sim_connected
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_sim_disconnected
import com.project.analyzer.feature.dev.settings.Res.dev_settings_status_waiting_for_telemetry
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_car
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_fields
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_frame
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_session
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_status
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_track
import com.project.analyzer.feature.dev.settings.Res.dev_settings_telemetry_updated
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TelemetryInspectorScreen(state: TelemetryInspectorState, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        TelemetryInspectorHeader(state)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.entries, key = { it.path }) { entry ->
                    TelemetryEntryRow(entry = entry)
                }
            }
            AppVerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                adapter = AppScrollbarAdapter(rememberScrollbarAdapter(listState)),
            )
        }
    }
}

@Composable
private fun TelemetryInspectorHeader(state: TelemetryInspectorState) {
    Column(
        modifier = Modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_status, state.status.asText()),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_session, state.sessionType),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_track, state.trackLabel),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = stringResource(Res.string.dev_settings_telemetry_car, state.carLabel),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_frame, state.frameId?.toString() ?: "-"),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_fields, state.entries.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.dev_settings_telemetry_updated, state.lastUpdatedLabel),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TelemetryEntryRow(entry: TelemetryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.corners.item)
            .background(SimAnalyzerTheme.material.surface.copy(alpha = 0.85f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = entry.path,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(0.6f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = entry.value,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(0.4f),
        )
    }
}

@Composable
private fun TelemetryStatusUi.asText(): String = when (this) {
    TelemetryStatusUi.WaitingForTelemetry -> stringResource(Res.string.dev_settings_status_waiting_for_telemetry)
    TelemetryStatusUi.SimConnected -> stringResource(Res.string.dev_settings_status_sim_connected)
    TelemetryStatusUi.SimDisconnected -> stringResource(Res.string.dev_settings_status_sim_disconnected)
    TelemetryStatusUi.SessionStarted -> stringResource(Res.string.dev_settings_status_session_started)
    TelemetryStatusUi.SessionUpdated -> stringResource(Res.string.dev_settings_status_session_updated)
    TelemetryStatusUi.SessionPaused -> stringResource(Res.string.dev_settings_status_session_paused)
    TelemetryStatusUi.SessionResumed -> stringResource(Res.string.dev_settings_status_session_resumed)
    TelemetryStatusUi.SessionEnded -> stringResource(Res.string.dev_settings_status_session_ended)
    TelemetryStatusUi.LapStarted -> stringResource(Res.string.dev_settings_status_lap_started)
    TelemetryStatusUi.LapFinished -> stringResource(Res.string.dev_settings_status_lap_finished)
    is TelemetryStatusUi.Raw -> value
}
