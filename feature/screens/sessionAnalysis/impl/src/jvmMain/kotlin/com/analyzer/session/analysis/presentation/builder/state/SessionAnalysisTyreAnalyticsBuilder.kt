package com.analyzer.session.analysis.presentation.builder.state

import com.analyzer.session.analysis.presentation.formatter.formatTemperature
import com.analyzer.session.analysis.presentation.model.SessionAnalysisSampleUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisTyreAnalyticsUi
import com.project.analyzer.utils.ext.averageOrNull
import kotlinx.collections.immutable.ImmutableList

/**
 * Summarizes selected-lap tyre behaviour into UI-ready cards without exposing raw sample noise.
 */
internal fun buildTyreAnalyticsUi(samples: ImmutableList<SessionAnalysisSampleUi>): SessionAnalysisTyreAnalyticsUi {
    if (samples.isEmpty()) return SessionAnalysisTyreAnalyticsUi()

    val coreTemps = buildList<Float> {
        samples.forEach { sample ->
            sample.tyreFl?.coreTempC?.let(::add)
            sample.tyreFr?.coreTempC?.let(::add)
            sample.tyreRl?.coreTempC?.let(::add)
            sample.tyreRr?.coreTempC?.let(::add)
        }
    }
    val brakeTemps = buildList<Float> {
        samples.forEach { sample ->
            sample.tyreFl?.brakeTempC?.let(::add)
            sample.tyreFr?.brakeTempC?.let(::add)
            sample.tyreRl?.brakeTempC?.let(::add)
            sample.tyreRr?.brakeTempC?.let(::add)
        }
    }
    val slips = buildList<Float> {
        samples.forEach { sample ->
            sample.tyreFl?.slip?.let(::add)
            sample.tyreFr?.slip?.let(::add)
            sample.tyreRl?.slip?.let(::add)
            sample.tyreRr?.slip?.let(::add)
        }
    }
    val avgPressures = listOfNotNull(
        samples.mapNotNull { it.tyreFl?.pressurePsi }.averageOrNull()?.let { "FL" to it },
        samples.mapNotNull { it.tyreFr?.pressurePsi }.averageOrNull()?.let { "FR" to it },
        samples.mapNotNull { it.tyreRl?.pressurePsi }.averageOrNull()?.let { "RL" to it },
        samples.mapNotNull { it.tyreRr?.pressurePsi }.averageOrNull()?.let { "RR" to it },
    )
    val avgCoreByTyre = listOfNotNull(
        samples.mapNotNull { it.tyreFl?.coreTempC }.averageOrNull()?.let { "FL" to it },
        samples.mapNotNull { it.tyreFr?.coreTempC }.averageOrNull()?.let { "FR" to it },
        samples.mapNotNull { it.tyreRl?.coreTempC }.averageOrNull()?.let { "RL" to it },
        samples.mapNotNull { it.tyreRr?.coreTempC }.averageOrNull()?.let { "RR" to it },
    )

    return SessionAnalysisTyreAnalyticsUi(
        avgCoreTempC = coreTemps.averageOrNull(),
        peakCoreTempC = coreTemps.maxOrNull(),
        peakBrakeTempC = brakeTemps.maxOrNull(),
        pressureSpreadPsi = avgPressures.takeIf { it.isNotEmpty() }?.let { pressures ->
            (pressures.maxOf { it.second } - pressures.minOf { it.second }).coerceAtLeast(0f)
        },
        hottestTyreLabel = avgCoreByTyre.maxByOrNull { it.second }?.let { (label, temp) ->
            "$label ${formatTemperature(temp)}"
        } ?: "--",
        peakSlip = slips.maxOrNull(),
    )
}
