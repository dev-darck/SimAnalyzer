package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import com.project.analyzer.ac.telemetry.impl.trackmap.evo.AcEvoTrackAssetImporter
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path

@Serializable
internal data class AcEvoImportedContentManifest(
    val importerVersion: Int,
    val packagePath: String,
    val packageSizeBytes: Long,
    val packageLastModifiedEpochMs: Long,
    val importedAtEpochMs: Long,
    val assets: List<AcEvoImportedAsset>,
) {

    fun isCurrentFor(
        packagePath: Path,
        packageSizeBytes: Long,
        packageLastModifiedEpochMs: Long,
        assetsRoot: Path,
    ): Boolean {
        if (importerVersion != AcEvoTrackAssetImporter.IMPORTER_VERSION) return false
        if (this.packagePath != packagePath.toAbsolutePath().normalize().toString()) return false
        if (this.packageSizeBytes != packageSizeBytes) return false
        if (this.packageLastModifiedEpochMs != packageLastModifiedEpochMs) return false
        if (!Files.isDirectory(assetsRoot)) return false

        return assets.all { asset -> Files.isRegularFile(resolveRelativePath(assetsRoot, asset.relativePath)) }
    }

    private fun resolveRelativePath(root: Path, relativePath: String): Path {
        var current = root

        relativePath.split('/').filter(String::isNotBlank).forEach { segment ->
            current = current.resolve(segment)
        }

        return current.normalize()
    }
}
