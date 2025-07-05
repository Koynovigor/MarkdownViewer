package com.l3on1kl.mviewer.di

import com.l3on1kl.mviewer.domain.usecase.ParseMarkdownUseCase
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
    fun markdownRenderer(parseUc: ParseMarkdownUseCase): MarkdownRenderer =
        MarkdownRenderer(parseUc)
}
