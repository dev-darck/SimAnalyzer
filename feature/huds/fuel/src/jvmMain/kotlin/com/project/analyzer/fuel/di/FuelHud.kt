package com.project.analyzer.fuel.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@ContributesIntoSet(HudScope::class)
@SingleIn(HudScope::class)
@Inject
class FuelHud : HudPanel {

    override val id: String = "fuel"

    @Composable
    override fun Content(modifier: Modifier) {
    }
}
