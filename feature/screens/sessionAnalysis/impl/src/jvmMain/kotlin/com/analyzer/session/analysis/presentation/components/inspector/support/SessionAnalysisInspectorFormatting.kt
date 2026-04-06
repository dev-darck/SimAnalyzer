package com.analyzer.session.analysis.presentation.components.inspector.support

import androidx.compose.runtime.Composable
import com.analyzer.session.analysis.presentation.components.inspector.model.SessionAnalysisInspectorTab
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_corners
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_inputs
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_line
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_pace
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_setup
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_inspector_tab_tyres
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * Inspector formatting keeps card text terse and consistent across summary, tyre, and setup sections.
 */
internal fun formatTemperatureDelta(value: Float?, reference: Float?): String = when {
    value == null || reference == null -> "--"

    else -> {
        val delta = value - reference
        "${if (delta >= 0f) "+" else ""}${"%.1f".format(delta)}°C"
    }
}

internal fun formatPressureDelta(value: Float?): String = value?.let { "${"%.2f".format(abs(it))} psi" } ?: "--"

internal val SessionAnalysisInspectorTab.title: String
    @Composable
    get() = when (this) {
        SessionAnalysisInspectorTab.Timing -> stringResource(Res.string.session_analysis_inspector_tab_pace)
        SessionAnalysisInspectorTab.Corners -> stringResource(Res.string.session_analysis_inspector_tab_corners)
        SessionAnalysisInspectorTab.Line -> stringResource(Res.string.session_analysis_inspector_tab_line)
        SessionAnalysisInspectorTab.Setup -> stringResource(Res.string.session_analysis_inspector_tab_setup)
        SessionAnalysisInspectorTab.Inputs -> stringResource(Res.string.session_analysis_inspector_tab_inputs)
        SessionAnalysisInspectorTab.Tyres -> stringResource(Res.string.session_analysis_inspector_tab_tyres)
    }
