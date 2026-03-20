package com.project.analyzer.api.di

public interface AppLifecycleTask {

    public val startOrder: Int
        get() = 0

    public val stopOrder: Int
        get() = startOrder

    public suspend fun start() {}

    public suspend fun stop() {}
}
