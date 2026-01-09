@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.project.analyzer.kmp

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureProtoSerializer() {
    pluginManager.applyPlugin(deps.plugins.ksp)

    extensions.configure<KotlinMultiplatformExtension> {
        dependencies {
            implementation(deps.kotlinx.serialization.protobuf)
            implementation(deps.datastore.core)
            implementation(project(":ksp:datastore-processor:annotation"))
        }
        dependencies.add("kspJvm", project(":ksp:datastore-processor:processor"))
    }
}
