package com.project.analyzer.utils.di

import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.AppDirectoriesImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
public interface UtilsBindings {
    public companion object {

        @Provides
        @SingleIn(AppScope::class)
        public fun provideAppDirectories(appDirectories: AppDirectoriesImpl): AppDirectories = appDirectories
    }
}
