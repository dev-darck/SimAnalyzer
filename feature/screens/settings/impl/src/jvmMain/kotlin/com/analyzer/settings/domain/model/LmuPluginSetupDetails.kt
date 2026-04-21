package com.analyzer.settings.domain.model

internal data class LmuPluginSetupDetails(
    val latestVersion: String? = null,
    val repositoryUrl: String = LMU_PLUGIN_REPOSITORY_URL,
    val downloadPageUrl: String? = null,
    val archiveSha256: String? = null,
    val pluginSha256: String? = null,
    val gameInstallDir: String? = null,
    val pluginTargetPath: String? = null,
    val configTargetPath: String? = null,
)
