package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreTemperatureBand

/**
 * Sample helpers wrap common telemetry predicates so resolver code reads in driving terms instead of raw fields.
 */
internal fun SessionAnalysisSample.coreTempCandidates(): List<Float?> = listOf(
    tyreFl?.coreTempC,
    tyreFr?.coreTempC,
    tyreRl?.coreTempC,
    tyreRr?.coreTempC,
)

internal fun SessionAnalysisSample.brakeTempCandidates(): List<Float?> = listOf(
    tyreFl?.brakeTempC,
    tyreFr?.brakeTempC,
    tyreRl?.brakeTempC,
    tyreRr?.brakeTempC,
)

internal fun SessionAnalysisSample.hasTyreBand(band: SessionAnalysisTyreTemperatureBand): Boolean =
    listOf(tyreFl, tyreFr, tyreRl, tyreRr).any { tyre ->
        tyre?.tempBand == band
    }

internal fun List<SessionAnalysisSample>.maxWheelValue(candidates: (SessionAnalysisSample) -> List<Float?>): Float? =
    asSequence()
        .flatMap { sample -> candidates(sample).asSequence() }
        .filterNotNull()
        .maxOrNull()
