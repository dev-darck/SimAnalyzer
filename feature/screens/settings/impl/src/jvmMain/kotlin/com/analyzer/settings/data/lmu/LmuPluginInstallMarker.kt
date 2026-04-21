package com.analyzer.settings.data.lmu

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.preference.api.str
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.nio.file.Path

@Inject
@SingleIn(ScreenScope::class)
internal class LmuPluginInstallMarker(@param:UserPref private val userPreferences: Preference) {

    suspend fun isUpToDate(metadata: LmuPluginMetadata, installDir: Path, pluginSha256: String): Boolean {
        val storedVersion = userPreferences.get(KEY_LMU_PLUGIN_VERSION, "")
        val storedInstallDir = userPreferences.get(KEY_LMU_PLUGIN_INSTALL_DIR, "")
        val storedPluginSha256 = userPreferences.get(KEY_LMU_PLUGIN_SHA256, "")
        return storedVersion == metadata.version &&
            storedInstallDir.equals(normalizedInstallDir(installDir), ignoreCase = true) &&
            storedPluginSha256.equals(pluginSha256, ignoreCase = true)
    }

    suspend fun store(metadata: LmuPluginMetadata, installDir: Path, archiveSha256: String, pluginSha256: String) {
        userPreferences.put(KEY_LMU_PLUGIN_VERSION to metadata.version)
        userPreferences.put(KEY_LMU_PLUGIN_INSTALL_DIR to normalizedInstallDir(installDir))
        userPreferences.put(KEY_LMU_PLUGIN_ARCHIVE_SHA256 to archiveSha256)
        userPreferences.put(KEY_LMU_PLUGIN_SHA256 to pluginSha256)
    }

    private fun normalizedInstallDir(path: Path): String = path.toAbsolutePath().normalize().toString()

    private companion object {
        val KEY_LMU_PLUGIN_VERSION: StringPrefKey = "lmu_plugin_version".str
        val KEY_LMU_PLUGIN_INSTALL_DIR: StringPrefKey = "lmu_plugin_install_dir".str
        val KEY_LMU_PLUGIN_ARCHIVE_SHA256: StringPrefKey = "lmu_plugin_archive_sha256".str
        val KEY_LMU_PLUGIN_SHA256: StringPrefKey = "lmu_plugin_sha256".str
    }
}
