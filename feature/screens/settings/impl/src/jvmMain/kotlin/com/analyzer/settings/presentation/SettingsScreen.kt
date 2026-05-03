@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.settings.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.settings.presentation.components.AppearanceBlock
import com.analyzer.settings.presentation.components.CloseBehaviorBlock
import com.analyzer.settings.presentation.components.DevSettingsBlock
import com.analyzer.settings.presentation.components.HudSetupBlock
import com.analyzer.settings.presentation.components.LmuPluginSetupDialog
import com.analyzer.settings.presentation.components.TelemetryAcquisitionBlock
import com.analyzer.settings.presentation.components.TelemetryGameSelectionBlock
import com.project.analyzer.chooser.FileChooserDialog
import com.project.analyzer.chooser.SelectionMode
import com.project.analyzer.chooser.rememberFileChooserState
import com.project.analyzer.feature.screens.settings.impl.Res.Res
import com.project.analyzer.feature.screens.settings.impl.Res.settings_select_storage_location_title
import com.project.analyzer.feature.screens.settings.impl.Res.telemetry_recording_notice_message
import com.project.analyzer.feature.screens.settings.impl.Res.telemetry_recording_notice_title
import com.project.analyzer.navigation.api.LocalNavigator
import com.project.analyzer.navigation.api.Route
import com.project.analyzer.settings.BuildConfig
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import com.project.analyzer.ui.components.InfoBarSeverity
import com.project.analyzer.ui.components.InfoBarSnackbarHost
import com.project.analyzer.ui.components.InfoBarSnackbarVisuals
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsScreen() {
    val viewModel = metroViewModel<SettingsViewModel>()
    val navigationState = LocalNavigator.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    DisposableEffect(state.hudEnabled) {
        navigationState.registerForwardValidator(Route.SettingsRoot.HudSettings::class) { _: Route ->
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
    val recordingNoticeHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val recordingNoticeTitle = stringResource(Res.string.telemetry_recording_notice_title)
    val recordingNoticeMessage = stringResource(Res.string.telemetry_recording_notice_message)

    LaunchedEffect(state.showRecordingEnabledNotice, recordingNoticeTitle, recordingNoticeMessage) {
        if (!state.showRecordingEnabledNotice) return@LaunchedEffect
        dispatch(SettingsIntent.DismissRecordingEnabledNotice)
        snackbarScope.launch {
            recordingNoticeHostState.currentSnackbarData?.dismiss()
            recordingNoticeHostState.showSnackbar(
                visuals = InfoBarSnackbarVisuals(
                    title = recordingNoticeTitle,
                    message = recordingNoticeMessage,
                    severity = InfoBarSeverity.Warning,
                    duration = SnackbarDuration.Long,
                    withDismissAction = true,
                ),
            )
        }
    }

    val directoryChooserState = rememberFileChooserState(
        initialPath = state.storageLocation.ifBlank { null },
        selectionMode = SelectionMode.DIRECTORY,
        title = stringResource(Res.string.settings_select_storage_location_title),
        onResult = { selectedPath ->
            selectedPath?.let { path ->
                dispatch(SettingsIntent.ChangeStorageLocation(path))
            }
        },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ResponsiveScreen(
            contentPadding = PaddingValues(horizontal = 16.dp),
            mediumMinCellSize = 360.dp,
            expandedMinCellSize = 420.dp,
            backgroundColor = SimAnalyzerTheme.material.background,
        ) {
            item(key = "AppearanceBlock", contentType = "settings:choice") {
                AppearanceBlock(
                    selectedTheme = state.themeMode,
                    onThemeSelected = { mode ->
                        dispatch(SettingsIntent.ChangeTheme(mode))
                    },
                )
            }
            item(key = "CloseBehaviorBlock", contentType = "settings:choice") {
                CloseBehaviorBlock(
                    selectedBehavior = state.appCloseBehavior,
                    onBehaviorSelected = { behavior ->
                        dispatch(SettingsIntent.ChangeAppCloseBehavior(behavior))
                    },
                )
            }
            item(key = "HudSetupBlock", contentType = "settings:setup") {
                HudSetupBlock(
                    isHudEnabled = state.hudEnabled,
                    onHudEnabledChange = {
                        dispatch(SettingsIntent.ChangeHudEnabled(it))
                    },
                    onEditClick = {
                        navigateTo(Route.SettingsRoot.HudSettings)
                    },
                )
            }
            if (BuildConfig.IS_DEBUG) {
                item(key = "DevSettingsBlock", contentType = "settings:link") {
                    DevSettingsBlock(
                        onOpen = { navigateTo(Route.SettingsRoot.DevSettings) },
                    )
                }
                item(key = "TelemetryGameSelectionBlock", contentType = "settings:choice") {
                    TelemetryGameSelectionBlock(
                        selectionUi = state.gameSelectionUi,
                        onSelectionChange = { selection ->
                            dispatch(SettingsIntent.ChangeGameSelection(selection))
                        },
                    )
                }
            } else {
                item(key = "TelemetryGameSelectionBlock", contentType = "settings:choice") {
                    TelemetryGameSelectionBlock(
                        selectionUi = state.gameSelectionUi,
                        onSelectionChange = { selection ->
                            dispatch(SettingsIntent.ChangeGameSelection(selection))
                        },
                    )
                }
            }
            item(key = "TelemetryAcquisitionBlock", contentType = "settings:form") {
                TelemetryAcquisitionBlock(
                    samplingRateHz = state.samplingRateHz,
                    storageLocation = state.storageLocation,
                    storageLocationError = state.storageLocationError,
                    storageSizeInfo = state.storageSizeInfo,
                    recordingEnabled = state.recordingEnabled,
                    recordingWarning = state.recordingWarning,
                    maxRecordedLaps = state.maxRecordedLaps,
                    onSamplingRateChange = {
                        dispatch(SettingsIntent.ChangeSamplingRate(it))
                    },
                    onStorageLocationInputChange = {
                        dispatch(SettingsIntent.ChangeStorageLocationInput(it))
                    },
                    onStorageLocationCommit = {
                        dispatch(SettingsIntent.CommitStorageLocationInput)
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
        }

        InfoBarSnackbarHost(
            hostState = recordingNoticeHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .widthIn(max = 560.dp),
        )
    }

    FileChooserDialog(state = directoryChooserState)
    state.lmuPluginDialog?.let { dialogState ->
        LmuPluginSetupDialog(
            state = dialogState,
            onDismiss = { dispatch(SettingsIntent.DismissLmuPluginDialog) },
            onConfirmInstall = { dispatch(SettingsIntent.ConfirmLmuPluginInstall) },
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SimAnalyzerTheme {
        Screen()
    }
}
