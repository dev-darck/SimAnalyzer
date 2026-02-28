package com.analyzer.session.details.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.details.presentation.SessionDetailViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface SessionDetailsBindings {
    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(SessionDetailViewModel::class)
        private fun provideSessionDetailViewModel(impl: SessionDetailViewModel): ViewModel = impl
    }
}
