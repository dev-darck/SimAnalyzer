package com.analyzer.settings.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.settings.presentation.components.AppearanceBlock
import com.analyzer.settings.presentation.components.DevSettingsBlock
import com.analyzer.settings.presentation.components.HudSetupBlock
import com.analyzer.settings.presentation.components.TelemetryAcquisitionBlock
import com.analyzer.settings.presentation.components.TelemetryGameSelectionBlock
import com.project.analyzer.chooser.FileChooserDialog
import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.rememberFileChooserState
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.settings.BuildConfig
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
internal fun SettingsScreen() {
    val viewModel = metroViewModel<SettingsViewModel>()
    val navigationState = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(state.hudEnabled) {
        navigationState.registerForwardValidator(Route.SettingsRoot.HudSettings::class) {
            state.hudEnabled
        }
        onDispose {
            navigationState.unregisterForwardValidator(Route.SettingsRoot.HudSettings::class)
        }
    }

    Screen(state, viewModel::dispatch) { route ->
        navigationState.navigate(route)
    }
}

@Composable
private fun Screen(
    state: SettingsState = SettingsState(),
    dispatch: (SettingsIntent) -> Unit = {},
    navigateTo: (Route) -> Unit = {},
) {
    val directoryChooserState = rememberFileChooserState(
        initialPath = state.storageLocation.ifBlank { null },
        selectionMode = SelectionMode.DIRECTORY,
        title = "Select storage location",
        onResult = { selectedPath ->
            selectedPath?.let { path ->
                dispatch(SettingsIntent.ChangeStorageLocation(path))
            }
        },
    )

    ResponsiveScreen(
        contentPadding = PaddingValues(horizontal = 16.dp),
        backgroundColor = SimAnalyzerTheme.material.background,
    ) {
        item("AppearanceBlock") {
            AppearanceBlock(
                modifier = Modifier.fillMaxHeight(),
                selectedTheme = state.themeMode,
                onThemeSelected = { mode ->
                    dispatch(SettingsIntent.ChangeTheme(mode))
                },
            )
        }
        item("HudSetupBlock") {
            HudSetupBlock(
                modifier = Modifier.fillMaxHeight(),
                isHudEnabled = state.hudEnabled,
                onHudEnabledChange = {
                    dispatch(SettingsIntent.ChangeHudEnabled(it))
                },
                onEditClick = {
                    navigateTo(Route.SettingsRoot.HudSettings)
                },
            )
        }
        item("TelemetryAcquisitionBlock") {
            TelemetryAcquisitionBlock(
                modifier = Modifier.fillMaxHeight(),
                samplingRateHz = state.samplingRateHz,
                storageLocation = state.storageLocation,
                storageLocationError = state.storageLocationError,
                storageSizeLabel = state.storageSizeLabel,
                recordingEnabled = state.recordingEnabled,
                recordingWarning = state.recordingWarning,
                maxRecordedLaps = state.maxRecordedLaps,
                onSamplingRateChange = {
                    dispatch(SettingsIntent.ChangeSamplingRate(it))
                },
                onStorageLocationChange = {
                    dispatch(SettingsIntent.ChangeStorageLocation(it))
                },
                onRecordingEnabledChange = {
                    dispatch(SettingsIntent.ChangeRecordingEnabled(it))
                },
                onMaxRecordedLapsChange = {
                    dispatch(SettingsIntent.ChangeMaxRecordedLaps(it))
                },
                onBrowseClick = {
                    directoryChooserState.show()
                },
            )
        }
        item("TelemetryGameSelectionBlock") {
            TelemetryGameSelectionBlock(
                modifier = Modifier.fillMaxHeight(),
                selectionUi = state.gameSelectionUi,
                onSelectionChange = { selection ->
                    dispatch(SettingsIntent.ChangeGameSelection(selection))
                },
            )
        }
        if (BuildConfig.IS_DEBUG) {
            item("DevSettingsBlock") {
                DevSettingsBlock(
                    modifier = Modifier.fillMaxHeight(),
                    onOpen = { navigateTo(Route.SettingsRoot.DevSettings) },
                )
            }
        }
    }

    FileChooserDialog(state = directoryChooserState)
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SimAnalyzerTheme {
        Screen()
    }
}
