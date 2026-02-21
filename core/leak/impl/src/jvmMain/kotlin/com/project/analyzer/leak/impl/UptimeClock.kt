package com.project.analyzer.leak.impl

import leakcanary.UptimeClock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

internal object UptimeClock : UptimeClock {

    override fun uptime(): Duration = System.nanoTime().nanoseconds
}
