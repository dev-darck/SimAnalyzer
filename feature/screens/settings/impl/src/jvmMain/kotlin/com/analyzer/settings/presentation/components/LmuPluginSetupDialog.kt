@file:Suppress("NoWildcardImports", "WildcardImport")

package com.analyzer.settings.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.analyzer.settings.presentation.LmuPluginDialogPhase
import com.analyzer.settings.presentation.LmuPluginDialogState
import com.analyzer.settings.presentation.LmuPluginInstallStepUi
import com.project.analyzer.feature.screens.settings.impl.Res.*
import com.project.analyzer.feature.screens.settings.impl.Res.Res
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.InfoDialog
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LmuPluginSetupDialog(state: LmuPluginDialogState, onDismiss: () -> Unit, onConfirmInstall: () -> Unit) {
    val dismissible = state.phase != LmuPluginDialogPhase.Installing
    val dismissButtonText = when (state.phase) {
        LmuPluginDialogPhase.Prompt -> stringResource(Res.string.lmu_plugin_dialog_button_cancel)

        LmuPluginDialogPhase.Success -> stringResource(Res.string.lmu_plugin_dialog_button_done)

        LmuPluginDialogPhase.Failure, LmuPluginDialogPhase.MissingGame ->
            stringResource(Res.string.lmu_plugin_dialog_button_close)

        LmuPluginDialogPhase.Installing -> ""
    }
    val confirmButtonText = when (state.phase) {
        LmuPluginDialogPhase.Prompt -> stringResource(Res.string.lmu_plugin_dialog_button_install)
        LmuPluginDialogPhase.Failure -> stringResource(Res.string.lmu_plugin_dialog_button_retry)
        else -> ""
    }

    InfoDialog(
        title = state.title(),
        message = state.message(),
        modifier = Modifier.widthIn(max = 620.dp),
        onDismissRequest = {
            if (dismissible) onDismiss()
        },
        dismissButtonText = dismissButtonText,
        onDismissClick = if (dismissible) onDismiss else null,
        confirmButtonText = confirmButtonText,
        onConfirmClick = if (confirmButtonText.isNotBlank()) onConfirmInstall else null,
        dismissButtonWidth = 176.dp,
        confirmButtonWidth = 176.dp,
        buttonHeight = 32.dp,
        buttonSize = SimAnalyzerButtonSize.Compact,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.phase == LmuPluginDialogPhase.Installing) {
                InstallProgressRow(state = state)
            }

            DetailSection(title = stringResource(Res.string.lmu_plugin_dialog_section_source)) {
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_source_version),
                    value = state.details.latestVersion,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_source_repository),
                    value = state.details.repositoryUrl,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_source_download_page),
                    value = state.details.downloadPageUrl,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_source_archive_sha256),
                    value = state.details.archiveSha256,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_source_plugin_sha256),
                    value = state.details.pluginSha256,
                )
            }

            DetailSection(title = stringResource(Res.string.lmu_plugin_dialog_section_destination)) {
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_destination_game),
                    value = state.details.gameInstallDir,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_destination_plugin),
                    value = state.details.pluginTargetPath,
                )
                LabeledValue(
                    label = stringResource(Res.string.lmu_plugin_dialog_destination_config),
                    value = state.details.configTargetPath,
                )
            }

            if (state.phase == LmuPluginDialogPhase.Prompt || state.phase == LmuPluginDialogPhase.Installing) {
                DetailSection(title = stringResource(Res.string.lmu_plugin_dialog_section_actions)) {
                    ActionLine(text = stringResource(Res.string.lmu_plugin_dialog_action_resolve))
                    ActionLine(text = stringResource(Res.string.lmu_plugin_dialog_action_download))
                    ActionLine(text = stringResource(Res.string.lmu_plugin_dialog_action_configure))
                    ActionLine(text = stringResource(Res.string.lmu_plugin_dialog_action_safe))
                }
            }

            if (!state.detailMessage.isNullOrBlank()) {
                DetailSection(title = stringResource(Res.string.lmu_plugin_dialog_section_status)) {
                    Text(
                        text = state.detailMessage,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallProgressRow(state: LmuPluginDialogState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = state.progressLabel(),
            style = SimAnalyzerTheme.typography.bodyMedium,
            color = SimAnalyzerTheme.material.onSurface,
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelLarge,
        )
        content()
    }
}

@Composable
private fun LabeledValue(label: String, value: String?) {
    if (value.isNullOrBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
        SelectionContainer {
            Text(
                text = value,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ActionLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "-",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
        Text(
            text = text,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LmuPluginDialogState.title(): String = when (phase) {
    LmuPluginDialogPhase.Prompt -> stringResource(Res.string.lmu_plugin_dialog_title_prompt)
    LmuPluginDialogPhase.Installing -> stringResource(Res.string.lmu_plugin_dialog_title_installing)
    LmuPluginDialogPhase.Success -> stringResource(Res.string.lmu_plugin_dialog_title_success)
    LmuPluginDialogPhase.Failure -> stringResource(Res.string.lmu_plugin_dialog_title_failure)
    LmuPluginDialogPhase.MissingGame -> stringResource(Res.string.lmu_plugin_dialog_title_missing_game)
}

@Composable
private fun LmuPluginDialogState.message(): String = when (phase) {
    LmuPluginDialogPhase.Prompt -> stringResource(Res.string.lmu_plugin_dialog_message_prompt)
    LmuPluginDialogPhase.Installing -> stringResource(Res.string.lmu_plugin_dialog_message_installing)
    LmuPluginDialogPhase.Success -> stringResource(Res.string.lmu_plugin_dialog_message_success)
    LmuPluginDialogPhase.Failure -> stringResource(Res.string.lmu_plugin_dialog_message_failure)
    LmuPluginDialogPhase.MissingGame -> stringResource(Res.string.lmu_plugin_dialog_message_missing_game)
}

@Composable
private fun LmuPluginDialogState.progressLabel(): String = when (progressStep) {
    null -> stringResource(Res.string.lmu_plugin_dialog_progress_resolving)

    LmuPluginInstallStepUi.ResolvingSource ->
        stringResource(Res.string.lmu_plugin_dialog_progress_resolving)

    LmuPluginInstallStepUi.DownloadingPackage ->
        stringResource(Res.string.lmu_plugin_dialog_progress_downloading)

    LmuPluginInstallStepUi.ValidatingPackage ->
        stringResource(Res.string.lmu_plugin_dialog_progress_validating)

    LmuPluginInstallStepUi.ExtractingPlugin ->
        stringResource(Res.string.lmu_plugin_dialog_progress_extracting)

    LmuPluginInstallStepUi.WritingConfiguration ->
        stringResource(Res.string.lmu_plugin_dialog_progress_configuring)

    LmuPluginInstallStepUi.Finalizing ->
        stringResource(Res.string.lmu_plugin_dialog_progress_finalizing)
}
