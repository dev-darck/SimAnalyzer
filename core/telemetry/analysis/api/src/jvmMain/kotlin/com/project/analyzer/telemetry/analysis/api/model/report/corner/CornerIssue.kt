package com.project.analyzer.telemetry.analysis.api.model.report.corner

public enum class CornerIssue {
    LATE_BRAKING,
    EARLY_BRAKING,
    AGGRESSIVE_TRAIL_BRAKE,
    INSUFFICIENT_TRAIL_BRAKE,
    EARLY_APEX,
    LATE_APEX,
    SLOW_APEX_SPEED,
    FAST_APEX_SPEED,
    EARLY_THROTTLE,
    LATE_THROTTLE,
    WHEEL_SPIN_EXIT,
    UNDERSTEER_ENTRY,
    OVERSTEER_EXIT,
    PORPOISING,
    LOCKUP_BRAKING,
    NONE,
}
