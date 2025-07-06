package com.l3on1kl.mviewer.di

import com.l3on1kl.mviewer.domain.repository.HistoryRepository
import com.l3on1kl.mviewer.domain.usecase.AddToHistoryUseCase
import com.l3on1kl.mviewer.domain.usecase.GetHistoryUseCase
import com.l3on1kl.mviewer.domain.usecase.InitDefaultHistoryUseCase
import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
import com.l3on1kl.mviewer.domain.usecase.RemoveFromHistoryUseCase
import com.l3on1kl.mviewer.presentation.viewer.MarkdownRenderer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PresentationModule {

    @Provides
    @Singleton
    fun markdownRenderer(
        parser: ParseMarkdownUseCase
    ): MarkdownRenderer = MarkdownRenderer(parser)

    @Provides
    @Singleton
    fun addHistoryUc(
        repository: HistoryRepository
    ) = AddToHistoryUseCase(repository)

    @Provides
    @Singleton
    fun getHistoryUc(
        repository: HistoryRepository
    ) = GetHistoryUseCase(repository)

    @Provides
    @Singleton
    fun removeHistoryUc(
        repository: HistoryRepository
    ) = RemoveFromHistoryUseCase(repository)

    @Provides
    @Singleton
    fun initDefaultHistoryUc(
        repository: HistoryRepository
    ) = InitDefaultHistoryUseCase(repository)
}
