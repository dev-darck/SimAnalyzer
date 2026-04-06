package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.FuelAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.PressureBalance
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.TyreAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.TyreConditionState
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.WearBalance
import com.project.analyzer.telemetry.analysis.api.model.report.common.IssueSeverity
import com.project.analyzer.telemetry.analysis.api.model.report.common.WheelPosition
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionContext
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Summarizes tyre condition and fuel cost for the comprehensive analysis report.
 */
@Inject
internal class ComprehensiveSessionAnalysisTyreFuelBuilder {

    internal fun buildTyreAnalyses(
        report: SessionAnalysisReport,
        tyreProfile: SessionAnalysisTyreProfile?,
    ): List<TyreAnalysis> = report.samples
        .groupBy(SessionAnalysisSample::lapNumber)
        .toSortedMap()
        .map { (lapNumber, lapSamples) ->
            val tyres = lapSamples.flatMap(::allTyres)
            val coreTemps = tyres.mapNotNull(SessionAnalysisTyreState::coreTempC)
            val pressures = mapOf(
                WheelPosition.FRONT_LEFT to lapSamples.mapNotNull { it.tyreFl?.pressurePsi }.averageOrNull(),
                WheelPosition.FRONT_RIGHT to lapSamples.mapNotNull { it.tyreFr?.pressurePsi }.averageOrNull(),
                WheelPosition.REAR_LEFT to lapSamples.mapNotNull { it.tyreRl?.pressurePsi }.averageOrNull(),
                WheelPosition.REAR_RIGHT to lapSamples.mapNotNull { it.tyreRr?.pressurePsi }.averageOrNull(),
            ).filterValues { it != null }.mapValues { (_, value) -> value ?: 0f }
            val avgCore = coreTemps.averageOrNull() ?: 0f
            val optimalPressure = tyreProfile?.let { (it.pressureOptimalMinPsi + it.pressureOptimalMaxPsi) * 0.5f }
                ?: pressures.values.averageOrNull()
                ?: 0f
            val pressureDeviation = pressures.values.map { abs(it - optimalPressure) }.averageOrNull() ?: 0f
            val avgSlip = tyres.mapNotNull(SessionAnalysisTyreState::slip).averageOrNull() ?: 0f
            val state = tyreStateFor(avgCore, tyreProfile)
            val wearBalance = resolveWearBalance(lapSamples)
            val severity = when {
                state == TyreConditionState.OVERHEATED || state == TyreConditionState.CRITICAL -> IssueSeverity.CRITICAL
                state == TyreConditionState.HOT || pressureDeviation >= 0.45f -> IssueSeverity.WARNING
                else -> IssueSeverity.NEUTRAL
            }

            TyreAnalysis(
                lapNumber = lapNumber,
                avgCoreTemp = avgCore,
                peakCoreTemp = coreTemps.maxOrNull() ?: 0f,
                tempSpread = coreTemps.spread(),
                innerOuterSpread = tyres.mapNotNull { tyre ->
                    val inner = tyre.innerTempC ?: return@mapNotNull null
                    val outer = tyre.outerTempC ?: return@mapNotNull null
                    abs(inner - outer)
                }.averageOrNull() ?: 0f,
                tyreState = state,
                pressures = pressures,
                pressureSpread = pressures.values.toList().spread(),
                optimalPressureDeviation = pressureDeviation,
                pressureBalance = when {
                    pressureDeviation >= 0.5f && pressures.values.averageOrNull() ?: 0f > optimalPressure -> PressureBalance.OVERINFLATED
                    pressureDeviation >= 0.5f -> PressureBalance.UNDERINFLATED
                    else -> PressureBalance.OPTIMAL
                },
                wearRate = ((avgSlip * 10f) + severity.ordinal).coerceAtLeast(0.1f),
                estimatedLifespan = (100f / ((avgSlip * 10f) + 1f)).roundToInt().coerceAtLeast(1),
                wearBalance = wearBalance,
                avgSlipRatio = avgSlip,
                peakSlipRatio = tyres.mapNotNull(SessionAnalysisTyreState::slip).maxOrNull() ?: 0f,
                slipConsistency = 1f - (
                    (
                        tyres.mapNotNull(
                            SessionAnalysisTyreState::slip,
                        ).standardDeviation() ?: 0f
                        ) / 0.2f
                    ).coerceIn(0f, 1f),
                degradationLevel = ((avgCore - (tyreProfile?.coreOptimalMaxC ?: avgCore)) / 18f).coerceIn(0f, 1f),
                performanceLoss = report.laps.firstOrNull {
                    it.lapNumber == lapNumber
                }?.deltaToBestMs?.toFloat()?.coerceAtLeast(0f) ?: 0f,
                recommendations = buildTyreRecommendations(state, pressureDeviation, wearBalance),
                severity = severity,
            )
        }

