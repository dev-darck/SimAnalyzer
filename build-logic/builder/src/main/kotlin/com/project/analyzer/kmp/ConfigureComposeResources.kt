package com.project.analyzer.kmp

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeResources() {
    pluginManager.applyPlugin(deps.plugins.composeCompiler)

    val compose = extensions.getByName("compose") as ComposeExtension
    compose.apply {
        extensions.configure<ResourcesExtension> {
            generateResClass = ResourceClassGeneration.Auto
            packageOfResClass = "${project.name}.Res"
        }
    }

    extensions.configure<KotlinMultiplatformExtension> {
        dependencies {
            implementation(deps.compose.components.resources)
        }
    }
}
