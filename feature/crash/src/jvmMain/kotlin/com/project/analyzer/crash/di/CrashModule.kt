package com.project.analyzer.crash.di

import com.project.analyzer.crash.domain.usecase.CopyReportUseCase
import com.project.analyzer.crash.domain.usecase.OpenFileUseCase
import com.project.analyzer.crash.domain.usecase.OpenLogsFolderUseCase
import com.project.analyzer.crash.domain.usecase.ReportOnGitHubUseCase
import com.project.analyzer.crash.presentation.CrashViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(CrashScope::class)
@BindingContainer
object CrashModule {

    @Provides
    @SingleIn(CrashScope::class)
    fun provideCrashViewModel(
        copyReportUseCase: CopyReportUseCase,
        openFileUseCase: OpenFileUseCase,
        openLogsFolderUseCase: OpenLogsFolderUseCase,
        reportOnGitHubUseCase: ReportOnGitHubUseCase,
    ): CrashViewModel = CrashViewModel(
        copyReportUseCase = copyReportUseCase,
        openFileUseCase = openFileUseCase,
        openLogsFolderUseCase = openLogsFolderUseCase,
        reportOnGitHubUseCase = reportOnGitHubUseCase,
    )

    @Provides
    @SingleIn(CrashScope::class)
    fun provideCopyReportUseCase(): CopyReportUseCase = CopyReportUseCase()

    @Provides
    @SingleIn(CrashScope::class)
    fun provideOpenFileUseCase(): OpenFileUseCase = OpenFileUseCase()

    @Provides
    @SingleIn(CrashScope::class)
    fun provideOpenLogsFolderUseCase(): OpenLogsFolderUseCase = OpenLogsFolderUseCase()

    @Provides
    @SingleIn(CrashScope::class)
    fun provideReportOnGitHubUseCase(copyReportUseCase: CopyReportUseCase): ReportOnGitHubUseCase =
        ReportOnGitHubUseCase(copyReportUseCase)
}
