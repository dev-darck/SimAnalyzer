package com.project.analyzer

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.project.analyzer.calibration.presentation.CalibrationHost
import com.project.analyzer.calibration.presentation.CalibrationScreen
import com.project.analyzer.calibration.presentation.LocalWindow
import com.project.analyzer.impl.di.createAppGraph
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

fun main() = application {
    val appGraph = createAppGraph()
    val dataSource = appGraph.telemetryDataSource

    Window(
        onCloseRequest = ::exitApplication,
        title = "SimAnalyzer – Telemetry Raw Data",
    ) {
        CompositionLocalProvider(
            LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            LocalWindow provides window
        ) {
            MaterialTheme {
                CalibrationHost()
//                val info = dataSource.frames().collectAsStateWithLifecycle(null).value
//                Text(
//                    "trackId ${info?.session?.track?.trackId}" +
//                        "\nbestLapTimeMs ${formatMsMmSs(info?.lap?.bestLapTimeMs)}" +
//                        "\ncurrentLapTimeMs ${formatMsMmSs(info?.lap?.currentLapTimeMs)}" +
//                        "\nlastSectorTimeMs ${formatMsMmSs(info?.lap?.lastSectorTimeMs)}" +
//                        "\nsector index ${info?.lap?.currentSectorIndex}" +
//                        "\nlap index ${info?.lap?.currentLapIndex}"
//                )
            }
        }
    }
}

fun formatMsMmSs(ms: Int?): String {
    if (ms == null) return "00:00.000"
    if (ms <= 0L) return "00:00.000"

    val totalSeconds = ms / 1_000L
    val milli = (ms % 1_000L).toInt()

    val seconds = (totalSeconds % 60L).toInt()
    val minutes = (totalSeconds / 60L).toInt()

    return "%02d:%02d.%03d".format(minutes, seconds, milli)
}
