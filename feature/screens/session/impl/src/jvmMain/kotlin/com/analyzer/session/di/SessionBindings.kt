package com.analyzer.session.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.analyzer.session.data.repository.impl.RecordedSessionRepositoryImpl
import com.analyzer.session.presentation.SessionListViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(ScreenScope::class)
@BindingContainer
interface SessionBindings {

    @Binds
    fun bindRecordedSessionRepository(impl: RecordedSessionRepositoryImpl): RecordedSessionRepository

    @Binds
    @IntoMap
    @ViewModelKey(SessionListViewModel::class)
    fun bindSessionListViewModel(impl: SessionListViewModel): ViewModel
}
