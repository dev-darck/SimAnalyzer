package com.project.analyzer.kmp

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.exclude
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeKmp() {
    with(pluginManager) {
        applyPlugin(deps.plugins.kotlinMultiplatform)
        applyPlugin(deps.plugins.composeMultiplatform)
        applyPlugin(deps.plugins.composeCompiler)
        applyPlugin(deps.plugins.composeHotReload)
    }

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(21)
        jvm()

        val compose = extensions.getByName("compose") as ComposePlugin.Dependencies
        sourceSets {
            commonMain.dependencies {
                implementation(deps.compose.material3)
                implementation(deps.androidx.lifecycle.viewmodelCompose)
                implementation(deps.androidx.lifecycle.runtimeCompose)
                implementation(deps.compose.icons.core)
                implementation(deps.compose.icons.extended)
            }
            commonTest.dependencies {
                implementation(deps.kotlin.test)
            }
            jvmMain.dependencies {
                implementation(compose.desktop.currentOs) {
                    exclude("org.jetbrains.compose.material")
                }
                implementation(deps.kotlinx.coroutinesSwing)
                implementation(deps.compose.components.ui.tooling.preview)
                implementation(deps.compose.components.resources)
                implementation(deps.compose.ui)
                implementation(deps.compose.foundation)
                implementation(deps.compose.runtime)
            }
        }
    }
}
