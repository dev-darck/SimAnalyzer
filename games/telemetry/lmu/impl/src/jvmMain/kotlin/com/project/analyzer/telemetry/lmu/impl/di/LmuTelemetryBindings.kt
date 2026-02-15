package com.project.analyzer.telemetry.lmu.impl.di

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.game.api.LMU_KEY
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.lmu.impl.LmuTelemetryFeed
import com.project.analyzer.telemetry.lmu.impl.LmuTelemetryLifecycle
import com.project.analyzer.telemetry.lmu.impl.shm.DefaultLmuSharedMemory
import com.project.analyzer.telemetry.lmu.impl.shm.LmuSharedMemory
import com.project.analyzer.telemetry.lmu.impl.shm.LmuSharedMemoryTelemetryFeed
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey

@ContributesTo(SessionScope::class)
@BindingContainer
object LmuTelemetryBindings {

    @Provides
    @IntoMap
    @StringKey(LMU_KEY)
    private fun provideLmuTelemetryLifecycle(
        impl: LmuTelemetryLifecycle
    ): TelemetryLifecycle = impl

    @Provides
    private fun provideLmuSharedMemory(
        impl: DefaultLmuSharedMemory
    ): LmuSharedMemory = impl

    @Provides
    private fun provideLmuTelemetryFeed(
        impl: LmuSharedMemoryTelemetryFeed
    ): LmuTelemetryFeed = impl
}
