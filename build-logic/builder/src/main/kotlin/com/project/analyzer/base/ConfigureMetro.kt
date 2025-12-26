package com.project.analyzer.base

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import dev.zacsweers.metro.gradle.DelicateMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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

    extensions.configure<KotlinMultiplatformExtension> {
        dependencies {
            implementation(deps.metro.runtime)
        }
    }
}