    internal fun buildFuelAnalysis(report: SessionAnalysisReport, context: SessionContext): FuelAnalysis {
        val validLaps = report.laps.filter(SessionAnalysisLap::isComplete)
        val trend = validLaps.mapNotNull(SessionAnalysisLap::fuelUsedLiters)
        val startFuel = firstFuel(report.samples) ?: context.fuelLoad
        val endFuel = lastFuel(report.samples) ?: startFuel
        val fuelPerLap = trend.averageOrNull() ?: ((startFuel - endFuel) / max(validLaps.size, 1))
        val referenceFuelPerLap =
            report.laps.minByOrNull { it.durationMs ?: Int.MAX_VALUE }?.fuelUsedLiters ?: fuelPerLap
        val coastZones = detectLiftAndCoastZones(report.samples)
        return FuelAnalysis(
            startFuel = startFuel,
            fuelUsed = (startFuel - endFuel).coerceAtLeast(0f),
            fuelPerLap = fuelPerLap,
            fuelPerLapTrend = trend,
            referenceFuelPerLap = referenceFuelPerLap,
            efficiencyDelta = fuelPerLap - referenceFuelPerLap,
            weightImpact = startFuel * 0.03f,
            estimatedLapsRemaining = if (fuelPerLap > 0f) (endFuel / fuelPerLap).toInt() else 0,
            liftingCoastingDetected = coastZones.isNotEmpty(),
            liftingCoastingZones = coastZones,
            recommendations = buildFuelRecommendations(fuelPerLap, coastZones),
            severity = if (fuelPerLap - referenceFuelPerLap >= 0.25f) IssueSeverity.WARNING else IssueSeverity.NEUTRAL,
        )
    }

    private fun tyreStateFor(avgCore: Float, tyreProfile: SessionAnalysisTyreProfile?): TyreConditionState {
        val min = tyreProfile?.coreOptimalMinC ?: return TyreConditionState.OPTIMAL
        val max = tyreProfile.coreOptimalMaxC
        return when {
            avgCore < min - 10f -> TyreConditionState.COLD
            avgCore < min - 3f -> TyreConditionState.WARMING
            avgCore <= max -> TyreConditionState.OPTIMAL
            avgCore <= max + 6f -> TyreConditionState.WARM
            avgCore <= max + 12f -> TyreConditionState.HOT
            avgCore <= max + 18f -> TyreConditionState.OVERHEATED
            else -> TyreConditionState.CRITICAL
        }
    }

    private fun buildTyreRecommendations(
        tyreState: TyreConditionState,
        pressureDeviation: Float,
        wearBalance: WearBalance,
    ): List<String> = buildList {
        if (tyreState in setOf(TyreConditionState.HOT, TyreConditionState.OVERHEATED, TyreConditionState.CRITICAL)) {
            add("Reduce slip and baseline pressure to bring the tyre back into the window.")
        }
        if (tyreState in setOf(TyreConditionState.COLD, TyreConditionState.WARMING)) {
            add("Build tyre temperature earlier in the lap.")
        }
        if (pressureDeviation >= 0.45f) add("Rebalance tyre pressures around the target window.")
        if (wearBalance != WearBalance.EVEN) {
            add(
                "Load balance across the car is uneven. Review setup and driving balance.",
            )
        }
    }

    private fun buildFuelRecommendations(fuelPerLap: Float, coastZones: List<Float>): List<String> = buildList {
        if (coastZones.isNotEmpty()) add("Use the existing lift-and-coast zones consistently.")
        if (fuelPerLap > 0f) add("Current average burn is ${fuelPerLap.roundToInt()} L/lap.")
    }

    private fun detectLiftAndCoastZones(samples: List<SessionAnalysisSample>): List<Float> = samples.zipWithNext()
        .filter { (current, next) ->
            (current.speedKmh ?: 0f) >= 120f &&
                (current.throttle ?: 0f) <= 0.08f &&
                (current.brake ?: 0f) <= 0.05f &&
                (next.brake ?: 0f) >= 0.2f
        }
        .mapNotNull { (current, _) -> current.trackPosition }
        .distinctBy { (it * 100f).roundToInt() }

    private fun resolveWearBalance(samples: List<SessionAnalysisSample>): WearBalance {
        val front = samples.mapNotNull {
            listOfNotNull(it.tyreFl?.coreTempC, it.tyreFr?.coreTempC).averageOrNull()
        }.averageOrNull() ?: return WearBalance.EVEN
        val rear = samples.mapNotNull {
            listOfNotNull(it.tyreRl?.coreTempC, it.tyreRr?.coreTempC).averageOrNull()
        }.averageOrNull() ?: front
        val left = samples.mapNotNull {
            listOfNotNull(it.tyreFl?.coreTempC, it.tyreRl?.coreTempC).averageOrNull()
        }.averageOrNull() ?: front
        val right = samples.mapNotNull {
            listOfNotNull(it.tyreFr?.coreTempC, it.tyreRr?.coreTempC).averageOrNull()
        }.averageOrNull() ?: front
        return when {
            front - rear >= 4f -> WearBalance.FRONT_HEAVY
            rear - front >= 4f -> WearBalance.REAR_HEAVY
            left - right >= 3f -> WearBalance.LEFT_HEAVY
            right - left >= 3f -> WearBalance.RIGHT_HEAVY
            else -> WearBalance.EVEN
        }
    }

    private fun allTyres(sample: SessionAnalysisSample): List<SessionAnalysisTyreState> = listOfNotNull(
        sample.tyreFl,
        sample.tyreFr,
        sample.tyreRl,
        sample.tyreRr,
    )

    private fun firstFuel(samples: List<SessionAnalysisSample>): Float? {
        for (sample in samples) sample.fuelLiters?.let { return it }
        return null
    }

    private fun lastFuel(samples: List<SessionAnalysisSample>): Float? {
        for (index in samples.indices.reversed()) samples[index].fuelLiters?.let { return it }
        return null
    }
}
