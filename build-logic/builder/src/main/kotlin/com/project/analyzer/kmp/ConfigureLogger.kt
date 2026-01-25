@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.project.analyzer.kmp

import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureLogger() {
    extensions.configure<KotlinMultiplatformExtension> {
        dependencies {
            implementation(deps.kotlin.logging)
            implementation(deps.sfl4j.api)
            implementation(deps.kotlinx.coroutines.slf4j)
            runtimeOnly(deps.logback.classic)
            implementation(project(":core:utils"))
        }
    }
}
