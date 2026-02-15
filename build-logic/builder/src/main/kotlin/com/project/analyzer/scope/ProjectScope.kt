package com.project.analyzer.scope

import com.project.analyzer.ProjectDsl
import dev.detekt.gradle.extensions.DetektExtension
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.JvmApplication

@ProjectDsl
class ProjectScope(
    project: Project,
) : Project by project {

    fun configureApp(scope: JvmApplication.() -> Unit = {}) {
        configureAppImpl(scope)
    }

    fun configureResources() {
        configureResourcesImpl()
    }

    fun configureDesktopApp(scope: JvmApplication.() -> Unit = {}) {
        configureDesktopAppImpl(scope)
    }

    fun detektConfiguration(block: DetektExtension.() -> Unit = {}) {
        detektConfigurationImpl(block)
    }

    fun configureExplicitApi() {
        configureExplicitApiImpl()
    }

    fun configureLibrary() {
        configureLibraryImpl()
    }

    fun compose() {
        composeImpl()
    }

    fun resources() {
        resourcesImpl()
    }

    fun metro(block: MetroPluginExtension.() -> Unit = {}) {
        metroImpl(block)
    }

    fun proto() {
        protoImpl()
    }

    fun logger() {
        loggerImpl()
    }

    fun leakCanary() {
        leakCanaryImpl()
    }

    fun test(config: TestOptions.() -> Unit = { both() }) {
        testImpl(config)
    }

    fun buildConfig(block: BuildConfigOptions.() -> Unit) {
        buildConfigImpl(block)
    }

    fun dependencies(scope: DependenciesScope.() -> Unit = {}) {
        dependenciesImpl(scope)
    }

    fun custom(block: Project.() -> Unit = {}) {
        customImpl(block)
    }
}
