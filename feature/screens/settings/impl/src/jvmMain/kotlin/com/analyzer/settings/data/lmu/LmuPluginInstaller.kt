package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginInstaller(
    private val pathResolver: LmuPluginPathResolver,
    private val configWriter: LmuPluginConfigWriter,
    private val marker: LmuPluginInstallMarker,
    @param:IO private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun writePluginDll(pluginDll: LmuPluginDll, installDir: Path) {
        writePluginDll(pathResolver.pluginTargetPath(installDir), pluginDll.bytes)
    }

    suspend fun writeConfiguration(installDir: Path) {
        configWriter.write(pathResolver.configPath(installDir))
    }

    suspend fun finalizeInstall(
        metadata: LmuPluginMetadata,
        archive: LmuPluginArchive,
        pluginDll: LmuPluginDll,
        installDir: Path,
    ) {
        marker.store(
            metadata = metadata,
            installDir = installDir,
            archiveSha256 = archive.sha256,
            pluginSha256 = pluginDll.sha256,
        )
    }

    private suspend fun writePluginDll(targetPath: Path, bytes: ByteArray) = withContext(ioDispatcher) {
        Files.createDirectories(targetPath.parent)
        Files.write(
            targetPath,
            bytes,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
    }
}
