package com.project.analyzer.leak.impl

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class DelayedExecutor(
    private val scheduler: ScheduledExecutorService,
    private val delayMillis: Long,
) : Executor {

    override fun execute(command: Runnable) {
        try {
            scheduler.schedule(command, delayMillis, TimeUnit.MILLISECONDS)
        } catch (_: RejectedExecutionException) {
            // Ignore: scheduler may be shutdown during app shutdown.
        }
    }
}