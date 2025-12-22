package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment

internal class JvmAppEnvironment : AppEnvironment {
    override val isDev: Boolean = System.getProperty("sim.dev") == "true" || System.getProperty("idea.active") == "true"
}
