package com.l3on1kl.mviewer.di

import android.content.ContentResolver
import android.content.Context
import com.l3on1kl.mviewer.data.datasource.DocumentDataSource
import com.l3on1kl.mviewer.data.datasource.LocalMarkdownFileDataSource
import com.l3on1kl.mviewer.data.datasource.RemoteMarkdownDataSource
import com.l3on1kl.mviewer.data.repository.DocumentRepositoryImpl
import com.l3on1kl.mviewer.domain.repository.DocumentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver

    @Provides
    @Singleton
    fun provideLocalMarkdownDataSource(
        contentResolver: ContentResolver
    ): LocalMarkdownFileDataSource =
        LocalMarkdownFileDataSource(contentResolver)

    @Provides
    @Singleton
    fun provideRemoteMarkdownDataSource(): RemoteMarkdownDataSource =
        RemoteMarkdownDataSource()

    @Provides
    fun provideDataSources(
        local: LocalMarkdownFileDataSource,
        remote: RemoteMarkdownDataSource
    ): List<DocumentDataSource> = listOf(local, remote)

    @Provides
    @Singleton
    @JvmSuppressWildcards
    fun provideDocumentRepository(
        dataSources: List<DocumentDataSource>
    ): DocumentRepository = DocumentRepositoryImpl(dataSources)
}
