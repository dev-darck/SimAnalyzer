package com.project.analyzer.kmp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.compose.resources.ResourcesExtension.ResourceClassGeneration

internal fun Project.configureComposeResources() {
    val compose = extensions.getByName("compose") as ComposeExtension
    compose.apply {
        extensions.configure<ResourcesExtension> {
            generateResClass = ResourceClassGeneration.Auto
            packageOfResClass = "${project.name}.Res"
        }
    }
}
