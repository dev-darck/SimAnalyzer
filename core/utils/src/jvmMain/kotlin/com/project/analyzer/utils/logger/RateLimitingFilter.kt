@file:OptIn(ExperimentalAtomicApi::class)

package com.project.analyzer.utils.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.Marker
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

public class RateLimitingFilter : TurboFilter() {

    public var markerName: String = "RATE_LIMITED"
    public var frequency: Int = 350

    private val minIntervalNs by lazy { 1_000_000_000L / frequency }
    private val lastLogTime = AtomicLong(0L)

    override fun decide(
        marker: Marker?,
        logger: Logger?,
        level: Level?,
        format: String?,
        params: Array<out Any>?,
        t: Throwable?
    ): FilterReply {
        if (marker == null || !marker.contains(markerName)) {
            return FilterReply.NEUTRAL
        }

        val now = System.nanoTime()
        val last = lastLogTime.load()
        return if (now - last >= minIntervalNs && lastLogTime.compareAndSet(last, now)) {
            FilterReply.NEUTRAL
        } else {
            FilterReply.DENY
        }
    }
}