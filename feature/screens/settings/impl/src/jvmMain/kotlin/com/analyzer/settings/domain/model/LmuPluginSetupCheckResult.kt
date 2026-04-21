package com.analyzer.settings.domain.model

internal sealed interface LmuPluginSetupCheckResult {
    data class Ready(val details: LmuPluginSetupDetails) : LmuPluginSetupCheckResult
    data class InstallRequired(val details: LmuPluginSetupDetails) : LmuPluginSetupCheckResult
    data class GameNotFound(val details: LmuPluginSetupDetails, val message: String) : LmuPluginSetupCheckResult

    data class Error(val details: LmuPluginSetupDetails, val message: String) : LmuPluginSetupCheckResult
}
