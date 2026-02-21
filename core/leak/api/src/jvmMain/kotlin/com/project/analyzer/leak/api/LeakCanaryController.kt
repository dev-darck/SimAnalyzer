package com.project.analyzer.leak.api

public interface LeakCanaryController {

    public val isEnabled: Boolean

    public fun watch(watchedObject: Any, description: String = "")

    public fun dumpNow(reason: String? = null)

    public suspend fun start()

    public suspend fun stop()
}
