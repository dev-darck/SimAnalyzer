package com.project.analyzer.base

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import dev.zacsweers.metro.gradle.DelicateMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@OptIn(DelicateMetroGradleApi::class)
internal fun Project.configureMetro(
    block: MetroPluginExtension.() -> Unit = {}
) {
    pluginManager.applyPlugin(deps.plugins.metro)

    extensions.configure<MetroPluginExtension> {
        contributesAsInject.convention(true)
        block()
        enableFullBindingGraphValidation.convention(true)
    }
}
