package com.project.analyzer.impl.compose

import com.project.analyzer.hud.api.HudPreferencesStore
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface HudAppBindings {
    companion object {

        @Provides
        private fun provideHudPreferencesStore(@UserPref preference: Preference): HudPreferencesStore =
            HudPreferences(preference)
    }
}
