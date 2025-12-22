package com.project.analyzer.base

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureMetro() {
    pluginManager.applyPlugin(deps.plugins.metro)

    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            commonMain.dependencies {
                implementation(deps.metro.runtime)
                implementation(deps.metro.metrox.viewmodel.compose)
            }
        }
    }
}
