package com.project.analyzer.telemetry.ac.api.util

public fun Float?.finiteOrNull(): Float? = this?.takeIf { it.isFinite() }
public fun Int?.nonNegativeOrNull(): Int? = this?.takeIf { it >= 0 }
