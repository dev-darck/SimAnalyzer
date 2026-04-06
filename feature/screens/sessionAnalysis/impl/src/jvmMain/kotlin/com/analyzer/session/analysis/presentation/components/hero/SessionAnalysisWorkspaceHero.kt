@file:OptIn(ExperimentalComposeUiApi::class)

package com.analyzer.session.analysis.presentation.components.hero

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.SessionAnalysisUiTokens
import com.analyzer.session.analysis.presentation.components.common.SessionAnalysisStudioPanel
import com.analyzer.session.analysis.presentation.components.hero.support.HeroRailCompactWidth
import com.analyzer.session.analysis.presentation.components.hero.support.HeroRailNarrowBreakpoint
import com.analyzer.session.analysis.presentation.components.hero.support.HeroRailWideWidth
import com.analyzer.session.analysis.presentation.components.hero.support.SessionAnalysisHeroCard
import com.analyzer.session.analysis.presentation.components.hero.support.SessionAnalysisHeroMetricRowUi
import com.analyzer.session.analysis.presentation.components.hero.support.buildSessionAnalysisHeroMetricRows
import com.analyzer.session.analysis.presentation.components.map.SessionAnalysisTrackMapCanvas
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisDiagnosticSummaryUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTrackCanvasState
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.Res
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_collapse
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_expand
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_focus_off
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_focus_on
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_fuel
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_gear_rpm
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_ideal
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_pinned
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_position
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_reference
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_reference_lap
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_hero_selected
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_lap_label
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_delta
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_metric_speed
import com.project.analyzer.feature.screens.sessionAnalysis.impl.Res.session_analysis_telemetry_inputs
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * Renders the main workspace hero by combining the interactive track map with the live telemetry snapshot
 * and high-priority diagnostic cues around the current selection.
 */
