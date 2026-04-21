package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment
import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.impl.compose.HudPreferences
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
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
        private fun provideHudPreferencesStore(@UserPref preference: Preference): HudPreferencesStore =
            HudPreferences(preference)

        @Provides
        fun provideJson(): Json = Json {
            prettyPrint = true
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }
}
