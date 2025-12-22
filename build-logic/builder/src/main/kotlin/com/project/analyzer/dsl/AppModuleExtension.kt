package com.project.analyzer.dsl

import com.project.analyzer.base.configureLeakCanaryJvm
import org.gradle.api.Project
import javax.inject.Inject

abstract class AppModuleExtension @Inject constructor(
    project: Project
) : ModuleExtension(project) {

    fun leakCanary() {
        configureLeakCanaryJvm()
    }
}
