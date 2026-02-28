package com.project.analyzer.inputs.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.hud.api.HudAnchor
import com.project.analyzer.hud.api.HudPanel
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.presentation.InputsHudContent
import com.project.analyzer.inputs.presentation.InputsHudViewModel
import com.project.analyzer.inputs.presentation.InputsIntent
import com.project.analyzer.inputs.presentation.demoState
import com.project.analyzer.inputs.settings.InputsHudSettingsContent
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Inject
@ContributesIntoSet(HudScope::class)
@SingleIn(HudScope::class)
class InputsHudPanel : HudPanel {

    override val id: String = "inputs"
    override val description: String = "Real-time input traces to analyze throttle/brake/clutch and steering."
    override val defaultAnchor: HudAnchor = HudAnchor.BottomLeft
    override val hasSettings: Boolean = true

    @Composable
    override fun Content(modifier: Modifier) {
        val viewModel = metroViewModel<InputsHudViewModel>()
        LaunchedEffect(Unit) { viewModel.dispatch(InputsIntent.Start) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        InputsHudContent(
            state = state,
            modifier = modifier,
        )
    }

    @Composable
    override fun DemoContent(modifier: Modifier) {
        val vm = metroViewModel<InputsHudViewModel>()
        val state by vm.state.collectAsStateWithLifecycle()

        InputsHudContent(
            state = demoState(sessionActive = true, inputHudSettings = state.settings),
            modifier = modifier,
            isToolTipEnabled = true,
        )
    }

    @Composable
    override fun SettingsContent(modifier: Modifier) {
        val vm = metroViewModel<InputsHudViewModel>()
        val state by vm.state.collectAsStateWithLifecycle()

        InputsHudSettingsContent(
            settings = state.settings,
            modifier = modifier,
            onChange = { settings ->
                vm.dispatch(InputsIntent.UpdateSettings(settings))
            },
        )
    }
}
