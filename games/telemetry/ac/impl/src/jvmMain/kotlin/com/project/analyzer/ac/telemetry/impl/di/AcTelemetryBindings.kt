package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.AcTelemetryLifecycle
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.EvoFileInfoSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.FileInfoExtractorStabilizer
import com.project.analyzer.ac.telemetry.impl.internal.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcFallbackPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.internal.pipeline.AcPollSnapshotAdapter
import com.project.analyzer.ac.telemetry.impl.shm.AcShmNames
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.game.api.AC_KEY
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.StringKey

@ContributesTo(SessionScope::class)
@BindingContainer
interface AcTelemetryBindings {

    @Binds
    @Extractor
    fun bindAcEvoFileInfoExtractor(impl: AcEvoFileInfoExtractor): EvoFileInfoSource

    @Binds
    @Stabilizer
    fun bindFileInfoExtractorStabilizer(impl: FileInfoExtractorStabilizer): EvoFileInfoSource

    @Binds
    @IntoMap
    @StringKey(AC_KEY)
    fun bindAcTelemetryLifecycle(impl: AcTelemetryLifecycle): TelemetryLifecycle

    companion object {

        @Provides
        @SingleIn(SessionScope::class)
        fun providePollConfig(): AcPollConfig = AcPollConfig()

        @Provides
        fun provideShmNames(): AcShmNames = AcShmNames()

        @Provides
        @IntoSet
        private fun provideFallbackSnapshotAdapter(impl: AcFallbackPollSnapshotAdapter): AcPollSnapshotAdapter = impl
    }
}

@Qualifier
annotation class Extractor

@Qualifier
annotation class Stabilizer
