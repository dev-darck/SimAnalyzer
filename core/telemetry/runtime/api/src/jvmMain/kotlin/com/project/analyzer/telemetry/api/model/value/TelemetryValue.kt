package com.project.analyzer.telemetry.api.model.value

import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3

public sealed interface TelemetryValue {
    public data class BoolVal(val value: Boolean) : TelemetryValue
    public data class IntVal(val value: Int) : TelemetryValue
    public data class LongVal(val value: Long) : TelemetryValue
    public data class FloatVal(val value: Float) : TelemetryValue
    public data class DoubleVal(val value: Double) : TelemetryValue
    public data class StringVal(val value: String) : TelemetryValue
    public data class Vec2Val(val value: Vec2) : TelemetryValue
    public data class Vec3Val(val value: Vec3) : TelemetryValue
    public data class FloatArrayVal(val value: FloatArray) : TelemetryValue
}
