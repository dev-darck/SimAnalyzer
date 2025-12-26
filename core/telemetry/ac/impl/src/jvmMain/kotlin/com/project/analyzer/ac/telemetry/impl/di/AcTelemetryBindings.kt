package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.internal.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.shm.AcShmNames
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(SessionScope::class)
@BindingContainer
object AcTelemetryBindings {

    @Provides
    fun providePollConfig(): AcPollConfig = AcPollConfig(
        targetHz = 360,
        reconnectDelayMs = 1000,
        gameNotRunningPollMs = 500,
    )

    @Provides
    fun provideShmNames(): AcShmNames = AcShmNames()
}
