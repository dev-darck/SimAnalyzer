package com.project.analyzer.game.api

import kotlinx.coroutines.CoroutineDispatcher

public interface GameDetectorFactory {
    public fun create(requireForeground: Boolean, coroutineDispatcher: CoroutineDispatcher): GameWindowDetector
}
