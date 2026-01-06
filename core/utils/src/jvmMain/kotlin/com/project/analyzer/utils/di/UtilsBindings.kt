package com.project.analyzer.utils.di

import com.project.analyzer.api.di.AppEnvironment
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.AppDirectoriesImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
public object UtilsBindings {

    @Provides
    @SingleIn(AppScope::class)
    public fun provideAppDirectories(
        appEnvironment: AppEnvironment
    ): AppDirectories = AppDirectoriesImpl(appEnvironment)
}
