package com.project.analyzer.dsl

import com.project.analyzer.base.configureLeakCanaryJvm
import com.project.analyzer.kmp.configureDesktop
import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.dsl.JvmApplication
import javax.inject.Inject

abstract class AppModuleExtension @Inject constructor(
    project: Project
) : ModuleExtension(project) {

    fun leakCanary() {
        configureLeakCanaryJvm()
    }

    fun configureApp(scope: JvmApplication.() -> Unit = {}) {
        configureDesktop(scope)
    }
}
