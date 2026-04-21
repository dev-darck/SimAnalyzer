package com.analyzer.settings.data.lmu

import com.analyzer.settings.domain.model.LmuPluginInstallResult
import com.analyzer.settings.domain.model.LmuPluginInstallStep
import com.analyzer.settings.domain.model.LmuPluginSetupCheckResult
import com.analyzer.settings.domain.model.LmuPluginSetupDetails
import com.analyzer.settings.domain.repository.LmuPluginSetupRepository
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

@Inject
@SingleIn(ScreenScope::class)
internal class DefaultLmuPluginSetupRepository(
    private val gameInstallLocator: LmuGameInstallLocator,
    private val metadataSource: LmuPluginMetadataSource,
    private val archiveDownloader: LmuPluginArchiveDownloader,
    private val archiveValidator: LmuPluginArchiveValidator,
    private val installer: LmuPluginInstaller,
    private val configWriter: LmuPluginConfigWriter,
    private val marker: LmuPluginInstallMarker,
    private val pathResolver: LmuPluginPathResolver,
    @param:IO private val ioDispatcher: CoroutineDispatcher,
) : LmuPluginSetupRepository {

    private val logger = logger()

    override suspend fun inspectSetup(): LmuPluginSetupCheckResult {
        val installDir = locateInstallDir()
        val baseDetails = pathResolver.buildSetupDetails(installDir = installDir)
        val metadata = runCatching { metadataSource.resolveLatest() }.getOrElse { error ->
            if (error is CancellationException) throw error
            logger.warn(error) { "Unable to resolve the latest LMU shared memory plugin metadata." }
            return LmuPluginSetupCheckResult.Error(
                details = baseDetails,
                message = error.toLmuPluginUserMessage(),
            )
        }

        if (installDir == null) {
            return LmuPluginSetupCheckResult.GameNotFound(
                details = pathResolver.buildSetupDetails(metadata = metadata, installDir = null),
                message = "Expected Steam folder: steamapps/common/Le Mans Ultimate",
            )
        }

        val pluginTargetPath = pathResolver.pluginTargetPath(installDir)
        val existingPluginSha256 = readPluginSha256(pluginTargetPath)
        val details = pathResolver.buildSetupDetails(
            metadata = metadata,
            pluginSha256 = existingPluginSha256,
            installDir = installDir,
        )
        val configReady = configWriter.isReady(pathResolver.configPath(installDir))
        return if (existingPluginSha256 != null && configReady) {
            LmuPluginSetupCheckResult.Ready(details)
        } else {
            LmuPluginSetupCheckResult.InstallRequired(details)
        }
    }

    override suspend fun installLatestPlugin(
        details: LmuPluginSetupDetails,
        onProgress: (LmuPluginInstallStep) -> Unit,
    ): LmuPluginInstallResult {
        val installDir = parsePath(details.gameInstallDir) ?: locateInstallDir()
        if (installDir == null) {
            return LmuPluginInstallResult.Failure(
                details = pathResolver.buildSetupDetails(
                    latestVersion = details.latestVersion,
                    downloadPageUrl = details.downloadPageUrl,
                    installDir = null,
                ),
                message = "Le Mans Ultimate was not found in your Steam libraries.",
            )
        }

        val metadata = runCatching {
            resolveInstallMetadata(details, onProgress)
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            logger.warn(error) { "Unable to resolve the latest LMU shared memory plugin metadata." }
            return LmuPluginInstallResult.Failure(
                details = pathResolver.buildSetupDetails(
                    latestVersion = details.latestVersion,
                    downloadPageUrl = details.downloadPageUrl,
                    installDir = installDir,
                ),
                message = error.toLmuPluginUserMessage(),
            )
        }

        return runCatching {
            onProgress(LmuPluginInstallStep.DownloadingPackage)
            val archive = archiveDownloader.downloadArchive(metadata)
            onProgress(LmuPluginInstallStep.ValidatingPackage)
            val pluginDll = archiveValidator.extractPluginDll(archive)
            onProgress(LmuPluginInstallStep.ExtractingPlugin)
            installer.writePluginDll(pluginDll = pluginDll, installDir = installDir)
            onProgress(LmuPluginInstallStep.WritingConfiguration)
            installer.writeConfiguration(installDir = installDir)
            onProgress(LmuPluginInstallStep.Finalizing)
            installer.finalizeInstall(
                metadata = metadata,
                archive = archive,
                pluginDll = pluginDll,
                installDir = installDir,
            )
            pathResolver.buildSetupDetails(
                metadata = metadata,
                archiveSha256 = archive.sha256,
                pluginSha256 = pluginDll.sha256,
                installDir = installDir,
            )
        }.fold(
            onSuccess = { installedDetails ->
                logger.info {
                    "LMU shared memory plugin installed: " +
                        "version=${metadata.version} path=${installDir.toAbsolutePath()}"
                }
                LmuPluginInstallResult.Success(installedDetails)
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                logger.warn(error) { "LMU shared memory plugin install failed." }
                LmuPluginInstallResult.Failure(
                    details = pathResolver.buildSetupDetails(metadata = metadata, installDir = installDir),
                    message = error.toLmuPluginUserMessage(),
                )
            },
        )
    }

    private suspend fun resolveInstallMetadata(
        details: LmuPluginSetupDetails,
        onProgress: (LmuPluginInstallStep) -> Unit,
    ): LmuPluginMetadata {
        val version = details.latestVersion
        val downloadPageUrl = details.downloadPageUrl
        if (version != null && downloadPageUrl != null) {
            return LmuPluginMetadata(version = version, downloadPageUrl = downloadPageUrl)
        }

        onProgress(LmuPluginInstallStep.ResolvingSource)
        return metadataSource.resolveLatest()
    }

    private fun parsePath(raw: String?): Path? = runCatching {
        raw?.let(Path::of)?.toAbsolutePath()?.normalize()
    }.getOrNull()

    private suspend fun locateInstallDir(): Path? = withContext(ioDispatcher) {
        gameInstallLocator.locate()
    }

    private suspend fun readPluginSha256(path: Path): String? = withContext(ioDispatcher) {
        runCatching {
            path.takeIf(Files::isRegularFile)?.sha256Hex()
        }.getOrNull()
    }
}
