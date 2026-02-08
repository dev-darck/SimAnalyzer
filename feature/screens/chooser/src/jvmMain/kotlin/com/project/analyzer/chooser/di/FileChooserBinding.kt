package com.project.analyzer.chooser.di

import androidx.lifecycle.ViewModel
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.chooser.data.FileSystemRepository
import com.project.analyzer.chooser.data.FileSystemRepositoryImpl
import com.project.analyzer.chooser.domain.useCase.FileChooserUseCase
import com.project.analyzer.chooser.domain.useCase.FileChooserUseCaseImpl
import com.project.analyzer.chooser.presentation.FileChooserViewModel
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey

@BindingContainer
@ContributesTo(ScreenScope::class)
interface FileChooserBinding {

    @Binds
    fun bindRepository(repository: FileSystemRepositoryImpl): FileSystemRepository

    @Binds
    fun bindFileChooserUseCase(impl: FileChooserUseCaseImpl): FileChooserUseCase

    companion object {

        @Provides
        @IntoMap
        @ViewModelKey(FileChooserViewModel::class)
        private fun provideFileChooserViewModel(
            useCase: FileChooserUseCase,
        ): ViewModel = FileChooserViewModel(
            useCase = useCase
        )
    }
}