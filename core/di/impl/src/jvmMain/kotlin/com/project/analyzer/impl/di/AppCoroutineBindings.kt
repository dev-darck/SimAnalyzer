package com.project.analyzer.impl.di

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
interface AppCoroutineBindings {
    companion object {

        @IO
        @Provides
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Main
        @Provides
        fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

        @Default
        @Provides
        fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    }
}
