package com.project.analyzer.scope

import com.project.analyzer.detekt.configureDetekt
import com.project.analyzer.kmp.configureComposeKmp
import com.project.analyzer.kmp.configureComposeResources
import com.project.analyzer.kmp.configureDesktop
import com.project.analyzer.kmp.configureKmpLibrary
import com.project.analyzer.versioncatalog.checker.registerDependencyWatcher
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun ProjectScope.configureAppImpl(scope: JvmApplication.() -> Unit = {}) {
    registerDependencyWatcher()
    detektConfiguration()
    configureComposeKmp()
    configureDesktop(scope)
}

internal fun ProjectScope.configureDesktopAppImpl(scope: JvmApplication.() -> Unit = {}) {
    detektConfiguration()
    configureDesktop(scope)
}

internal fun ProjectScope.detektConfigurationImpl(block: DetektExtension.() -> Unit = {}) {
    configureDetekt(block)
}

internal fun ProjectScope.configureExplicitApiImpl() {
    extensions.configure<KotlinMultiplatformExtension> {
        explicitApi()
    }
}

internal fun ProjectScope.configureLibraryImpl() {
    detektConfiguration()
    configureKmpLibrary()
}

internal fun ProjectScope.composeImpl() {
    configureComposeKmp()
}

internal fun ProjectScope.resourcesImpl() {
    configureComposeResources()
}

internal fun ProjectScope.testFixturesImpl() {
    pluginManager.apply("java-test-fixtures")
}
