package com.project.analyzer.leak.impl

import leakcanary.Clock

internal object UptimeClock : Clock {

    override fun uptimeMillis(): Long = System.nanoTime() / 1_000_000L
}
