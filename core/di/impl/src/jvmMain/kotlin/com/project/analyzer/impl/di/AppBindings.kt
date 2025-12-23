package com.project.analyzer.impl.di

import com.project.analyzer.api.di.AppEnvironment
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.Main
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@ContributesTo(AppScope::class)
@BindingContainer
object AppBindings {

    @Provides
    fun provideEnv(): AppEnvironment = JvmAppEnvironment()

    @IO
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Default
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Main
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
