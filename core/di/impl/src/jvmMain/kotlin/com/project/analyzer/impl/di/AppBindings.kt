package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment
import com.project.analyzer.api.di.AppLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.serialization.json.Json

@ContributesTo(AppScope::class)
@BindingContainer
interface AppBindings {
    companion object {

        @Provides
        fun provideEnv(): AppEnvironment = JvmAppEnvironment()

        @Provides
        private fun provideAppLifecycle(impl: AppLifecycleImpl): AppLifecycle = impl

        @Provides
        fun provideJson(): Json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
