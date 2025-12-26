package com.project.analyzer

import com.project.analyzer.detekt.configureDetekt
import com.project.analyzer.kmp.configureComposeResources
import com.project.analyzer.kmp.configureDesktop
import com.project.analyzer.kmp.configureComposeKmp
import com.project.analyzer.kmp.configureKmpLibrary
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@ProjectDsl
class ProjectScope(
    project: Project,
) : Project by project {

    fun configureApp() {
        configureComposeKmp()
    }

    fun configureResources() {
        configureComposeResources()
    }

    fun configureDesktopApp(scope: JvmApplication.() -> Unit = {}) {
        configureDesktop(scope)
    }

    fun detektConfiguration(block: DetektExtension.() -> Unit = {}) {
        configureDetekt(block)
    }

    fun configureExplicitApi() {
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
        }
    }

    fun configureLibrary() {
        configureKmpLibrary()
    }
}

fun Project.projectScope(block: ProjectScope.() -> Unit) {
    ProjectScope(this).block()
}
