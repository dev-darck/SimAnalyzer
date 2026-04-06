package com.analyzer.session.analysis.domain.trackmap

/**
 * Small geometry primitives keep the higher-level track-map classes readable and deterministic.
 */
internal fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
