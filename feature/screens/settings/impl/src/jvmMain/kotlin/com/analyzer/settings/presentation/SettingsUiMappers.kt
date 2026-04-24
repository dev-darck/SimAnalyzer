package com.analyzer.settings.presentation

import com.analyzer.settings.domain.model.LmuPluginInstallResult
import com.analyzer.settings.domain.model.LmuPluginInstallStep
import com.analyzer.settings.domain.model.LmuPluginSetupCheckResult
import com.analyzer.settings.domain.model.LmuPluginSetupDetails
import com.analyzer.settings.domain.model.StorageValidationResult

internal sealed interface LmuPluginSetupCheckUiResult {
    data object Ready : LmuPluginSetupCheckUiResult
    data class ShowDialog(val dialogState: LmuPluginDialogState) : LmuPluginSetupCheckUiResult
}

internal sealed interface LmuPluginInstallUiResult {
    data class Success(val dialogState: LmuPluginDialogState) : LmuPluginInstallUiResult
    data class Failure(val dialogState: LmuPluginDialogState) : LmuPluginInstallUiResult
}

internal fun LmuPluginSetupCheckResult.toUiResult(): LmuPluginSetupCheckUiResult = when (this) {
    is LmuPluginSetupCheckResult.Ready -> LmuPluginSetupCheckUiResult.Ready
    is LmuPluginSetupCheckResult.InstallRequired -> LmuPluginSetupCheckUiResult.ShowDialog(
        LmuPluginDialogState(
            phase = LmuPluginDialogPhase.Prompt,
            details = details.toUi(),
        ),
    )

    is LmuPluginSetupCheckResult.GameNotFound -> LmuPluginSetupCheckUiResult.ShowDialog(
        LmuPluginDialogState(
            phase = LmuPluginDialogPhase.MissingGame,
            details = details.toUi(),
            detailMessage = message,
        ),
    )

    is LmuPluginSetupCheckResult.Error -> LmuPluginSetupCheckUiResult.ShowDialog(
        LmuPluginDialogState(
            phase = LmuPluginDialogPhase.Failure,
            details = details.toUi(),
            detailMessage = message,
        ),
    )
}

internal fun LmuPluginInstallResult.toUiResult(): LmuPluginInstallUiResult = when (this) {
    is LmuPluginInstallResult.Success -> LmuPluginInstallUiResult.Success(
        LmuPluginDialogState(
            phase = LmuPluginDialogPhase.Success,
            details = details.toUi(),
        ),
    )

    is LmuPluginInstallResult.Failure -> LmuPluginInstallUiResult.Failure(
        LmuPluginDialogState(
            phase = LmuPluginDialogPhase.Failure,
            details = details.toUi(),
            detailMessage = message,
        ),
    )
}

internal fun LmuPluginSetupDetails.toUi(): LmuPluginDialogDetailsUi = LmuPluginDialogDetailsUi(
    latestVersion = latestVersion,
    repositoryUrl = repositoryUrl,
    downloadPageUrl = downloadPageUrl,
    archiveSha256 = archiveSha256,
    pluginSha256 = pluginSha256,
    gameInstallDir = gameInstallDir,
    pluginTargetPath = pluginTargetPath,
    configTargetPath = configTargetPath,
)

internal fun LmuPluginDialogDetailsUi.toDomain(): LmuPluginSetupDetails = LmuPluginSetupDetails(
    latestVersion = latestVersion,
    repositoryUrl = repositoryUrl,
    downloadPageUrl = downloadPageUrl,
    archiveSha256 = archiveSha256,
    pluginSha256 = pluginSha256,
    gameInstallDir = gameInstallDir,
    pluginTargetPath = pluginTargetPath,
    configTargetPath = configTargetPath,
)

internal fun LmuPluginInstallStep.toUi(): LmuPluginInstallStepUi = when (this) {
    LmuPluginInstallStep.ResolvingSource -> LmuPluginInstallStepUi.ResolvingSource
    LmuPluginInstallStep.DownloadingPackage -> LmuPluginInstallStepUi.DownloadingPackage
    LmuPluginInstallStep.ValidatingPackage -> LmuPluginInstallStepUi.ValidatingPackage
    LmuPluginInstallStep.ExtractingPlugin -> LmuPluginInstallStepUi.ExtractingPlugin
    LmuPluginInstallStep.WritingConfiguration -> LmuPluginInstallStepUi.WritingConfiguration
    LmuPluginInstallStep.Finalizing -> LmuPluginInstallStepUi.Finalizing
}

internal fun StorageValidationResult?.toUi(): StorageValidationUi? = when (this) {
    null, StorageValidationResult.Valid -> null
    StorageValidationResult.Empty -> StorageValidationUi.Empty
    StorageValidationResult.NotAbsolutePath -> StorageValidationUi.NotAbsolutePath
    StorageValidationResult.NotADirectory -> StorageValidationUi.NotADirectory
    StorageValidationResult.NotWritable -> StorageValidationUi.NotWritable
    StorageValidationResult.CannotCreate -> StorageValidationUi.CannotCreate
}
