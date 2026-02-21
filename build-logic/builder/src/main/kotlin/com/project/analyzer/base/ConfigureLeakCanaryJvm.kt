package com.project.analyzer.base

import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureLeakCanaryJvm() {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            jvmMain.dependencies {
                implementation(deps.leakcanary.objectWatcher)
                implementation(deps.leakcanary.shark)
                implementation(deps.leakcanary.sharkLog)
            }
        }
    }
}
