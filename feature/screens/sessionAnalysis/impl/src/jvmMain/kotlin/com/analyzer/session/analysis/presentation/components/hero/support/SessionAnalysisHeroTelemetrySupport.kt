package com.analyzer.session.analysis.presentation.components.hero.support

import com.analyzer.session.analysis.presentation.formatter.formatDelta
import com.analyzer.session.analysis.presentation.formatter.formatFuel
import com.analyzer.session.analysis.presentation.formatter.formatGear
import com.analyzer.session.analysis.presentation.formatter.formatPercent
import com.analyzer.session.analysis.presentation.formatter.formatRpm
import com.analyzer.session.analysis.presentation.formatter.formatSpeed
import com.analyzer.session.analysis.presentation.formatter.formatTrackPosition
import com.analyzer.session.analysis.presentation.model.SessionAnalysisComparisonPointUi
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi

internal data class SessionAnalysisHeroMetricUi(val label: String, val value: String)

internal data class SessionAnalysisHeroMetricRowUi(
    val start: SessionAnalysisHeroMetricUi,
    val end: SessionAnalysisHeroMetricUi,
)

internal fun buildSessionAnalysisHeroMetricRows(
    activePoint: SessionAnalysisComparisonPointUi?,
    activeSample: SessionAnalysisSampleUi?,
    deltaLabel: String,
    speedLabel: String,
    positionLabel: String,
    inputsLabel: String,
    gearRpmLabel: String,
    fuelLabel: String,
    selectedLabel: String,
    referenceLabel: String,
): List<SessionAnalysisHeroMetricRowUi> = listOf(
    SessionAnalysisHeroMetricRowUi(
        start = SessionAnalysisHeroMetricUi(
            label = deltaLabel,
            value = formatDelta(activePoint?.deltaMs),
        ),
        end = SessionAnalysisHeroMetricUi(
            label = speedLabel,
            value = formatSpeed(activeSample?.speedKmh),
        ),
    ),
    SessionAnalysisHeroMetricRowUi(
        start = SessionAnalysisHeroMetricUi(
            label = positionLabel,
            value = formatTrackPosition(activePoint?.trackPosition ?: activeSample?.trackPosition),
        ),
        end = SessionAnalysisHeroMetricUi(
            label = inputsLabel,
            value = "${formatPercent(activeSample?.throttle)} / ${formatPercent(activeSample?.brake)}",
        ),
    ),
    SessionAnalysisHeroMetricRowUi(
        start = SessionAnalysisHeroMetricUi(
            label = gearRpmLabel,
            value = "${formatGear(activeSample?.gear)}  ${formatRpm(activeSample?.rpm)}",
        ),
        end = SessionAnalysisHeroMetricUi(
            label = fuelLabel,
            value = formatFuel(activeSample?.fuelLiters),
        ),
    ),
    SessionAnalysisHeroMetricRowUi(
        start = SessionAnalysisHeroMetricUi(
            label = selectedLabel,
            value = formatSpeed(activePoint?.selectedSpeedKmh ?: activeSample?.speedKmh),
        ),
        end = SessionAnalysisHeroMetricUi(
            label = referenceLabel,
            value = formatSpeed(activePoint?.referenceSpeedKmh),
        ),
    ),
)
