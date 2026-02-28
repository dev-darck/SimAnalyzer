package com.project.analyzer.leak.impl

import com.project.analyzer.leak.api.LeakCanaryController
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface LeakBindings {
    companion object {

        @Provides
        private fun provideLeakCanaryController(impl: LeakCanaryControllerImpl): LeakCanaryController = impl
    }
}
