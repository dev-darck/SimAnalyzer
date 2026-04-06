package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.impl.domain.highlight.extension.toPercentLabel
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerApexClassification
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisCornerSetupInsight
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun SessionAnalysisCornerAnalysis.timeLossContextText(): String {
    val entryLoss = abs(entrySpeedDeltaKmh ?: 0f).roundToInt()
    val exitLoss = abs(exitSpeedDeltaKmh ?: 0f).roundToInt()
    return when {
        trailBrakingScore <= 55 ->
            "The car is unloaded too early before apex, so entry grip fades before rotation is finished."

        apexClassification == SessionAnalysisCornerApexClassification.EarlyApex ->
            "The car reaches apex too early, then pinches the exit and gives away throttle time."

        wheelSpin && exitSpeedDeltaKmh != null && exitSpeedDeltaKmh <= -4f ->
            "Rear slip is trimming about $exitLoss km/h from the exit, so the loss continues down the next straight."

        exitSpeedDeltaKmh != null && exitSpeedDeltaKmh <= -4f ->
            "Most of the leak is after apex, where the car is about $exitLoss km/h slower than the reference on exit."

        entrySpeedDeltaKmh != null && entrySpeedDeltaKmh <= -5f ->
            "The loss starts before apex, where the car arrives about $entryLoss km/h slower than the reference."

        coastingRatio >= 0.22f ->
            "There is a neutral coast phase through the middle, so the corner is split into two separate inputs."

        else ->
            "Compare brake release, apex timing, and first throttle pickup against the reference trace here."
    }
}

internal fun SessionAnalysisCornerAnalysis.timeLossRecommendationText(): String = when {
    trailBrakingScore <= 55 ->
        "Keep 5-10% brake pressure a fraction longer, then release as the car reaches apex ${apexTrackPosition.toPercentLabel()}."

    apexClassification == SessionAnalysisCornerApexClassification.EarlyApex ->
        "Turn in later by about half a car width so the apex lands later and the car can open the exit earlier."

    wheelSpin || (exitSpeedDeltaKmh ?: 0f) <= -4f ->
        "Wait for more steering unwind, then squeeze the throttle instead of going to power while the car is still rotating."

    (entrySpeedDeltaKmh ?: 0f) <= -5f ->
        "Brake a touch later and release the pedal smoother so the car keeps more entry speed without washing wide."

    coastingRatio >= 0.22f ->
        "Replace the neutral coast with one committed transition: either light trail brake deeper or pick the throttle up earlier."

    else ->
        "Use the reference lap to compare where you release brake, hit apex, and make the first clean throttle pickup."
}

internal const val earlyApexRecommendationText: String =
    "Delay turn-in slightly and let the apex happen later so the car can stay wider and freer on exit."

internal const val lateApexRecommendationText: String =
    "Start rotation a touch earlier instead of waiting too deep into the corner before asking the car to turn."

internal const val coastingRecommendationText: String =
    "Replace the neutral coast with either a lighter trail-brake release or an earlier throttle pickup."

internal const val wheelSpinRecommendationText: String =
    "Let the steering open a little more, then squeeze the throttle instead of snapping to full pedal on exit."

internal fun SessionAnalysisCornerAnalysis.trailBrakingRecommendationText(): String =
    "Keep a small amount of brake pressure to apex ${apexTrackPosition.toPercentLabel()} instead of coming fully off the pedal early."

internal fun SessionAnalysisCornerAnalysis.lockupRecommendationText(
    setupInsight: SessionAnalysisCornerSetupInsight?,
): String = setupInsight?.recommendation
    ?: "Brake 2-3% softer at peak and start releasing the pedal earlier into turn-in. If it repeats every lap, move bias rearward."

internal fun SessionAnalysisCornerAnalysis.understeerRecommendationText(
    setupInsight: SessionAnalysisCornerSetupInsight?,
): String = setupInsight?.recommendation
    ?: "Keep light trail brake a fraction longer, delay first throttle, and avoid asking for rotation and power at the same time."

internal fun SessionAnalysisCornerAnalysis.oversteerRecommendationText(
    setupInsight: SessionAnalysisCornerSetupInsight?,
): String = setupInsight?.recommendation
    ?: "Release brake smoother, ask for less initial rotation, and wait for the rear to settle before adding throttle."
