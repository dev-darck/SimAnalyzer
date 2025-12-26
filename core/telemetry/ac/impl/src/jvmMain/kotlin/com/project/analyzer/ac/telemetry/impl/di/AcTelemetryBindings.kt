package com.project.analyzer.ac.telemetry.impl.di

import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFallbackShmPatcher
import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.AcEvoLogLocator
import com.project.analyzer.ac.telemetry.impl.fallback.TrackCalibrationLoader
import com.project.analyzer.ac.telemetry.impl.internal.AcLapState
import com.project.analyzer.ac.telemetry.impl.internal.AcPollConfig
import com.project.analyzer.ac.telemetry.impl.internal.AcPollLoop
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.AcSessionCache
import com.project.analyzer.ac.telemetry.impl.internal.mapper.CarMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.DamageMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.EnvironmentMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.LapMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.SessionMapper
import com.project.analyzer.ac.telemetry.impl.internal.mapper.WheelsMapper
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.AcShmNames
import com.project.analyzer.ac.telemetry.impl.shm.DefaultAcSharedMemory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
@BindingContainer
object AcTelemetryBindings {

    @Provides
    @SingleIn(AppScope::class)
    fun providePollConfig(): AcPollConfig = AcPollConfig(
        targetHz = 360,
        reconnectDelayMs = 1000,
        gameNotRunningPollMs = 500,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideSessionCache(): AcSessionCache = AcSessionCache()

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideLapState(cache: AcSessionCache): AcLapState = AcLapState(cache)

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideSessionMapper(cache: AcSessionCache): SessionMapper = SessionMapper(cache)

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideLapMapper(
        cache: AcSessionCache,
        lapState: AcLapState,
    ): LapMapper {
        val mapper = LapMapper(cache, lapState)
        cache.addSessionChangeListener { mapper.reset() }
        return mapper
    }

    @Provides
    @SingleIn(AppScope::class)
    fun provideAcSharedMemory(
        shm: AcShmNames
    ): AcSharedMemory = DefaultAcSharedMemory(
        names = shm,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideAcPollLoop(
        shm: AcSharedMemory,
        cfg: AcPollConfig,
        fallback: AcEvoFallbackShmPatcher,
    ): AcPollLoop = AcPollLoop(
        shm = shm,
        cfg = cfg,
        fallback
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideAcEvoFileInfoExtractor(
        locator: AcEvoLogLocator,
        calibrationLoader: TrackCalibrationLoader,
    ): AcEvoFileInfoExtractor = AcEvoFileInfoExtractor(
        locator = locator,
        calibrationLoader = calibrationLoader
    )

    @Provides
    fun provideAcEvoLogLocator(): AcEvoLogLocator = AcEvoLogLocator()

    @Provides
    fun provideTrackCalibrationLoader(json: Json): TrackCalibrationLoader = TrackCalibrationLoader(json)

    @Provides
    fun provideAcShmNames(): AcShmNames = AcShmNames()

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideCarMapper(): CarMapper = CarMapper()

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideWheelsMapper(cache: AcSessionCache): WheelsMapper = WheelsMapper(cache)

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideDamageMapper(): DamageMapper = DamageMapper()

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideEnvironmentMapper(): EnvironmentMapper = EnvironmentMapper()

    @Provides
    @SingleIn(AppScope::class)
    internal fun provideMapper(
        cache: AcSessionCache,
        sessionMapper: SessionMapper,
        lapMapper: LapMapper,
        carMapper: CarMapper,
        wheelsMapper: WheelsMapper,
        damageMapper: DamageMapper,
        environmentMapper: EnvironmentMapper,
    ): AcMapper = AcMapper(
        cache = cache,
        sessionMapper = sessionMapper,
        lapMapper = lapMapper,
        carMapper = carMapper,
        wheelsMapper = wheelsMapper,
        damageMapper = damageMapper,
        environmentMapper = environmentMapper,
    )
}
