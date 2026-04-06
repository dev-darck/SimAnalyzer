package com.project.analyzer.telemetry.analysis.impl.domain.highlight.extension

import kotlin.math.roundToInt

/**
 * Turns continuous track position into short labels that stay readable in highlight copy.
 */
internal fun Float?.toPercentLabel(): String = this
    ?.let { value -> "${(value.coerceIn(0f, 1f) * 100f).roundToInt()}%" }
    ?: "this section"
