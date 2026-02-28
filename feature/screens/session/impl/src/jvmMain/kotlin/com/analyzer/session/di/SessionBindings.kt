package com.analyzer.session.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.data.repository.impl.RecordedSessionRepositoryImpl
import com.analyzer.session.presentation.SessionListViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface SessionBindings {
    companion object {

        @Provides
        private fun provideRecordedSessionRepository(impl: RecordedSessionRepositoryImpl): RecordedSessionRepository =
            impl

        @Provides
        @IntoMap
        @ViewModelKey(SessionListViewModel::class)
        private fun provideSessionListViewModel(impl: SessionListViewModel): ViewModel = impl
    }
}
