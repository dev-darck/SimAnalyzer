package com.project.analyzer.live.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.live.presentation.components.ElectronicsBlock
import com.project.analyzer.live.presentation.components.FuelSectorsBlock
import com.project.analyzer.live.presentation.components.TelemetryBlock
import com.project.analyzer.live.presentation.components.TelemetryInputsBlock
import com.project.analyzer.live.presentation.components.TimingBoardBlock
import com.project.analyzer.live.presentation.components.WheelsBlock
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.adaptive.ResponsiveGridMode
import com.project.analyzer.ui.adaptive.ResponsiveScreen
import dev.zacsweers.metrox.viewmodel.metroViewModel

private val LIVE_TALL_CARD_HEIGHT = 200.dp
private val LIVE_COMPACT_CARD_HEIGHT = 208.dp

@Composable
internal fun LiveScreen() {
    val viewModel = metroViewModel<LiveViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatch(LiveIntent.Start)
    }

    Screen(state)
}

@Composable
internal fun Screen(state: LiveScreenState = LiveScreenState(), modifier: Modifier = Modifier) {
    ResponsiveScreen(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        gridMode = ResponsiveGridMode.Grid,
        backgroundColor = SimAnalyzerTheme.material.background,
    ) {
        item(key = "TelemetryBlock", isContentFull = true) {
            TelemetryBlock(
                modifier = Modifier.height(230.dp),
                speedKmh = state.speedKmh,
                rpmInt = state.rpmInt,
                rpmScale = state.rpmScale,
                maxScale = state.maxRpmScale,
                gear = state.gear,
            )
        }

        item("TimingBoardBlock") {
            TimingBoardBlock(
                modifier = Modifier.height(LIVE_TALL_CARD_HEIGHT),
                bestLapTime = state.bestLapTime,
                currentLapTime = state.currentLapTime,
                lastLapTime = state.lastLapTime,
                deltaCurrentTime = state.deltaCurrentTime,
                deltaLastTime = state.deltaLastTime,
                deltaCurrentIsPositive = state.deltaCurrentIsPositive,
                deltaLastIsPositive = state.deltaLastIsPositive,
                lapCount = state.lapCount,
            )
        }

        item("TelemetryInputsBlock") {
            TelemetryInputsBlock(
                modifier = Modifier.height(LIVE_TALL_CARD_HEIGHT),
                clutch = state.clutch,
                brake = state.brake,
                throttle = state.throttle,
                steerDeg = state.steerDeg,
            )
        }

        item("FuelSectorsBlock") {
            FuelSectorsBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LIVE_COMPACT_CARD_HEIGHT),
                fuelLiters = state.fuelLiters,
                estLaps = state.estLaps,
                sectors = state.sectors,
                fuelPerLap = state.fuelPerLap,
            )
        }

        item("ElectronicsBlock") {
            ElectronicsBlock(
                data = state.electronics,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LIVE_COMPACT_CARD_HEIGHT),
            )
        }

        item(key = "WheelsBlock", isContentFull = true) {
            WheelsBlock(
                wheels = state.wheels,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Composable
private fun ScreenPreview() {
    SimAnalyzerTheme {
        Screen()
    }
}
