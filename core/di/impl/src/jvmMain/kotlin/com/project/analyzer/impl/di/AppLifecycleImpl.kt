package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppLifecycle
import com.project.analyzer.api.di.AppLifecycleTask
import com.project.analyzer.api.di.IO
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Inject
@SingleIn(AppScope::class)
internal class AppLifecycleImpl(
    lifecycleTasks: Set<@JvmSuppressWildcards AppLifecycleTask>,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : AppLifecycle {

    private val started = AtomicBoolean(false)
    private val logger = logger()
    private val lifecycleDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(2, "AppLifecycle")
    private val scope = CoroutineScope(
        SupervisorJob() + lifecycleDispatcher + CoroutineExceptionHandler { _, throwable ->
            logger.error(throwable) { "Unhandled exception in AppLifecycle" }
        },
    )
    private var startupJob: Job? = null
    private val startTasks: List<AppLifecycleTask> = lifecycleTasks.sortedBy(AppLifecycleTask::startOrder)
    private val stopTasks: List<AppLifecycleTask> = lifecycleTasks.sortedBy(AppLifecycleTask::stopOrder)

    override suspend fun start() {
        if (!started.compareAndSet(false, true)) return
        startupJob?.cancelAndJoin()
        startupJob = scope.launch {
            startTasks.forEach { task ->
                runCatching { task.start() }
            }
        }
    }

    override suspend fun stop() {
        if (!started.compareAndSet(true, false)) return
        startupJob?.cancelAndJoin()
        startupJob = null

        stopTasks.forEach { task ->
            runCatching { task.stop() }
        }
    }
}
