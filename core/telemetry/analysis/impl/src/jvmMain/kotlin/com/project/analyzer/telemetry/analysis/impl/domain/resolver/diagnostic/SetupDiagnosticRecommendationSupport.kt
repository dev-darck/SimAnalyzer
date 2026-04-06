package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

internal const val setupUndersteerRecommendation: String =
    "Soften the front anti-roll bar by one step first. If the push only shows up in fast corners, try +1 front wing instead."

internal const val setupOversteerRecommendation: String =
    "Soften the rear anti-roll bar or add a click of rear wing first, then recheck whether the rear still rotates too eagerly."

internal const val brakeBiasRecommendation: String =
    "Move brake bias 0.3-0.6% rearward first. If the fronts still lock, trim peak pedal pressure into the stop."

internal const val aeroFrontBalanceRecommendation: String =
    "Add 1 click of front wing first. If rake is aggressive, reduce it before chasing the line with more steering."

internal const val aeroRearBalanceRecommendation: String =
    "Add rear wing or reduce rake first. If the rear is only nervous on release, calm rear rebound next."

internal const val tyrePressureAxisRecommendation: String =
    "Close the front/rear pressure split by about 0.3-0.5 psi first, then recheck hot pressures after a clean lap."

internal const val tyrePressureSideRecommendation: String =
    "Trim left/right pressures until hot pressures converge side to side, then re-evaluate balance in the same corner set."

internal const val tyrePressureHighRecommendation: String =
    "Drop baseline pressure by about 0.5 psi first so the tyres come back into the hot working window."

internal const val tyrePressureLowRecommendation: String =
    "Raise baseline pressure slightly first so the carcass reaches the working window sooner."

internal const val tyrePressureFallbackRecommendation: String =
    "Rebalance pressures across the car first, then check whether the imbalance remains after one stable reference lap."

internal const val tyreInnerShoulderRecommendation: String =
    "Reduce negative camber slightly first. If the driver is also overloading entry, fix the line before adding more setup change."

internal const val tyreOuterShoulderRecommendation: String =
    "Add a touch more negative camber first, or reduce steering scrub on entry if the line is also too aggressive."

internal const val damperRecommendation: String =
    "Add a small amount of rebound damping first. If the platform still oscillates, add a little compression next."
