package com.project.analyzer.math

/**
 * Shared math tolerances.
 *
 * IMPORTANT: keep these stable; gameplay/telemetry algorithms often depend on exact eps values.
 */
public object MathEps {

    /** Segment intersection: treat vectors as parallel when |cross| < PARALLEL. */
    public const val PARALLEL: Float = 1e-6f

    /** Segment intersection: allow parameters slightly outside [0..1] by this tolerance. */
    public const val PARAM: Float = 1e-4f

    /** Direction checks around 0 (forward/back side). */
    public const val DIR: Float = 1e-4f

    public const val EPS: Float = 1e-5f
}

/** Legacy constant from earlier code. */
public const val MinLen: Float = 0.1F
