package com.project.analyzer.scope

import com.project.analyzer.ProjectDsl
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

internal fun ProjectScope.dependenciesImpl(scope: DependenciesScope.() -> Unit = {}) {
    DependenciesScope(this).scope()
}

internal fun ProjectScope.customImpl(block: Project.() -> Unit = {}) {
    block()
}

@ProjectDsl
class DependenciesScope internal constructor(
    project: Project,
) : Project by project {

    val lib
        get() = deps

    val ProjectDependency.commonImpl
        get() = addTo(
            sourceSetName = "commonMain",
            notation = this,
        ) { dep -> implementation(dep) }

    val ProjectDependency.jvmImpl
        get() = addTo(
            sourceSetName = "jvmMain",
            notation = this,
        ) { dep -> implementation(dep) }

    val ProjectDependency.jvmTestFixtures: Unit
        get() {
            val projectPath = path

            addTo(
                sourceSetName = "jvmTest",
                notation = dependencies.project(
                    mapOf(
                        "path" to projectPath,
                        "configuration" to "testFixturesApiElements",
                    ),
                ),
            ) { dep -> compileOnly(dep) }

            addTo(
                sourceSetName = "jvmTest",
                notation = dependencies.project(
                    mapOf(
                        "path" to projectPath,
                        "configuration" to "testFixturesRuntimeElements",
                    ),
                ),
            ) { dep -> runtimeOnly(dep) }
        }

    val Provider<MinimalExternalModuleDependency>.jvmImpl
        get() = addTo(
            sourceSetName = "jvmMain",
            notation = get(),
        ) { dep -> implementation(dep) }

    val Provider<MinimalExternalModuleDependency>.jvmTest
        get() = addTo(
            sourceSetName = "jvmTest",
            notation = get(),
        ) { dep -> implementation(dep) }

    val Provider<MinimalExternalModuleDependency>.testFixturesImpl
        get() = addToConfiguration(
            configurationName = "testFixturesImplementation",
            notation = get(),
        )

    val Provider<MinimalExternalModuleDependency>.testFixturesApi
        get() = addToConfiguration(
            configurationName = "testFixturesApi",
            notation = get(),
        )

    val Provider<MinimalExternalModuleDependency>.commonImpl
        get() = addTo(
            sourceSetName = "commonMain",
            notation = get(),
        ) { dep -> implementation(dep) }

    val Provider<MinimalExternalModuleDependency>.commonTestImpl
        get() = addTo(
            sourceSetName = "commonTest",
            notation = get(),
        ) { dep -> implementation(dep) }

    val Provider<MinimalExternalModuleDependency>.runtimeOnly
        get() = addTo(
            sourceSetName = "jvmMain",
            notation = get(),
        ) { dep -> runtimeOnly(dep) }

    fun jvmImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.jvmImpl

    fun jvmTest(dep: Provider<MinimalExternalModuleDependency>) = dep.jvmTest

    fun jvmTestFixtures(dep: ProjectDependency) = dep.jvmTestFixtures

    fun commonImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.commonImpl

    fun commonTestImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.commonTestImpl

    fun testFixturesImpl(dep: Provider<MinimalExternalModuleDependency>) = dep.testFixturesImpl

    fun testFixturesApi(dep: Provider<MinimalExternalModuleDependency>) = dep.testFixturesApi

    private inline fun <T : Any> addTo(
        sourceSetName: String,
        notation: T,
        crossinline addOne: KotlinDependencyHandler.(T) -> Unit,
    ) {
        val kmp = extensions.findByType(KotlinMultiplatformExtension::class.java)
            ?: error("KMP extension is not configured for project '$path'")

        val sourceSet = kmp.sourceSets.findByName(sourceSetName)
            ?: error("KMP source set '$sourceSetName' not found. Available: ${kmp.sourceSets.names.sorted()}")

        sourceSet.dependencies {
            addOne(notation)
        }
    }

    private fun <T : Any> addToConfiguration(
        configurationName: String,
        notation: T,
    ) {
        dependencies.add(configurationName, notation)
    }
}
