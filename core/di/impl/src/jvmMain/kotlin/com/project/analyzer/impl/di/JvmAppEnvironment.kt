package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment

internal class JvmAppEnvironment : AppEnvironment {

    override val isDev: Boolean = System.getProperty("sim.dev") == "true" || System.getProperty("idea.active") == "true"
    override val osName: String by lazy { System.getProperty("os.name").lowercase() }
    override val isWindows: Boolean by lazy { osName.contains("win") }
    override val userHome: String by lazy { System.getProperty("user.home") }
}
