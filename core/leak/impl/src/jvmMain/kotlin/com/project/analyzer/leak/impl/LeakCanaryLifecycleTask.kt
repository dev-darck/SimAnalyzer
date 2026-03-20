package com.project.analyzer.leak.impl

import com.project.analyzer.api.di.AppLifecycleTask
import com.project.analyzer.leak.api.LeakCanaryController
import com.project.analyzer.leak.api.LeakCanaryRuntime
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@ContributesIntoSet(AppScope::class)
@SingleIn(AppScope::class)
class LeakCanaryLifecycleTask(
    private val leakCanaryController: LeakCanaryController,
) : AppLifecycleTask {

    override val startOrder: Int = 0
    override val stopOrder: Int = 30

    override suspend fun start() {
        LeakCanaryRuntime.install(leakCanaryController)
        leakCanaryController.start()
    }

    override suspend fun stop() {
        leakCanaryController.stop()
        LeakCanaryRuntime.uninstall(leakCanaryController)
    }
}
