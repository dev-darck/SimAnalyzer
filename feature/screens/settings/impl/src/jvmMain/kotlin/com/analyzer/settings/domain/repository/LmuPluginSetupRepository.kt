package com.analyzer.settings.domain.repository

import com.analyzer.settings.domain.model.LmuPluginInstallResult
import com.analyzer.settings.domain.model.LmuPluginInstallStep
import com.analyzer.settings.domain.model.LmuPluginSetupCheckResult
import com.analyzer.settings.domain.model.LmuPluginSetupDetails

internal interface LmuPluginSetupRepository {
    suspend fun inspectSetup(): LmuPluginSetupCheckResult

    suspend fun installLatestPlugin(
        details: LmuPluginSetupDetails,
        onProgress: (LmuPluginInstallStep) -> Unit = {},
    ): LmuPluginInstallResult
}
