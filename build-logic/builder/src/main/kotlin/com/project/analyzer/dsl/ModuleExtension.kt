package com.project.analyzer.dsl

import com.project.analyzer.base.configureMetro
import com.project.analyzer.kmp.configureKmpApp
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import javax.inject.Inject

abstract class ModuleExtension @Inject constructor(
    private val project: Project
) : Project by project {

    fun dependencies(scope: KmpDependenciesScope.() -> Unit = {}) {
        extensions.configure<KotlinMultiplatformExtension> {
            KmpDependenciesScope(this, project).scope()
        }
    }

    fun custom(project: Project.() -> Unit = {}) {
        project()
    }

    fun compose() {
        configureKmpApp()
    }

    fun metro(block: MetroPluginExtension.() -> Unit = {}) {
        configureMetro(block)
    }
}
