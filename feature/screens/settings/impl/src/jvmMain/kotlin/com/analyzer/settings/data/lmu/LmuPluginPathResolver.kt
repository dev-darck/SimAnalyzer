package com.analyzer.settings.data.lmu

import com.analyzer.settings.domain.model.LMU_PLUGIN_REPOSITORY_URL
import com.analyzer.settings.domain.model.LmuPluginSetupDetails
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Path

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginPathResolver {

    fun pluginTargetPath(installDir: Path): Path = installDir.resolve(LMU_PLUGIN_DIR_NAME).resolve(LMU_PLUGIN_DLL_NAME)

    fun configPath(installDir: Path): Path = installDir.resolve(LMU_USER_DATA_DIR_NAME)
        .resolve(LMU_PLAYER_DIR_NAME)
        .resolve(LMU_CUSTOM_PLUGIN_VARIABLES_FILE_NAME)

    fun buildSetupDetails(
        metadata: LmuPluginMetadata? = null,
        latestVersion: String? = metadata?.version,
        downloadPageUrl: String? = metadata?.downloadPageUrl,
        archiveSha256: String? = null,
        pluginSha256: String? = null,
        installDir: Path?,
    ): LmuPluginSetupDetails = LmuPluginSetupDetails(
        latestVersion = latestVersion,
        repositoryUrl = LMU_PLUGIN_REPOSITORY_URL,
        downloadPageUrl = downloadPageUrl,
        archiveSha256 = archiveSha256,
        pluginSha256 = pluginSha256,
        gameInstallDir = installDir?.toAbsolutePath()?.normalize()?.toString(),
        pluginTargetPath = installDir?.let(::pluginTargetPath)?.toString(),
        configTargetPath = installDir?.let(::configPath)?.toString(),
    )
}
