package com.analyzer.settings.di

import com.analyzer.settings.api.ThemeRepository as PublicThemeRepository
import com.analyzer.settings.data.theme.ThemeRepository
import com.analyzer.settings.data.theme.ThemeRepositoryImpl
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
interface SettingsAppBindings {
    companion object {

        @Provides
        private fun provideThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository = impl

        @Provides
        private fun providePublicThemeRepository(repo: ThemeRepository): PublicThemeRepository = repo
    }
}
