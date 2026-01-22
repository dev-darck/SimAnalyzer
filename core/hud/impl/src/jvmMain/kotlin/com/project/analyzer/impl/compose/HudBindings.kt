package com.project.analyzer.impl.compose

import androidx.lifecycle.ViewModel
import com.project.analyzer.hud.api.HudScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(HudScope::class)
@BindingContainer
object HudBindings {

    @Provides
    @IntoMap
    @ViewModelKey(HudViewModel::class)
    private fun provideHudViewModel(
        preferences: HudPreferences
    ): ViewModel = HudViewModel(preferences)

}
