package com.project.analyzer.calibration.presentation

import com.project.analyzer.calibration.domain.model.CalibrationSample
import com.project.analyzer.calibration.domain.model.WheelDebug
import com.project.analyzer.math.Pose2D
import com.project.analyzer.math.headingDegreesOrZero

internal data class CalibrationDebugSnapshot(val pose: Pose2D, val wheels: WheelDebug?, val headingDegrees: Float)

internal fun CalibrationSample.toDebugSnapshot(): CalibrationDebugSnapshot? {
    val pose = pose ?: return null
    return CalibrationDebugSnapshot(
        pose = pose,
        wheels = wheels,
        headingDegrees = pose.forward.headingDegreesOrZero(),
    )
}
