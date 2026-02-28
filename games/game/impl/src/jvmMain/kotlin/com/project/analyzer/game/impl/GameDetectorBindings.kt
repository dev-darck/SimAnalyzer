package com.project.analyzer.game.impl

import com.project.analyzer.game.api.GameDetectorFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface GameDetectorBindings {
    companion object {

        @Provides
        private fun provideGameDetectorFactory(impl: GameDetectorFactoryImpl): GameDetectorFactory = impl
    }
}
