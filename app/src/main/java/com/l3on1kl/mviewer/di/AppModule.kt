package com.l3on1kl.mviewer.di

import com.l3on1kl.mviewer.data.datasource.DocumentDataSource
import com.l3on1kl.mviewer.data.datasource.LocalFileDataSource
import com.l3on1kl.mviewer.data.datasource.RemoteUrlDataSource
import com.l3on1kl.mviewer.data.repository.DocumentRepositoryImpl
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideDataSources(
        local: LocalFileDataSource,
        remote: RemoteUrlDataSource
    ): List<DocumentDataSource> = listOf(local, remote)

    @Provides
    @JvmSuppressWildcards
    fun provideDocumentRepository(
        dataSources: List<DocumentDataSource>
    ): DocumentRepository = DocumentRepositoryImpl(dataSources)

}
