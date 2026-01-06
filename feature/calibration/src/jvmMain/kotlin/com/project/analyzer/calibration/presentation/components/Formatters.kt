package com.project.analyzer.calibration.presentation.components

import com.project.analyzer.math.Vec2

fun fmt(v: Float): String = "%.2f".format(v)
fun fmt(v: Vec2?) = if (v == null) "null" else "(${"%.1f".format(v.x)}, ${"%.1f".format(v.y)})"
