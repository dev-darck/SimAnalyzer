package com.project.analyzer.scope

import com.project.analyzer.ProjectDsl
import com.project.analyzer.base.configureLeakCanaryJvm
import com.project.analyzer.base.configureMetro
import com.project.analyzer.base.configureTest
import com.project.analyzer.kmp.configureDesktopBuildConfig
import com.project.analyzer.kmp.configureLogger
import com.project.analyzer.kmp.configureProtoSerializer
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
        configureMetro(block)
    }

    fun proto() {
        configureProtoSerializer()
    }

    fun logger() {
        configureLogger()
    }

    fun leakCanary() {
        configureLeakCanaryJvm()
    }

    fun test(config: TestOptions.() -> Unit = { both() }) {
        val options = TestOptions().apply(config)

        configureTest {
            if (options.delegate.enableUnit) unit()
            if (options.delegate.enableUi) ui()
        }
    }

    fun buildConfig(block: BuildConfigOptions.() -> Unit) {
        val options = BuildConfigOptions().apply(block)

        configureDesktopBuildConfig(options.spec)
    }

    fun dependencies(scope: DependenciesScope.() -> Unit = {}) {
        dependenciesImpl(scope)
    }

    fun custom(block: Project.() -> Unit = {}) {
        customImpl(block)
    }
}
