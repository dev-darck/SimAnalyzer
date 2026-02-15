package com.project.analyzer.fuel.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.fuel.presentation.FuelDemoContent
import com.project.analyzer.fuel.presentation.FuelHudContent
import com.project.analyzer.fuel.presentation.viewmodel.FuelHudViewModel
import com.project.analyzer.fuel.presentation.viewmodel.FuelIntent
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Inject
@ContributesIntoSet(HudScope::class)
@SingleIn(HudScope::class)
class FuelHudPanel : HudPanel {

    override val id: String = "fuel"
    override val description: String = "Fuel tracking that uses prediction when game data is missing, " +
        "otherwise calculates per-lap consumption, saves the best lap stats, and restores them on next launch."

    @Composable
    override fun DemoContent(modifier: Modifier) {
        FuelDemoContent(modifier = modifier)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val viewModel = metroViewModel<FuelHudViewModel>()
        LaunchedEffect(Unit) { viewModel.dispatch(FuelIntent.Start) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        FuelHudContent(
            state = state,
            onResetAll = { viewModel.dispatch(FuelIntent.ResetAll) },
            modifier = modifier,
        )
    }
}
