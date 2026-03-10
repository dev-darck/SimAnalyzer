package com.analyzer.session.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.data.repository.impl.RecordedSessionRepositoryImpl
import com.analyzer.session.domain.usecase.SessionListDataUseCase
import com.analyzer.session.domain.usecase.SessionListDataUseCaseImpl
import com.analyzer.session.domain.usecase.SessionTrackMapUseCase
import com.analyzer.session.domain.usecase.SessionTrackMapUseCaseImpl
import com.analyzer.session.presentation.SessionListViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
@Suppress("unused")
interface SessionBindings {

    companion object {

        @Provides
        private fun provideRecordedSessionRepository(impl: RecordedSessionRepositoryImpl): RecordedSessionRepository =
            impl

        @Provides
        private fun provideSessionListDataUseCase(impl: SessionListDataUseCaseImpl): SessionListDataUseCase = impl

        @Provides
        private fun provideSessionTrackMapUseCase(impl: SessionTrackMapUseCaseImpl): SessionTrackMapUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(SessionListViewModel::class)
        private fun provideSessionListViewModel(impl: SessionListViewModel): ViewModel = impl
    }
}
