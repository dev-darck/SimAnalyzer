package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.EvoFileInfoSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.FileInfoExtractorStabilizer
import com.project.analyzer.ac.telemetry.impl.internal.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.shm.AcShmNames
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier

@ContributesTo(SessionScope::class)
@BindingContainer
interface AcTelemetryBindings {

    @Binds
    @Extractor
    fun bindAcEvoFileInfoExtractor(impl: AcEvoFileInfoExtractor): EvoFileInfoSource

    @Binds
    @Stabilizer
    fun bindFileInfoExtractorStabilizer(impl: FileInfoExtractorStabilizer): EvoFileInfoSource

    companion object {

        @Provides
        fun providePollConfig(): AcPollConfig = AcPollConfig()

        @Provides
        fun provideShmNames(): AcShmNames = AcShmNames()
    }
}

@Qualifier
annotation class Extractor

@Qualifier
annotation class Stabilizer
