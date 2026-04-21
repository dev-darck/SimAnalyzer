package com.analyzer.settings.domain.model

internal sealed interface LmuPluginInstallResult {
    data class Success(val details: LmuPluginSetupDetails) : LmuPluginInstallResult
    data class Failure(val details: LmuPluginSetupDetails, val message: String) : LmuPluginInstallResult
}
