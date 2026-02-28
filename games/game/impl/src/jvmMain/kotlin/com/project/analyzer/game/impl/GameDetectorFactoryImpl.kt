package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameDetectorFactory
import com.project.analyzer.game.api.GameProfiles
import com.project.analyzer.game.api.GameWindowDetector
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher

@Inject
@SingleIn(AppScope::class)
internal class GameDetectorFactoryImpl : GameDetectorFactory {
    override fun create(
        requireForeground: Boolean,
        coroutineDispatcher: CoroutineDispatcher,
    ): GameWindowDetector = GameDetector(
        configs = GameProfiles.detectorConfigs(),
        requireForeground = requireForeground,
        coroutineDispatcher = coroutineDispatcher,
    )
}

