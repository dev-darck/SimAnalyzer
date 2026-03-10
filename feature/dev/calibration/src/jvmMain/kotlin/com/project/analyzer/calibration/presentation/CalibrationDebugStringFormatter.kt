package com.project.analyzer.calibration.presentation

import com.project.analyzer.calibration.presentation.components.fmt
import com.project.analyzer.math.Vec2

internal fun CalibrationDebugSnapshot.formatDebugString(
    speedKmh: Float,
    directionLabel: String,
    direction: Vec2,
): String =
    """
    POS: ${fmt(pose.pos)}  |  Speed: ${"%.1f".format(speedKmh)} km/h

    $directionLabel: ${fmt(direction)}  |  Heading: ${"%.1f".format(headingDegrees)}°

    FL: ${fmt(wheels?.fl)}   FR: ${fmt(wheels?.fr)}
    RL: ${fmt(wheels?.rl)}   RR: ${fmt(wheels?.rr)}
    """.trimIndent()
