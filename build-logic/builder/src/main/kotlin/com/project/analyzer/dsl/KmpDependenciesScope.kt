package com.project.analyzer.dsl

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

class KmpDependenciesScope(
    private val kmp: KotlinMultiplatformExtension,
    project: Project
) : Project by project {

    val lib: LibrariesForLibs get() = deps

    val Provider<PluginDependency>.plugin get() = pluginManager.applyPlugin(this)
    val ProjectDependency.commonImpl
        get() = addTo(
            sourceSetName = "commonMain",
            notation = this
        ) { dep -> implementation(dep) }
    val Provider<MinimalExternalModuleDependency>.jvmImpl
        get() = addTo(
            sourceSetName = "jvmMain",
            notation = this.get()
        ) { dep -> implementation(dep) }
    val Provider<MinimalExternalModuleDependency>.commonImpl
        get() = addTo(
            sourceSetName = "commonMain",
            notation = this.get()
        ) { dep -> implementation(dep) }
    val Provider<MinimalExternalModuleDependency>.commonTestImpl
        get() = addTo(
            sourceSetName = "commonTest",
            notation = this.get()
        ) { dep -> implementation(dep) }

    fun jvmImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.jvmImpl
    fun commonImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.commonImpl
    fun commonTestImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.commonTestImpl

    private inline fun <T : Any> addTo(
        sourceSetName: String,
        notation: T,
        crossinline addOne: KotlinDependencyHandler.(T) -> Unit
    ) {
        val ss = kmp.sourceSets.findByName(sourceSetName)
            ?: error("KMP source set '$sourceSetName' not found. Available: ${kmp.sourceSets.names.sorted()}")
        ss.dependencies { addOne(notation) }
    }
}
