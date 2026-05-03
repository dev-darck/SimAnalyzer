package com.analyzer.session.details.di

import androidx.lifecycle.ViewModel
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestionsUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailCompareSuggestionsUseCaseImpl
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailDataUseCaseImpl
import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailImportCompareSessionUseCaseImpl
import com.analyzer.session.details.domain.usecase.SessionDetailShareResultsUseCase
import com.analyzer.session.details.domain.usecase.SessionDetailShareResultsUseCaseImpl
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
        private fun provideSessionDetailDataUseCase(impl: SessionDetailDataUseCaseImpl): SessionDetailDataUseCase = impl

        @Provides
        private fun provideSessionDetailCompareSuggestionsUseCase(
            impl: SessionDetailCompareSuggestionsUseCaseImpl,
        ): SessionDetailCompareSuggestionsUseCase = impl

        @Provides
        private fun provideSessionDetailImportCompareSessionUseCase(
            impl: SessionDetailImportCompareSessionUseCaseImpl,
        ): SessionDetailImportCompareSessionUseCase = impl

        @Provides
        private fun provideSessionDetailShareResultsUseCase(
            impl: SessionDetailShareResultsUseCaseImpl,
        ): SessionDetailShareResultsUseCase = impl

        @Provides
        @IntoMap
        @ViewModelKey(SessionDetailViewModel::class)
        private fun provideSessionDetailViewModel(impl: SessionDetailViewModel): ViewModel = impl
    }
}
