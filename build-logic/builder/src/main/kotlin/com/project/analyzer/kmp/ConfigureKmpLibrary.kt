package com.project.analyzer.kmp

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureKmpLibrary() {
    with(pluginManager) {
        applyPlugin(deps.plugins.kotlinMultiplatform)
        applyPlugin(deps.plugins.kotlinSerialization)
    }

    extensions.configure<KotlinMultiplatformExtension> {
        jvmToolchain(21)
        jvm()

        sourceSets {
            commonMain.dependencies {
                implementation(deps.kotlinx.coroutines.core)
                implementation(deps.kotlinx.serialization.json)
            }
            commonTest.dependencies {
                implementation(deps.kotlin.test)
                implementation(deps.kotlin.testJunit)
                implementation(deps.junit)
            }
            jvmMain.dependencies {
                implementation(deps.kotlinx.coroutinesSwing)
            }
        }
    }
}
