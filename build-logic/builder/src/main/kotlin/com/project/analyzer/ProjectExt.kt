package com.project.analyzer

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.plugins.PluginManager
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the
import org.gradle.plugin.use.PluginDependency

/**
 * workaround to make version catalog accessible in convention plugins
 * https://github.com/gradle/gradle/issues/15383
 */
internal val Project.deps: LibrariesForLibs
    get() =
        if (project.name != "gradle-kotlin-dsl-accessors") {
            the()
        } else {
            error("VersionCatalog can't work without gradle-kotlin-dsl-accessors")
        }

internal fun PluginManager.applyPlugin(provider: Provider<PluginDependency>) =
    apply(provider.plugin)

internal val Provider<PluginDependency>.plugin: String
    get() = get().pluginId
