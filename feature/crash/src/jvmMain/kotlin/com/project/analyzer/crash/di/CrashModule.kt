package com.project.analyzer.crash.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.crash.domain.CreateCrashReportUseCase
import com.project.analyzer.crash.domain.CreateCrashReportUseCaseImpl
import com.project.analyzer.crash.domain.usecase.CopyReportUseCase
import com.project.analyzer.crash.domain.usecase.CopyReportUseCaseImpl
import com.project.analyzer.crash.domain.usecase.OpenFileUseCase
import com.project.analyzer.crash.domain.usecase.OpenFileUseCaseImpl
import com.project.analyzer.crash.domain.usecase.OpenLogsFolderUseCase
import com.project.analyzer.crash.domain.usecase.OpenLogsFolderUseCaseImpl
import com.project.analyzer.crash.domain.usecase.ReportOnGitHubUseCase
import com.project.analyzer.crash.domain.usecase.ReportOnGitHubUseCaseImpl
import com.project.analyzer.crash.presentation.CrashViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@ContributesTo(AppScope::class)
@BindingContainer
interface CrashModule {
    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(CrashViewModel::class)
        private fun provideCrashViewModel(
            copyReportUseCase: CopyReportUseCase,
            openFileUseCase: OpenFileUseCase,
            openLogsFolderUseCase: OpenLogsFolderUseCase,
            reportOnGitHubUseCase: ReportOnGitHubUseCase,
        ): ViewModel = CrashViewModel(
            copyReportUseCase = copyReportUseCase,
            openFileUseCase = openFileUseCase,
            openLogsFolderUseCase = openLogsFolderUseCase,
            reportOnGitHubUseCase = reportOnGitHubUseCase,
        )

        @Provides
        @SingleIn(AppScope::class)
        private fun provideCreateCrashReportUseCase(): CreateCrashReportUseCase = CreateCrashReportUseCaseImpl()

        @Provides
        @SingleIn(AppScope::class)
        private fun provideCopyReportUseCase(): CopyReportUseCase = CopyReportUseCaseImpl()

        @Provides
        @SingleIn(AppScope::class)
        private fun provideOpenFileUseCase(): OpenFileUseCase = OpenFileUseCaseImpl()

        @Provides
        @SingleIn(AppScope::class)
        private fun provideOpenLogsFolderUseCase(): OpenLogsFolderUseCase = OpenLogsFolderUseCaseImpl()

        @Provides
        @SingleIn(AppScope::class)
        private fun provideReportOnGitHubUseCase(copyReportUseCase: CopyReportUseCase): ReportOnGitHubUseCase =
            ReportOnGitHubUseCaseImpl(copyReportUseCase)
    }
}
