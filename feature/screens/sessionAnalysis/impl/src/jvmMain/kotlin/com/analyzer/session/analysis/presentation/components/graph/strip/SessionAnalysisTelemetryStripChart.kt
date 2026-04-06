@file:OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
)

package com.analyzer.session.analysis.presentation.components.graph.strip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.analyzer.session.analysis.presentation.components.graph.model.SessionAnalysisTooltipRow
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewActivePoint
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewComparisonPoints
import com.analyzer.session.analysis.presentation.components.graph.preview.sessionAnalysisGraphPreviewSpeedChart
import com.analyzer.session.analysis.presentation.components.graph.support.chartTooltipRows
import com.analyzer.session.analysis.presentation.components.graph.support.projectChartPoints
import com.analyzer.session.analysis.presentation.components.graph.support.rememberSessionAnalysisTelemetryTonePalette
import com.analyzer.session.analysis.presentation.components.graph.support.resolveChartCursorX
import com.analyzer.session.analysis.presentation.components.graph.support.resolveFraction
import com.analyzer.session.analysis.presentation.components.graph.support.toPolylinePath
import com.analyzer.session.analysis.presentation.components.graph.support.toneColor
import com.analyzer.session.analysis.presentation.formatter.formatTrackPosition
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetryChartUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTelemetrySeriesTone
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun RowScope.SessionAnalysisTelemetryStripChart(
    chart: SessionAnalysisTelemetryChartUi,
    comparisonPoints: ImmutableList<SessionAnalysisComparisonPointUi>,
    activePoint: SessionAnalysisComparisonPointUi,
    selectionLocked: Boolean,
    chartDomainStart: Float,
    chartDomainEnd: Float,
    modifier: Modifier = Modifier,
    onHoverFraction: (Float?) -> Unit,
    onPressFraction: (Float?) -> Unit,
) {
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val onHoverFractionState by rememberUpdatedState(onHoverFraction)
    val onPressFractionState by rememberUpdatedState(onPressFraction)
    val dashEffect = remember(density) {
        PathEffect.dashPathEffect(
            intervals = with(density) { floatArrayOf(10.dp.toPx(), 8.dp.toPx()) },
            phase = 0f,
        )
    }

    val tonePalette = rememberSessionAnalysisTelemetryTonePalette()
    val baselineStrokeWidthPx = with(density) { 1.dp.toPx() }
    val solidStrokeWidthPx = with(density) { 2.6.dp.toPx() }
    val dashedStrokeWidthPx = with(density) { 1.9.dp.toPx() }
    val lapToneColor = toneColor(
        tone = chart.series.firstOrNull { series -> !series.dashed }?.tone
            ?: SessionAnalysisTelemetrySeriesTone.Primary,
        palette = tonePalette,
    )
    val neutralToneColor = SimAnalyzerTheme.material.onSurfaceVariant
    val baselineColor = SimAnalyzerTheme.chrome.dividerSubtle
    val zeroLineColor = SimAnalyzerTheme.chrome.borderSubtle
    val cursorColor = SimAnalyzerTheme.chrome.borderStrong

    val chartPaths = remember(
        chart.series,
        chartSize,
        chartDomainStart,
        chartDomainEnd,
    ) {
        if (chartSize.width <= 0 || chartSize.height <= 0) {
            emptyList()
        } else {
            chart.series.map { series ->
                series to series.points
                    .projectChartPoints(
                        chartSize = chartSize,
                        domainStart = chartDomainStart,
                        domainEnd = chartDomainEnd,
                    )
                    .toPolylinePath()
            }
        }
    }
    val zeroLineY = remember(chart.zeroBaseline, chartSize) {
        chart.zeroBaseline?.let { baseline -> chartSize.height.toFloat() * baseline }
    }
    val cursorX = remember(activePoint.fraction, chartSize, chartDomainStart, chartDomainEnd) {
        resolveChartCursorX(
            fraction = activePoint.fraction,
            chartWidth = chartSize.width.toFloat(),
            domainStart = chartDomainStart,
            domainEnd = chartDomainEnd,
        )
    }

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(SimAnalyzerTheme.corners.field)
            .background(SimAnalyzerTheme.chrome.fillMuted)
            .onSizeChanged { size -> chartSize = size }
            .onPointerEvent(PointerEventType.Move) { event ->
                if (selectionLocked) return@onPointerEvent
                val pointerX = event.changes.firstOrNull()?.position?.x ?: return@onPointerEvent
                onHoverFractionState(
                    comparisonPoints.resolveFraction(
                        pointerX = pointerX,
                        width = chartSize.width.toFloat(),
                        domainStart = chartDomainStart,
                        domainEnd = chartDomainEnd,
                    ),
                )
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                val pointerX = event.changes.firstOrNull()?.position?.x ?: return@onPointerEvent
                onPressFractionState(
                    comparisonPoints.resolveFraction(
                        pointerX = pointerX,
                        width = chartSize.width.toFloat(),
                        domainStart = chartDomainStart,
                        domainEnd = chartDomainEnd,
                    ),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawLine(
                        color = baselineColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = baselineStrokeWidthPx,
                    )
                    zeroLineY?.let { y ->
                        drawLine(
                            color = zeroLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = baselineStrokeWidthPx,
                        )
                    }
                    chartPaths.forEach { (series, path) ->
                        if (path.isEmpty) return@forEach
                        drawPath(
                            path = path,
                            color = toneColor(
                                tone = series.tone,
                                palette = tonePalette,
                            ),
                            style = Stroke(
                                width = if (series.dashed) dashedStrokeWidthPx else solidStrokeWidthPx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = if (series.dashed) dashEffect else null,
                            ),
                        )
                    }
                },
        )
        cursorX?.let { x ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLine(
                            color = cursorColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = baselineStrokeWidthPx,
                        )
                    },
            )
            SessionAnalysisStripCursorBubble(
                trackPositionLabel = formatTrackPosition(activePoint.trackPosition),
                rows = chartTooltipRows(
                    kind = chart.kind,
                    activePoint = activePoint,
                    lapAccent = lapToneColor,
                    refAccent = tonePalette.referenceToneColor,
                    neutralAccent = neutralToneColor,
                ),
                cursorX = x,
                chartWidth = chartSize.width,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

@Composable
internal fun SessionAnalysisStripCursorBubble(
    trackPositionLabel: String,
    rows: ImmutableList<SessionAnalysisTooltipRow>,
    cursorX: Float,
    chartWidth: Int,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bubbleWidthPx = with(density) { 154.dp.roundToPx() }
    val marginPx = with(density) { 10.dp.roundToPx() }
    val bubbleX = (cursorX.toInt() + marginPx).coerceIn(0, (chartWidth - bubbleWidthPx).coerceAtLeast(0))

    Surface(
        modifier = modifier.offset { IntOffset(x = bubbleX, y = marginPx) },
        shape = SimAnalyzerTheme.corners.item,
        color = SimAnalyzerTheme.material.surface,
        border = BorderStroke(1.dp, SimAnalyzerTheme.chrome.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = trackPositionLabel,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.labelSmall,
            )
            rows.forEach { row ->
                Row(
                    modifier = Modifier.width(134.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row.label,
                        color = row.color,
                        style = SimAnalyzerTheme.typography.labelSmall,
                    )
                    Text(
                        text = row.value,
                        color = SimAnalyzerTheme.material.onSurface,
                        style = SimAnalyzerTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
internal fun SessionAnalysisTelemetryStripChartPreview() {
    SimAnalyzerTheme {
        Row(
            modifier = Modifier
                .width(880.dp)
                .height(160.dp),
        ) {
            SessionAnalysisTelemetryStripChart(
                chart = sessionAnalysisGraphPreviewSpeedChart(),
                comparisonPoints = sessionAnalysisGraphPreviewComparisonPoints(),
                activePoint = sessionAnalysisGraphPreviewActivePoint(),
                selectionLocked = false,
                chartDomainStart = 0f,
                chartDomainEnd = 1f,
                onHoverFraction = {},
                onPressFraction = {},
            )
        }
    }
}
