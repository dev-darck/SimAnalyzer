package com.project.analyzer.api.di

public interface AppLifecycle {

    public suspend fun start()

    public suspend fun stop()
}
