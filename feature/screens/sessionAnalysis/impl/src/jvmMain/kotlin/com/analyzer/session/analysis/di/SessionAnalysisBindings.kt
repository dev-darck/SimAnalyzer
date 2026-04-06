package com.analyzer.session.analysis.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.analysis.data.repository.SessionAnalysisRepositoryImpl
import com.analyzer.session.analysis.domain.repository.SessionAnalysisRepository
import com.analyzer.session.analysis.domain.usecase.SessionAnalysisUseCase
import com.analyzer.session.analysis.domain.usecase.SessionAnalysisUseCaseImpl
import com.analyzer.session.analysis.presentation.SessionAnalysisViewModel
import com.project.analyzer.api.di.ScreenScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

/**
 * Wires the screen module graph so UI, use case, and repository resolve from one scoped entry point.
 */
@ContributesTo(ScreenScope::class)
@BindingContainer
interface SessionAnalysisBindings {

    companion object {

        @Provides
        private fun provideSessionAnalysisRepository(impl: SessionAnalysisRepositoryImpl): SessionAnalysisRepository =
            impl

        @Provides
        private fun provideSessionAnalysisUseCase(impl: SessionAnalysisUseCaseImpl): SessionAnalysisUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(SessionAnalysisViewModel::class)
        private fun provideSessionAnalysisViewModel(impl: SessionAnalysisViewModel): ViewModel = impl
    }
}
