package com.project.analyzer.kmp

import com.project.analyzer.applyPlugin
import com.project.analyzer.deps
import org.gradle.api.Project

internal fun Project.configureStorytale() {
    pluginManager.applyPlugin(deps.plugins.storytale)
}
