package com.analyzer.session.analysis.presentation.builder.state

import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_chart_gap_vs_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_graph_legend_ref
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_fuel
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_brake
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_rpm
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_steer
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_throttle
import org.jetbrains.compose.resources.getString

internal data class SessionAnalysisGraphStrings(
    val gapVsReference: String,
    val delta: String,
    val speed: String,
    val throttle: String,
    val brake: String,
    val steering: String,
    val rpm: String,
    val fuel: String,
    val lap: String,
    val reference: String,
) {

    companion object {

        suspend fun resolve(): SessionAnalysisGraphStrings = SessionAnalysisGraphStrings(
            gapVsReference = getString(Res.string.session_analysis_graph_chart_gap_vs_ref),
            delta = getString(Res.string.session_analysis_metric_delta),
            speed = getString(Res.string.session_analysis_metric_speed),
            throttle = getString(Res.string.session_analysis_metric_throttle),
            brake = getString(Res.string.session_analysis_metric_brake),
            steering = getString(Res.string.session_analysis_metric_steer),
            rpm = getString(Res.string.session_analysis_metric_rpm),
            fuel = getString(Res.string.session_analysis_hero_fuel),
            lap = getString(Res.string.session_analysis_graph_legend_lap),
            reference = getString(Res.string.session_analysis_graph_legend_ref),
        )
    }
}