@Composable
internal fun SessionAnalysisWorkspaceHero(
    trackCanvasState: SessionAnalysisTrackCanvasState?,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    diagnosticsLoading: Boolean,
    highlightsCount: Int,
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    selectionLocked: Boolean,
    cursorFraction: Float?,
    cursorFrameId: Long?,
    focusMode: Boolean,
    collapsed: Boolean,
    modifier: Modifier = Modifier,
    onHoverFraction: (Float?, Long?) -> Unit = { _, _ -> },
    onPressFraction: (Float?, Long?) -> Unit = { _, _ -> },
    onFocusModeToggle: () -> Unit = {},
    onCollapseToggle: () -> Unit = {},
    onVerticalScroll: (Float) -> Unit = {},
) {
    val scrollPixelsPerTick = with(LocalDensity.current) { 56.dp.toPx() }
    val mapScrollModifier = Modifier.onPointerEvent(PointerEventType.Scroll) { event ->
        val change = event.changes.firstOrNull() ?: return@onPointerEvent
        val delta = change.scrollDelta.y * scrollPixelsPerTick
        if (abs(delta) > 0.5f) {
            onVerticalScroll(delta)
        }
    }

    SessionAnalysisStudioPanel(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        opaqueBackground = true,
        containerColor = SimAnalyzerTheme.material.surface,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val railWidth = if (maxWidth >= HeroRailNarrowBreakpoint) {
                HeroRailWideWidth
            } else {
                HeroRailCompactWidth
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(SessionAnalysisUiTokens.blockGap),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SessionAnalysisHeroMapToolbar(
                        focusMode = focusMode,
                        collapsed = collapsed,
                        selectionLocked = selectionLocked,
                        onFocusModeToggle = onFocusModeToggle,
                        onCollapseToggle = onCollapseToggle,
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(mapScrollModifier),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = SimAnalyzerTheme.corners.card,
                            color = SimAnalyzerTheme.chrome.fillMuted,
                        ) {
                            SessionAnalysisTrackMapCanvas(
                                trackCanvasState = trackCanvasState,
                                cursorFraction = cursorFraction,
                                cursorFrameId = cursorFrameId,
                                selectionLocked = selectionLocked,
                                focusMode = focusMode,
                                activePoint = activePoint,
                                activeSample = activeSample,
                                modifier = Modifier.fillMaxSize(),
                                onHoverFraction = onHoverFraction,
                                onPressFraction = onPressFraction,
                            )
                        }

                        SessionAnalysisHeroTrackLegendOverlay(
                            selectedLapNumber = selectedLapNumber,
                            referenceLapNumber = referenceLapNumber,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 12.dp),
                        )
                    }
                }

                if (!collapsed) {
                    SessionAnalysisHeroSideRail(
                        trackCanvasState = trackCanvasState,
                        activePoint = activePoint,
                        activeSample = activeSample,
                        diagnosticSummary = diagnosticSummary,
                        diagnosticsLoading = diagnosticsLoading,
                        highlightsCount = highlightsCount,
                        railWidth = railWidth,
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionAnalysisHeroMapToolbar(
    focusMode: Boolean,
    collapsed: Boolean,
    selectionLocked: Boolean,
    onFocusModeToggle: () -> Unit,
    onCollapseToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisLegendPill(
            label = if (focusMode) {
                stringResource(Res.string.session_analysis_hero_focus_on)
            } else {
                stringResource(Res.string.session_analysis_hero_focus_off)
            },
            accent = if (focusMode) {
                SimAnalyzerTheme.extended.teal
            } else {
                SimAnalyzerTheme.material.onSurfaceVariant
            },
            dashed = false,
            onClick = onFocusModeToggle,
        )
        SessionAnalysisLegendPill(
            label = if (collapsed) {
                stringResource(Res.string.session_analysis_hero_expand)
            } else {
                stringResource(Res.string.session_analysis_hero_collapse)
            },
            accent = SimAnalyzerTheme.material.primary,
            dashed = false,
            onClick = onCollapseToggle,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (selectionLocked) {
            SessionAnalysisLegendPill(
                label = stringResource(Res.string.session_analysis_hero_pinned),
                accent = SimAnalyzerTheme.extended.amber,
                dashed = false,
            )
        }
    }
}

@Composable
private fun SessionAnalysisHeroTrackLegendOverlay(
    selectedLapNumber: Int?,
    referenceLapNumber: Int?,
    modifier: Modifier = Modifier,
) {
    SessionAnalysisHeroCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        SessionAnalysisHeroTrackLegendRow(
            label = selectedLapNumber?.let { lapNumber ->
                stringResource(Res.string.session_analysis_lap_label, lapNumber)
            } ?: stringResource(Res.string.session_analysis_hero_selected),
            accent = SimAnalyzerTheme.extended.amber,
            dashed = false,
        )
        SessionAnalysisHeroTrackLegendRow(
            label = referenceLapNumber?.let { lapNumber ->
                stringResource(Res.string.session_analysis_hero_reference_lap, lapNumber)
            } ?: stringResource(Res.string.session_analysis_hero_reference),
            accent = SimAnalyzerTheme.material.onSurfaceVariant,
            dashed = true,
        )
        SessionAnalysisHeroTrackLegendRow(
            label = stringResource(Res.string.session_analysis_hero_ideal),
            accent = SimAnalyzerTheme.extended.cyan,
            dashed = false,
        )
    }
}

@Composable
private fun SessionAnalysisHeroSideRail(
    trackCanvasState: SessionAnalysisTrackCanvasState?,
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    diagnosticSummary: SessionAnalysisDiagnosticSummaryUi?,
    diagnosticsLoading: Boolean,
    highlightsCount: Int,
    railWidth: Dp,
) {
    Column(
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SessionAnalysisDiagnosticOverview(
            summary = diagnosticSummary,
            isLoading = diagnosticsLoading,
            highlightsCount = highlightsCount,
            turnCount = trackCanvasState?.cornerMarkers?.size,
            modifier = Modifier.fillMaxWidth(),
        )

        SessionAnalysisHeroRailCard(modifier = Modifier.fillMaxWidth()) {
            SessionAnalysisHeroTelemetrySummary(
                activePoint = activePoint,
                activeSample = activeSample,
            )
        }
    }
}

@Composable
private fun SessionAnalysisHeroRailCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    SessionAnalysisHeroCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
private fun SessionAnalysisHeroTelemetrySummary(
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
) {
    val deltaLabel = stringResource(Res.string.session_analysis_metric_delta)
    val speedLabel = stringResource(Res.string.session_analysis_metric_speed)
    val positionLabel = stringResource(Res.string.session_analysis_hero_position)
    val inputsLabel = stringResource(Res.string.session_analysis_telemetry_inputs)
    val gearRpmLabel = stringResource(Res.string.session_analysis_hero_gear_rpm)
    val fuelLabel = stringResource(Res.string.session_analysis_hero_fuel)
    val selectedLabel = stringResource(Res.string.session_analysis_hero_selected)
    val referenceLabel = stringResource(Res.string.session_analysis_hero_reference)
    val metricRows = remember(
        activePoint,
        activeSample,
        deltaLabel,
        speedLabel,
        positionLabel,
        inputsLabel,
        gearRpmLabel,
        fuelLabel,
        selectedLabel,
        referenceLabel,
    ) {
        buildSessionAnalysisHeroMetricRows(
            activePoint = activePoint,
            activeSample = activeSample,
            deltaLabel = deltaLabel,
            speedLabel = speedLabel,
            positionLabel = positionLabel,
            inputsLabel = inputsLabel,
            gearRpmLabel = gearRpmLabel,
            fuelLabel = fuelLabel,
            selectedLabel = selectedLabel,
            referenceLabel = referenceLabel,
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        metricRows.forEach { metricRow ->
            SessionAnalysisHeroMetricRow(row = metricRow)
        }
    }
}

@Composable
private fun SessionAnalysisHeroMetricRow(row: SessionAnalysisHeroMetricRowUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionAnalysisHeroMetric(
            label = row.start.label,
            value = row.start.value,
            modifier = Modifier.weight(1f),
        )
        SessionAnalysisHeroMetric(
            label = row.end.label,
            value = row.end.value,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SessionAnalysisHeroTrackLegendRow(label: String, accent: Color, dashed: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionAnalysisLegendStroke(
            accent = accent,
            dashed = dashed,
            width = 22.dp,
            edgeInset = 1f,
            dashOn = 7f,
            dashOff = 5f,
        )
        Text(
            text = label,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SessionAnalysisLegendPill(label: String, accent: Color, dashed: Boolean, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.then(
            if (onClick != null) {
                Modifier.onClick(onClick = onClick)
            } else {
                Modifier
            },
        ),
        shape = SimAnalyzerTheme.corners.pill,
        color = if (onClick != null) {
            SimAnalyzerTheme.chrome.fillSelection
        } else {
            SimAnalyzerTheme.chrome.fillMuted
        },
        border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionAnalysisLegendStroke(
                accent = accent,
                dashed = dashed,
                width = 24.dp,
                edgeInset = 2f,
                dashOn = 7f,
                dashOff = 6f,
            )
            Text(
                text = label,
                color = accent,
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SessionAnalysisLegendStroke(
    accent: Color,
    dashed: Boolean,
    width: Dp,
    edgeInset: Float,
    dashOn: Float,
    dashOff: Float,
) {
    Canvas(modifier = Modifier.size(width = width, height = 8.dp)) {
        drawLine(
            color = accent,
            start = center.copy(x = edgeInset),
            end = center.copy(x = size.width - edgeInset),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff)) else null,
        )
    }
}

@Composable
private fun SessionAnalysisHeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = SimAnalyzerTheme.corners.item,
        color = SimAnalyzerTheme.chrome.fillMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
