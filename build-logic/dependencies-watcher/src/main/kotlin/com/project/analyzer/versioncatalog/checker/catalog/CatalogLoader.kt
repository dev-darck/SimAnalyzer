package com.project.analyzer.versioncatalog.checker.catalog

import com.project.analyzer.versioncatalog.checker.model.ArtifactLookup
import com.project.analyzer.versioncatalog.checker.model.ConsumerKind
import com.project.analyzer.versioncatalog.checker.model.DependencyConsumer
import com.project.analyzer.versioncatalog.checker.model.DependencyWatcherConstants
import com.project.analyzer.versioncatalog.checker.model.VersionResolution
import com.project.analyzer.versioncatalog.checker.model.VersionSource
import com.project.analyzer.versioncatalog.checker.model.VersionTarget
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File

internal class CatalogLoader(
    private val diagnostics: MutableList<String>,
) {

    fun load(file: File): List<VersionTarget> {
        require(file.isFile) { "Version catalog not found: ${file.absolutePath}" }

        val document = Toml.parse(file.toPath())
        if (document.hasErrors()) {
            val errors = document.errors().joinToString(separator = "\n") { error -> "$error" }
            error("Failed to parse ${file.path}:\n$errors")
        }

        val versionsTable = document.getTable("versions")
        val targetsByKey = linkedMapOf<String, VersionTarget>()

        collectLibraries(
            libraries = document.getTable("libraries"),
            versions = versionsTable,
            targetsByKey = targetsByKey,
        )
        collectPlugins(
            plugins = document.getTable("plugins"),
            versions = versionsTable,
            targetsByKey = targetsByKey,
        )

        return targetsByKey.values.toList()
    }

    private fun collectLibraries(
        libraries: TomlTable?,
        versions: TomlTable?,
        targetsByKey: MutableMap<String, VersionTarget>,
    ) {
        if (libraries == null) return

        libraries.keySet().sorted().forEach { alias ->
            val entry = getTableOrNull(libraries, alias)
            if (entry == null) {
                diagnostics += "libraries.$alias is not a table and was skipped."
                return@forEach
            }

            val coordinates = resolveLibraryCoordinates(entry, alias) ?: return@forEach
            when (val version = resolveVersion(entry, versions, "libraries", alias)) {
                is VersionResolution.Resolved -> registerConsumer(
                    targetsByKey = targetsByKey,
                    resolution = version,
                    consumer = DependencyConsumer(
                        kind = ConsumerKind.Library,
                        alias = alias,
                        displayName = "libraries.$alias",
                        lookup = ArtifactLookup(
                            repositoryBaseUrls = listOf(
                                DependencyWatcherConstants.MAVEN_CENTRAL,
                                DependencyWatcherConstants.GOOGLE_MAVEN,
                                DependencyWatcherConstants.GRADLE_PLUGIN_PORTAL,
                            ),
                            groupId = coordinates.first,
                            artifactId = coordinates.second,
                        ),
                    ),
                )

                is VersionResolution.Unresolved -> diagnostics += version.reason
            }
        }
    }

    private fun collectPlugins(
        plugins: TomlTable?,
        versions: TomlTable?,
        targetsByKey: MutableMap<String, VersionTarget>,
    ) {
        if (plugins == null) return

        plugins.keySet().sorted().forEach { alias ->
            val entry = getTableOrNull(plugins, alias)
            if (entry == null) {
                diagnostics += "plugins.$alias is not a table and was skipped."
                return@forEach
            }

            val pluginId = getStringOrNull(entry, "id")
            if (pluginId.isNullOrBlank()) {
                diagnostics += "plugins.$alias is missing id."
                return@forEach
            }

            if (getStringOrNull(entry, "version").isNullOrBlank() && getTableOrNull(entry, "version") == null) {
                return@forEach
            }

            when (val version = resolveVersion(entry, versions, "plugins", alias)) {
                is VersionResolution.Resolved -> registerConsumer(
                    targetsByKey = targetsByKey,
                    resolution = version,
                    consumer = DependencyConsumer(
                        kind = ConsumerKind.Plugin,
                        alias = alias,
                        displayName = "plugins.$alias",
                        lookup = buildPluginLookup(pluginId),
                    ),
                )

                is VersionResolution.Unresolved -> diagnostics += version.reason
            }
        }
    }

    private fun resolveLibraryCoordinates(entry: TomlTable, alias: String): Pair<String, String>? {
        val module = getStringOrNull(entry, "module")
        return when {
            !module.isNullOrBlank() -> {
                val parts = module.split(':')
                if (parts.size != 2) {
                    diagnostics += "libraries.$alias has invalid module notation `$module`."
                    null
                } else {
                    parts[0] to parts[1]
                }
            }

            else -> {
                val group = getStringOrNull(entry, "group")
                val name = getStringOrNull(entry, "name")
                if (group.isNullOrBlank() || name.isNullOrBlank()) {
                    diagnostics += "libraries.$alias is missing module or group/name."
                    null
                } else {
                    group to name
                }
            }
        }
    }

    private fun resolveVersion(
        entry: TomlTable,
        versions: TomlTable?,
        section: String,
        alias: String,
    ): VersionResolution {
        val directVersion = getStringOrNull(entry, "version")
        if (!directVersion.isNullOrBlank()) {
            return VersionResolution.Resolved(
                source = VersionSource.Inline(section = section, alias = alias),
                version = directVersion,
            )
        }

        val versionTable = getTableOrNull(entry, "version")
            ?: return VersionResolution.Unresolved("$section.$alias has no supported version or version.ref.")

        val versionRef = getStringOrNull(versionTable, "ref")
            ?: return VersionResolution.Unresolved(
                "$section.$alias uses unsupported version table; expected version.ref."
            )

        val resolvedVersion = versions?.let { getStringOrNull(it, versionRef) }
            ?: return VersionResolution.Unresolved("$section.$alias references missing versions.$versionRef.")

        return VersionResolution.Resolved(
            source = VersionSource.Ref(versionRef),
            version = resolvedVersion,
        )
    }

    private fun registerConsumer(
        targetsByKey: MutableMap<String, VersionTarget>,
        resolution: VersionResolution.Resolved,
        consumer: DependencyConsumer,
    ) {
        val targetKey = resolution.source.displayName
        val target = targetsByKey.getOrPut(targetKey) {
            VersionTarget(
                key = targetKey,
                source = resolution.source,
                currentVersion = resolution.version,
            )
        }

        if (target.currentVersion != resolution.version) {
            diagnostics += "Conflicting current version for $targetKey: " +
                "`${target.currentVersion}` vs `${resolution.version}`."
            return
        }

        target.consumers += consumer
    }

    private fun buildPluginLookup(pluginId: String): ArtifactLookup =
        ArtifactLookup(
            repositoryBaseUrls = buildList {
                add(DependencyWatcherConstants.GRADLE_PLUGIN_PORTAL)
                if (pluginId.startsWith("org.jetbrains.compose.")) {
                    add(DependencyWatcherConstants.JETBRAINS_COMPOSE_DEV)
                }
            },
            groupId = pluginId,
            artifactId = "$pluginId.gradle.plugin",
        )

    private fun getStringOrNull(table: TomlTable, key: String): String? =
        runCatching { table.getString(key) }.getOrNull()

    private fun getTableOrNull(table: TomlTable, key: String): TomlTable? =
        runCatching { table.getTable(key) }.getOrNull()
}
